
package org.crosswire.jsword.view.swing.desktop;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.crosswire.common.progress.swing.JobsProgressBar;
import org.crosswire.jsword.util.Project;

/**
 * The status bar provides useful info to the user as to the current
 * state of the program.
 * <p>We need to think about the stuff to put in here:<ul>
 * <li>A status message. This changes with what the user is pointing at,
 *     so is very similar to tool-tips. Although they are commonly more
 *     instructional.
 * <li>A set of panels that tell you the time/if CAPS is presses and so on
 * </ul>
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
 * @see gnu.gpl.Licence
 * @author Joe Walker [joe at eireneh dot com]
 * @version $Id$
 */
public class StatusBar extends JComponent implements MouseListener, HyperlinkListener
{
    /**
     * Create a new StatusBar
     */
    public StatusBar()
    {
        jbInit();
    }

    /**
     * Init the GUI
     */
    private void jbInit()
    {
        lbl_message.setBorder(BorderFactory.createEtchedBorder());
        lbl_message.setText(Msg.STATUS_DEFAULT.toString());

        pnl_progr.setBorder(BorderFactory.createEtchedBorder());
        Font font = pnl_progr.getFont();
        pnl_progr.setFont(font.deriveFont(6.0F));

        /*
        Dimension dim = pnl_progr.getPreferredSize();
        dim.height = lbl_message.getSize().height;
        pnl_progr.setPreferredSize(dim);
        */

        lbl_name.setBorder(BorderFactory.createEtchedBorder());
        lbl_name.setText(" "+Project.instance().getName()+" v"+Project.instance().getVersion()+" "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        this.setBorder(BorderFactory.createLoweredBevelBorder());
        this.setLayout(new GridBagLayout());

        this.add(lbl_message, new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        this.add(pnl_progr,   new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        this.add(lbl_name,    new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    }

    /**
     * When the mouse points at a hyperlink that has registered with us
     * to be shown on the statusbar
     * @see javax.swing.event.HyperlinkListener#hyperlinkUpdate(javax.swing.event.HyperlinkEvent)
     */
    public void hyperlinkUpdate(HyperlinkEvent ev)
    {
        HyperlinkEvent.EventType type = ev.getEventType();
        if (type == HyperlinkEvent.EventType.ENTERED)
        {
            lbl_message.setText(ev.getDescription());
        }
        else if (type == HyperlinkEvent.EventType.EXITED)
        {
            lbl_message.setText(Msg.STATUS_DEFAULT.toString());
        }
    }

    /**
     * When the mouse points at something that has registered with us
     * to be shown on the statusbar
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent ev)
    {
        if (ev.getSource() instanceof AbstractButton)
        {
            AbstractButton button = (AbstractButton) ev.getSource();
            Action action = button.getAction();

            if (action != null)
            {
                Object value = action.getValue(Action.LONG_DESCRIPTION);

                if (value != null)
                {
                    lbl_message.setText(value.toString());
                }
            }
        }
    }

    /**
     * When the mouse no longer points at something that has registered with us
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent ev)
    {
        lbl_message.setText(Msg.STATUS_DEFAULT.toString());
    }

    /**
     * Invoked when the mouse has been clicked on a component.
     * Ignored
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent ev)
    {
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     * Ignored
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent ev)
    {
    }

    /**
     * Invoked when a mouse button has been released on a component.
     * Ignored
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent ev)
    {
    }

    /**
     * Where the progress bars go
     */
    private JobsProgressBar pnl_progr = new JobsProgressBar(true);

    /**
     * Where the help messages go
     */
    private JLabel lbl_message = new JLabel();

    /**
     * Where the product name goes
     */
    private JLabel lbl_name = new JLabel();
}
