
package org.crosswire.jsword.book.jdbc;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.crosswire.common.util.NetUtil;
import org.crosswire.common.util.PropertiesUtil;
import org.crosswire.common.util.StringUtil;
import org.crosswire.jsword.book.Bible;
import org.crosswire.jsword.book.BibleDriverManager;
import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.book.basic.AbstractBibleDriver;

/**
 * This represents all of the JDBCBibles.
 * 
 * <p><table border='1' cellPadding='3' cellSpacing='0'>
 * <tr><td bgColor='white' class='TableRowColor'><font size='-7'>
 *
 * Distribution Licence:<br />
 * JSword is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General Public License,
 * version 2 as published by the Free Software Foundation.<br />
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.<br />
 * The License is available on the internet
 * <a href='http://www.gnu.org/copyleft/gpl.html'>here</a>, or by writing to:
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 * MA 02111-1307, USA<br />
 * The copyright to this program is held by it's authors.
 * </font></td></tr></table>
 * @see docs.Licence
 * @author Joe Walker [joe at eireneh dot com]
 * @version $Id$
 */
public class JDBCBibleDriver extends AbstractBibleDriver
{
    /**
     * Some basic driver initialization. If we couldn't get a Bibles root
     * then just give a warning but do no more. Perhaps there are other
     * BibleDrivers that can cope, so this needent be a big error.
     */
    private JDBCBibleDriver() throws MalformedURLException, IOException
    {
        log.debug("Starting");

        URL root = findBibleRoot();
        if (root == null)
        {
            log.warn("Cant find Bibles root, restart needed before service can resume.");
            dir = null;
            return;
        }

        dir = NetUtil.lengthenURL(root, "jdbc");

        // To save giving off hundreds of warnings later we'll do a check on the
        // setup and available Bibles now ...
        if (dir.getProtocol().equals("file"))
        {
            File fdir = new File(dir.getFile());
            if (!fdir.isDirectory())
            {
                log.debug("The directory '"+dir+"' does not exist.");
            }
        }
        else
        {
            URL search = NetUtil.lengthenURL(dir, "list.txt");
            InputStream in = search.openStream();
            if (in == null)
                log.debug("The remote listing file at '"+search+"' does not exist.");
        }       
    }

    /**
     * Some basic info about who we are
     * @param A short identifing string
     */
    public String getDriverName()
    {
        return "JDBC";
    }

    /**
     * Get a list of the Books available from the driver
     * @return an array of book names
     */
    public String[] getBibleNames()
    {
        if (dir == null)
            return new String[0];

        try
        {
            if (dir.getProtocol().equals("file"))
            {
                File fdir = new File(dir.getFile());

                // Check that the dir exists
                if (!fdir.isDirectory())
                {
                    return new String[0];
                }
                else
                {
                    // List all the versions
                    String[] temp = fdir.list(new FilenameFilter() {
                        public boolean accept(File parent, String name)
                        {
                            return name.endsWith(".properties");
                        }
                    });

                    // Chop off the .properties bit
                    for (int i=0; i<temp.length; i++)
                    {
                        temp[i] = temp[i].substring(0, temp[i].length()-11);
                    }

                    return temp;
                }
            }
            else
            {
                // We should probably cache this in some way? This is going
                // to get very slow calling this often across a network
                URL search = NetUtil.lengthenURL(dir, "list.txt");
                InputStream in = search.openStream();
                String contents = StringUtil.read(new InputStreamReader(in));
                return StringUtil.tokenize(contents, "\n");
            }
        }
        catch (IOException ex)
        {
            log.warn("failed to load jdbc Bibles: "+ex);
            return new String[0];
        }
    }

    /**
     * Does the named Bible exist?
     * @param name The name of the version to test for
     * @return true if the Bible exists
     */
    public boolean exists(String name)
    {
        try
        {
            URL url = NetUtil.lengthenURL(dir, name+".properties");
            return NetUtil.isFile(url);
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    /**
     * Featch a currently existing Bible, read-only
     * @param name The name of the version to create
     * @exception BookException If the name is not valid
     */
    public Bible getBible(String name) throws BookException
    {
        URL url = null;

        try
        {
            url = NetUtil.lengthenURL(dir, name+".properties");

            InputStream prop_in = url.openStream();
            Properties prop = new Properties();
            PropertiesUtil.load(prop, prop_in);

            // Generate a version (un-inited) to get the default properties
            JDBCBible dest = new JDBCBible(name, prop);
            return dest;
        }
        catch (MalformedURLException ex)
        {
            throw new BookException("jdbc_driver_conf", ex);
        }
        catch (IOException ex)
        {
            throw new BookException("jdbc_driver_save", ex, new Object[] { url });
        }
    }

    /**
     * Create a new blank Bible read for writing
     * @param name The name of the version to create
     * @exception BookException If the name is not valid
     */
    public Bible createBible(String name) throws BookException
    {
        throw new BookException("jdbc_driver_readonly");
    }

    /** The directory URL */
    private URL dir;

    /** The singleton driver */
    protected static JDBCBibleDriver driver;

    /** The log stream */
    protected static Logger log = Logger.getLogger(JDBCBibleDriver.class);

    /**
     * Register ourselves with the Driver Manager
     */
    static
    {
        try
        {
            driver = new JDBCBibleDriver();
            BibleDriverManager.registerDriver(driver);
        }
        catch (Exception ex)
        {
            log.info("JDBCBibleDriver init failure", ex);
        }
    }
}
