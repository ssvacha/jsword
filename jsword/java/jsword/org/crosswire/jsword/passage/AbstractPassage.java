package org.crosswire.jsword.passage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.crosswire.common.util.Logger;

/**
 * This is a base class to help with some of the common implementation
 * details of being a Passage.
 * <p>Importantly, this class takes care of Serialization in a general yet
 * optimized way. I think I am going to have a look at replacement here.
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
public abstract class AbstractPassage implements Passage
{
    /**
     * Setup that leaves original name being null
     */
    protected AbstractPassage()
    {
    }

    /**
     * Setup the original name of this reference
     * @param original_name The text originally used to create this Passage.
     */
    protected AbstractPassage(String original_name)
    {
        this.originalName = original_name;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object obj)
    {
        if (!(obj instanceof Passage))
        {
            log.warn("Can't compare a Passage to a "+obj.getClass().getName()); //$NON-NLS-1$
            return -1;
        }

        Passage thatref = (Passage) obj;

        if (thatref.countVerses() == 0)
        {
            if (countVerses() == 0)
            {
                return 0;
            }
            else
            {
                // that is empty so he should come before me
                return -1;
            }
        }

        if (countVerses() == 0)
        {
            // we are empty be he isn't so we are first
            return 1;
        }

        Verse thatfirst = thatref.getVerseAt(0);
        Verse thisfirst = getVerseAt(0);

        return thisfirst.compareTo(thatfirst);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException
    {
        // This gets us a shallow copy
        AbstractPassage copy = (AbstractPassage) super.clone();

        copy.listeners = new ArrayList();
        copy.listeners.addAll(listeners);

        copy.originalName  = originalName;

        return copy;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        // Since this can not be null
        if (obj == null)
        {
            return false;
        }

        // This is cheating beacuse I am supposed to say:
        // <code>!obj.getClass().equals(this.getClass())</code>
        // However I think it is entirely valid for a RangedPassage
        // to equal a DistinctPassage since the point of the Factory
        // is that the user does not need to know the actual type of the
        // Object he is using.
        if (!(obj instanceof Passage)) return false;

        Passage ref = (Passage) obj;
        // The real test
        if (!ref.getName().equals(getName())) return false;

        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return getName().hashCode();
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getName()
     */
    public String getName()
    {
        if (PassageUtil.isPersistentNaming() && originalName != null)
        {
            return originalName;
        }

        StringBuffer retcode = new StringBuffer();

        Iterator it = rangeIterator(PassageConstants.RESTRICT_NONE);
        Verse current = null;
        while (it.hasNext())
        {
            VerseRange range = (VerseRange) it.next();
            retcode.append(range.getName(current));

            if (it.hasNext())
            {
                retcode.append(PassageConstants.REF_PREF_DELIM);
            }

            current = range.getStart();
        }

        return retcode.toString();
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getOSISName()
     */
    public String getOSISName()
    {
        StringBuffer retcode = new StringBuffer();

        Iterator it = rangeIterator(PassageConstants.RESTRICT_NONE);
        while (it.hasNext())
        {
            VerseRange range = (VerseRange) it.next();
            retcode.append(range.getOSISName());

            if (it.hasNext())
            {
                retcode.append(PassageConstants.REF_OSIS_DELIM);
            }
        }

        return retcode.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return getName();
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getOverview()
     */
    public String getOverview()
    {
        int verse_count = countVerses();
        int book_count = booksInPassage();

        String verses = (verse_count == 1)
                      ? Msg.ABSTRACT_VERSE_SINGULAR.toString()
                      : Msg.ABSTRACT_VERSE_PLURAL.toString();

        String books = (book_count == 1)
                     ? Msg.ABSTRACT_BOOK_SINGULAR.toString()
                     : Msg.ABSTRACT_BOOK_PLURAL.toString();

        return verse_count+" "+verses+" "+book_count+" "+books; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#isEmpty()
     */
    public boolean isEmpty()
    {
        return countVerses() == 0;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#countVerses()
     */
    public int countVerses()
    {
        int count = 0;

        Iterator it = verseIterator();
        while (it.hasNext())
        {
            it.next();
            count++;
        }

        return count;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#countRanges(int)
     */
    public int countRanges(int restrict)
    {
        int count = 0;

        Iterator it = rangeIterator(restrict);
        while (it.hasNext())
        {
            it.next();
            count++;
        }

        return count;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#booksInPassage()
     */
    public int booksInPassage()
    {
        int current_book = 0;
        int book_count = 0;

        Iterator it = verseIterator();
        while (it.hasNext())
        {
            Verse verse = (Verse) it.next();
            if (current_book != verse.getBook())
            {
                current_book = verse.getBook();
                book_count++;
            }
        }

        return book_count;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#chaptersInPassage(int)
     */
    public int chaptersInPassage(int book) throws NoSuchVerseException
    {
        if (book != 0)  BibleInfo.validate(book, 1, 1);

        int current_chapter = 0;
        int chapter_count = 0;

        Iterator it = verseIterator();
        while (it.hasNext())
        {
            Verse verse = (Verse) it.next();

            if ((book == 0 || verse.getBook() == book) && current_chapter != verse.getChapter())
            {
                current_chapter = verse.getChapter();
                chapter_count++;
            }
        }

        return chapter_count;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#versesInPassage(int, int)
     */
    public int versesInPassage(int book, int chapter) throws NoSuchVerseException
    {
        BibleInfo.validate((book == 0 ? 1 : book), (chapter == 0 ? 1 : chapter), 1);

        int verse_count = 0;

        Iterator it = verseIterator();
        while (it.hasNext())
        {
            Verse verse = (Verse) it.next();

            if ((book == 0 || verse.getBook() == book) && (chapter == 0 || verse.getChapter() == chapter))
            {
                verse_count++;
            }
        }

        return verse_count;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getVerseAt(int)
     */
    public Verse getVerseAt(int offset) throws ArrayIndexOutOfBoundsException
    {
        Iterator it = verseIterator();
        Object retcode = null;

        for (int i=0; i<=offset; i++)
        {
            if (!it.hasNext())
            {
                Object[] params = new Object[] { new Integer(offset), new Integer(countVerses()) };
                throw new ArrayIndexOutOfBoundsException(Msg.ABSTRACT_INDEX.toString(params));
            }

            retcode = it.next();
        }

        return (Verse) retcode;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#getVerseRangeAt(int, int)
     */
    public VerseRange getVerseRangeAt(int offset, int restrict) throws ArrayIndexOutOfBoundsException
    {
        Iterator it = rangeIterator(restrict);
        Object retcode = null;

        for (int i=0; i<=offset; i++)
        {
            if (!it.hasNext())
            {
                Object[] params = new Object[] { new Integer(offset), new Integer(countVerses()) };
                throw new ArrayIndexOutOfBoundsException(Msg.ABSTRACT_INDEX.toString(params));
            }

            retcode = it.next();
        }

        return (VerseRange) retcode;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#rangeIterator()
     */
    public Iterator rangeIterator(int restrict)
    {
        return new VerseRangeIterator(verseIterator(), restrict);
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#containsAll(org.crosswire.jsword.passage.Passage)
     */
    public boolean containsAll(Passage that)
    {
        Iterator that_it = null;

        if (that instanceof RangedPassage)
        {
            that_it = ((RangedPassage) that).rangeIterator(PassageConstants.RESTRICT_NONE);
        }
        else
        {
            that_it = that.verseIterator();
        }

        while (that_it.hasNext())
        {
            if (!contains((VerseBase) that_it.next()))
                return false;
        }

        return true;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#trimVerses(int)
     */
    public Passage trimVerses(int count)
    {
        optimizeWrites();
        raiseNormalizeProtection();

        Passage remainder = null;
        int i = 0;
        boolean overflow = false;

        try
        {
            remainder = (Passage) this.clone();

            Iterator it = verseIterator();
            while (it.hasNext())
            {
                i++;
                Verse verse = (Verse) it.next();

                if (i > count)
                {
                    remove(verse);
                    overflow = true;
                }
                else
                {
                    remainder.remove(verse);
                }
            }

            lowerNormalizeProtection();
            // The event notification is done by the remove above

            if (overflow)
            {
                return remainder;
            }
            else
            {
                return null;
            }
        }
        catch (CloneNotSupportedException ex)
        {
            assert false : ex;
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#trimRanges(int, int)
     */
    public Passage trimRanges(int count, int restrict)
    {
        optimizeWrites();
        raiseNormalizeProtection();

        Passage remainder = null;
        int i = 0;
        boolean overflow = false;

        try
        {
            remainder = (Passage) this.clone();

            Iterator it = rangeIterator(restrict);
            while (it.hasNext())
            {
                i++;
                VerseRange range = (VerseRange) it.next();

                if (i > count)
                {
                    remove(range);
                    overflow = true;
                }
                else
                {
                    remainder.remove(range);
                }
            }

            lowerNormalizeProtection();
            // The event notification is done by the remove above

            if (overflow)
            {
                return remainder;
            }
            else
            {
                return null;
            }
        }
        catch (CloneNotSupportedException ex)
        {
            assert false : ex;
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#addAll(org.crosswire.jsword.passage.Passage)
     */
    public void addAll(Passage that)
    {
        optimizeWrites();
        raiseEventSuppresion();
        raiseNormalizeProtection();

        Iterator that_it = null;

        if (that instanceof RangedPassage)
        {
            that_it = that.rangeIterator(PassageConstants.RESTRICT_NONE);
        }
        else
        {
            that_it = that.verseIterator();
        }

        while (that_it.hasNext())
        {
            // Avoid touching store to make thread safety easier.
            add((VerseBase) that_it.next());
        }

        lowerNormalizeProtection();
        if (lowerEventSuppresionAndTest())
        {
            fireIntervalAdded(this, that.getVerseAt(0), that.getVerseAt(that.countVerses() - 1));
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#removeAll(org.crosswire.jsword.passage.Passage)
     */
    public void removeAll(Passage that)
    {
        optimizeWrites();
        raiseEventSuppresion();
        raiseNormalizeProtection();

        Iterator that_it = null;

        if (that instanceof RangedPassage)
        {
            that_it = that.rangeIterator(PassageConstants.RESTRICT_NONE);
        }
        else
        {
            that_it = that.verseIterator();
        }

        while (that_it.hasNext())
        {
            // Avoid touching store to make thread safety easier.
            remove((VerseBase) that_it.next());
        }

        lowerNormalizeProtection();
        if (lowerEventSuppresionAndTest())
        {
            fireIntervalRemoved(this, that.getVerseAt(0), that.getVerseAt(that.countVerses()-1));
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#retainAll(org.crosswire.jsword.passage.Passage)
     */
    public void retainAll(Passage that)
    {
        optimizeWrites();
        raiseEventSuppresion();
        raiseNormalizeProtection();

        try
        {
            Passage temp = (Passage) this.clone();
            Iterator it = temp.verseIterator();

            while (it.hasNext())
            {
                Verse verse = (Verse) it.next();
                if (!that.contains(verse))
                {
                    remove(verse);
                }
            }

            lowerNormalizeProtection();
            if (lowerEventSuppresionAndTest())
            {
                fireIntervalRemoved(this, null, null);
            }
        }
        catch (CloneNotSupportedException ex)
        {
            assert false : ex;
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#clear()
     */
    public void clear()
    {
        optimizeWrites();
        raiseNormalizeProtection();

        remove(VerseRange.getWholeBibleVerseRange());

        if (lowerEventSuppresionAndTest())
        {
            fireIntervalRemoved(this, null, null);
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#blur(int, int)
     */
    public void blur(int verses, int restrict)
    {
        optimizeWrites();
        raiseEventSuppresion();
        raiseNormalizeProtection();

        try
        {
            Passage temp = (Passage) this.clone();
            Iterator it = temp.rangeIterator(PassageConstants.RESTRICT_NONE);

            while (it.hasNext())
            {
                VerseRange range = new VerseRange((VerseRange) it.next(), verses, verses, restrict);
                add(range);
            }

            lowerNormalizeProtection();
            if (lowerEventSuppresionAndTest())
            {
                fireIntervalAdded(this, null, null);
            }
        }
        catch (CloneNotSupportedException ex)
        {
            assert false : ex;
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#writeDescription(java.io.Writer)
     */
    public void writeDescription(Writer out) throws IOException
    {
        BufferedWriter bout = new BufferedWriter(out);

        Iterator it = rangeIterator(PassageConstants.RESTRICT_NONE);

        while (it.hasNext())
        {
            VerseRange range = (VerseRange) it.next();
            bout.write(range.getName());
            bout.newLine();
        }

        bout.flush();
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#readDescription(java.io.Reader)
     */
    public void readDescription(Reader in) throws IOException, NoSuchVerseException
    {
        raiseEventSuppresion();
        raiseNormalizeProtection();

        int count = 0; // number of lines read
        BufferedReader bin = new BufferedReader(in);
        while (true)
        {
            String line = bin.readLine();
            if (line == null)
            {
                break;
            }

            count++;
            addVerses(line);
        }

        // If the file was empty then there is nothing to do
        if (count == 0)
        {
            return;
        }

        lowerNormalizeProtection();
        if (lowerEventSuppresionAndTest())
        {
            fireIntervalAdded(this, getVerseAt(0), getVerseAt(countVerses() - 1));
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#optimizeReads()
     */
    public void optimizeReads()
    {
    }

    /**
     * Simple method to instruct children to stop caching results
     */
    protected void optimizeWrites()
    {
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#addPassageListener(org.crosswire.jsword.passage.PassageListener)
     */
    public void addPassageListener(PassageListener li)
    {
        synchronized (listeners)
        {
            listeners.add(li);
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Passage#removePassageListener(org.crosswire.jsword.passage.PassageListener)
     */
    public void removePassageListener(PassageListener li)
    {
        synchronized (listeners)
        {
            listeners.remove(li);
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyList#add(org.crosswire.jsword.passage.Key)
     */
    public void add(Key key)
    {
        Passage ref = PassageUtil.getPassage(key);
        addAll(ref);
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyList#remove(org.crosswire.jsword.passage.Key)
     */
    public void remove(Key key)
    {
        Passage ref = PassageUtil.getPassage(key);
        removeAll(ref);
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyList#contains(org.crosswire.jsword.passage.Key)
     */
    public boolean contains(Key key)
    {
        Passage ref = PassageUtil.getPassage(key);
        return containsAll(ref);
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyList#size()
     */
    public int size()
    {
        return countVerses();
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyList#iterator()
     */
    public Iterator iterator()
    {
        return verseIterator();
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyList#indexOf(org.crosswire.jsword.passage.Key)
     */
    public int indexOf(Key that)
    {
        int index = 0;

        for (Iterator it = iterator(); it.hasNext(); )
        {
            Verse verse = (Verse) it.next();
            if (verse.equals(that))
            {
                return index;
            }

            index++;
        }

        return -1;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyList#get(int)
     */
    public Key get(int index)
    {
        return getVerseAt(index);
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getParent()
     */
    public Key getParent()
    {
        return parent;
    }

    /**
     * Set a parent Key. This allows us to follow the Key interface more
     * closely, although the concept of a parent for a verse is fairly
     * alien.
     * @param parent The parent Key for this verse
     */
    public void setParent(Key parent)
    {
        this.parent = parent;
    }

    /**
     * AbstractPassage subclasses must call this method <b>after</b> one
     * or more elements of the list are added.  The changed elements are
     * specified by a closed interval from start to end.
     * @param source The thing that changed, typically "this".
     * @param start One end of the new interval.
     * @param end The other end of the new interval.
     * @see PassageListener
     */
    protected void fireIntervalAdded(Object source, Verse start, Verse end)
    {
        if (suppressEvents != 0)
        {
            return;
        }

        // Create Event
        PassageEvent ev = new PassageEvent(source, PassageEvent.VERSES_ADDED, start, end);

        // Copy listener vector so it won't change while firing
        List temp;
        synchronized (listeners)
        {
            temp = new ArrayList();
            temp.addAll(listeners);
        }

        // And run throught the list shouting
        for (int i=0; i<temp.size(); i++)
        {
            PassageListener rl = (PassageListener) temp.get(i);
            rl.versesAdded(ev);
        }
    }

    /**
     * AbstractPassage subclasses must call this method <b>before</b> one
     * or more elements of the list are added.  The changed elements are
     * specified by a closed interval from start to end.
     * @param source The thing that changed, typically "this".
     * @param start One end of the new interval.
     * @param end The other end of the new interval.
     * @see PassageListener
     */
    protected void fireIntervalRemoved(Object source, Verse start, Verse end)
    {
        if (suppressEvents != 0)
        {
            return;
        }

        // Create Event
        PassageEvent ev = new PassageEvent(source, PassageEvent.VERSES_REMOVED, start, end);

        // Copy listener vector so it won't change while firing
        List temp;
        synchronized (listeners)
        {
            temp = new ArrayList();
            temp.addAll(listeners);
        }

        // And run throught the list shouting
        for (int i=0; i<temp.size(); i++)
        {
            PassageListener rl = (PassageListener) temp.get(i);
            rl.versesRemoved(ev);
        }
    }

    /**
     * AbstractPassage subclasses must call this method <b>before</b> one
     * or more elements of the list are added.  The changed elements are
     * specified by a closed interval from start to end.
     * @param source The thing that changed, typically "this".
     * @param start One end of the new interval.
     * @param end The other end of the new interval.
     * @see PassageListener
     */
    protected void fireContentsChanged(Object source, Verse start, Verse end)
    {
        if (suppressEvents != 0)
        {
            return;
        }

        // Create Event
        PassageEvent ev = new PassageEvent(source, PassageEvent.VERSES_CHANGED, start, end);

        // Copy listener vector so it won't change while firing
        List temp;
        synchronized (listeners)
        {
            temp = new ArrayList();
            temp.addAll(listeners);
        }

        // And run throught the list shouting
        for (int i=0; i<temp.size(); i++)
        {
            PassageListener rl = (PassageListener) temp.get(i);
            rl.versesChanged(ev);
        }
    }

    /**
     * Create a Passage from a human readable string. The opposite of
     * <code>toString()</code>. Since this method is not public it
     * leaves control of <code>suppress_events<code> up to the people
     * that call it.
     * @param refs A String containing the text of the RangedPassage
     * @throws NoSuchVerseException if the string is invalid
     */
    protected void addVerses(String refs) throws NoSuchVerseException
    {
        optimizeWrites();

        String[] parts = PassageUtil.tokenize(refs, PassageConstants.REF_ALLOWED_DELIMS);
        if (parts.length == 0) return;

        // We treat the first as a special case because there is
        // nothing to sensibly base this reference on
        VerseRange basis = new VerseRange(parts[0].trim());
        add(basis);

        // Loop for the other verses, interpreting each on the
        // basis of the one before.
        for (int i=1; i<parts.length; i++)
        {
            VerseRange next = new VerseRange(parts[i].trim(), basis);
            add(next);
            basis = next;
        }
    }

    /**
     * We sometimes need to sort ourselves out ...
     * I don't think we need to be synchronised since we are private
     * and we could check that all public calling of normalize() are
     * synchronised, however this is safe, and I don't think there is
     * a cost associated with a double synchronize. (?)
     */
    protected void normalize()
    {
        // before doing any normalization we should be checking that
        // skip_normalization == 0, and just returning if so.
    }

    /**
     * If things want to prevent normalization because they are doing
     * a set of changes that should be normalized in one go, this is
     * what to call. Be sure to call lowerNormalizeProtection() when
     * you are done.
     */
    protected void raiseNormalizeProtection()
    {
        skipNormalization++;

        if (skipNormalization > 10)
        {
            // This is a bit drastic and does not give us much
            // chance to fix the error
            //   throw new LogicError();

            log.warn("skip_normalization="+skipNormalization, new Exception()); //$NON-NLS-1$
        }
    }

    /**
     * If things want to prevent normalization because they are doing
     * a set of changes that should be normalized in one go, they should
     * call raiseNormalizeProtection() and when done call this. This also
     * calls normalize() if the count reaches zero.
     */
    protected void lowerNormalizeProtection()
    {
        skipNormalization--;

        if (skipNormalization == 0)
        {
            normalize();
        }

        assert skipNormalization >= 0;
    }

    /**
     * If things want to prevent event firing because they are doing
     * a set of changes that should be notified in one go, this is
     * what to call. Be sure to call lowerEventSuppression() when
     * you are done.
     */
    protected void raiseEventSuppresion()
    {
        suppressEvents++;

        if (suppressEvents > 10)
        {
            // This is a bit drastic and does not give us much
            // chance to fix the error
            //   throw new LogicError();

            log.warn("suppress_events="+suppressEvents, new Exception()); //$NON-NLS-1$
        }
    }

    /**
     * If things want to prevent event firing because they are doing
     * a set of changes that should be notified in one go, they should
     * call raiseEventSuppression() and when done call this.
     * @return true if it is then safe to fire an event.
     */
    protected boolean lowerEventSuppresionAndTest()
    {
        suppressEvents--;
        assert suppressEvents >= 0;

        return (suppressEvents == 0);
    }

    /**
     * Convert the Object to a VerseRange. If base is a Verse then return a
     * VerseRange of zero length.
     * @param base The object to be cast
     * @return The VerseRange
     * @exception java.lang.ClassCastException If this is not a Verse or a VerseRange
     */
    protected static VerseRange toVerseRange(Object base) throws ClassCastException
    {
        if (base == null)
        {
            throw new NullPointerException();
        }

        if (base instanceof VerseRange)
        {
            return (VerseRange) base;
        }
        else if (base instanceof Verse)
        {
            return new VerseRange((Verse) base);
        }

        throw new ClassCastException(PassageUtil.getResource(Msg.ABSTRACT_CAST));
    }

    /**
     * Convert the Object to an array of Verses. If base is a VerseRange then return a
     * Verse array of the VersesRanges Verses.
     * @param base The Object to be cast
     * @return The Verse array
     * @exception java.lang.ClassCastException If this is not a Verse or a VerseRange
     */
    protected static Verse[] toVerseArray(Object base) throws ClassCastException
    {
        if (base == null)
        {
            throw new NullPointerException();
        }

        if (base instanceof VerseRange)
        {
            VerseRange range = (VerseRange) base;
            return range.toVerseArray();
        }
        else if (base instanceof Verse)
        {
            return new Verse[] { (Verse) base };
        }

        throw new ClassCastException(PassageUtil.getResource(Msg.ABSTRACT_CAST));
    }

    /**
     * Skip over verses that are part of a range
     */
    protected static final class VerseRangeIterator implements Iterator
    {
        /**
         * iterate, amalgumating Verses into VerseRanges
         */
        protected VerseRangeIterator(Iterator it, int restrict)
        {
            this.it = it;
            this.restrict = restrict;

            if (it.hasNext())
            {
                next_verse = (Verse) it.next();
            }

            calculateNext();
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        public final boolean hasNext()
        {
            return next_range != null;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        public final Object next() throws NoSuchElementException
        {
            Object retcode = next_range;

            if (retcode == null)
            {
                throw new NoSuchElementException();
            }

            calculateNext();
            return retcode;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        public void remove() throws UnsupportedOperationException
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Find the next VerseRange
         */
        private void calculateNext()
        {
            if (next_verse == null)
            {
                next_range = null;
                return;
            }

            Verse start = next_verse;
            Verse end = next_verse;

            findnext:
            while (true)
            {
                if (!it.hasNext())
                {
                    next_verse = null;
                    break;
                }

                next_verse = (Verse) it.next();

                // If the next verse adjacent
                if (!end.adjacentTo(next_verse))
                {
                    break;
                }

                // Even if the next verse is adjacent we might want to break
                // if we have moved into a new chapter/book
                switch (restrict)
                {
                case PassageConstants.RESTRICT_NONE:
                    break;

                case PassageConstants.RESTRICT_BOOK:
                    if (!end.isSameBook(next_verse))
                    {
                        break findnext;
                    }
                    break;

                case PassageConstants.RESTRICT_CHAPTER:
                    if (!end.isSameChapter(next_verse))
                    {
                        break findnext;
                    }
                    break;

                default:
                    assert false;
                }

                end = next_verse;
            }

            next_range = new VerseRange(start, end);
        }

        /**
         * The Iterator that we are proxying to
         */
        private Iterator it;

        /**
         * What is the next VerseRange to be considered
         */
        private VerseRange next_range = null;

        /**
         * What is the next Verse to be considered
         */
        private Verse next_verse = null;

        /**
         * Do we restrict ranges to not crossing chapter boundries
         */
        private int restrict = PassageConstants.RESTRICT_NONE;
    }

    /**
     * Write out the object to the given ObjectOutputStream. There are 3
     * ways of doing this - according to the 3 implementations of
     * Passage.<ul>
     * <li>Distinct: If we write out a list if verse ordinals then the
     *     space used is 4 bytes per verse.
     * <li>Bitwise: If we write out a bitmap then the space used is
     *     something like 31104/8 = 4k bytes.
     * <li>Ranged: The we write a list of start/end pairs then the space
     *     used is 8 bytes per range.
     * </ul>
     * Since we can take our time about this section, we calculate the
     * optimal storage method before we do the saving. If some methods
     * come out equal first then bitwise is preferred, then distinct,
     * then ranged, because I imagine that for speed of de-serialization
     * this is the sensible order. I've not tested it though.
     * @param out The stream to write our state to
     * @throws IOException if the read fails
     */
    protected void writeObjectSupport(ObjectOutputStream out) throws IOException
    {
        // This allows our children to have default serializable fields
        // even though we have none.
        out.defaultWriteObject();

        // the size in bits of teach storage method
        int bitwise_size = BibleInfo.versesInBible();
        int ranged_size =  8 * countRanges(PassageConstants.RESTRICT_NONE);
        int distinct_size = 4 * countVerses();

        // if bitwise is equal smallest
        if (bitwise_size <= ranged_size && bitwise_size <= distinct_size)
        {
            out.writeInt(BITWISE);

            BitSet store = new BitSet(BibleInfo.versesInBible());
            Iterator it = verseIterator();
            while (it.hasNext())
            {
                Verse verse = (Verse) it.next();
                store.set(verse.getOrdinal()-1);
            }

            out.writeObject(store);
        }
        // if distinct is not bigger than ranged
        else if (distinct_size <= ranged_size)
        {
            // write the Passage type and the number of verses
            out.writeInt(DISTINCT);
            out.writeInt(countVerses());

            // write the verse ordinals in a loop
            Iterator it = verseIterator();
            while (it.hasNext())
            {
                Verse verse = (Verse) it.next();
                out.writeInt(verse.getOrdinal());
            }
        }
        // otherwise use ranges
        else
        {
            // write the Passage type and the number of ranges
            out.writeInt(RANGED);
            out.writeInt(countRanges(PassageConstants.RESTRICT_NONE));

            // write the verse ordinals in a loop
            Iterator it = rangeIterator(PassageConstants.RESTRICT_NONE);
            while (it.hasNext())
            {
                VerseRange range = (VerseRange) it.next();
                out.writeInt(range.getStart().getOrdinal());
                out.writeInt(range.getVerseCount());
            }
        }

        // Ignore the original name. Is this wise?
        // I am expecting that people are not that fussed about it and
        // it could make everything far more verbose
    }

    /**
     * Write out the object to the given ObjectOutputStream
     * @param in The stream to read our state from
     * @throws IOException if the read fails
     * @throws ClassNotFoundException If the read data is incorrect
     */
    protected void readObjectSupport(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        raiseEventSuppresion();
        raiseNormalizeProtection();

        // This allows our children to have default serializable fields
        // even though we have none.
        in.defaultReadObject();

        // Setup
        listeners = new Vector();

        try
        {
            int type = in.readInt();
            switch (type)
            {
            case BITWISE:
                BitSet store = (BitSet) in.readObject();
                for (int i=0; i<BibleInfo.versesInBible(); i++)
                {
                    if (store.get(i))
                    {
                        add(new Verse(i+1));
                    }
                }
                break;

            case DISTINCT:
                int verses = in.readInt();
                for (int i=0; i<verses; i++)
                {
                    int ord = in.readInt();
                    add(new Verse(ord));
                }
                break;

            case RANGED:
                int ranges = in.readInt();
                for (int i=0; i<ranges; i++)
                {
                    int ord = in.readInt();
                    int count = in.readInt();
                    add(new VerseRange(new Verse(ord), count));
                }
                break;

            default:
                throw new ClassCastException(PassageUtil.getResource(Msg.ABSTRACT_CAST));
            }
        }
        catch (NoSuchVerseException ex)
        {
            throw new IOException(ex.getMessage());
        }

        // We are ignoring the original_name. It was set to null in the
        // default ctor so I will ignore it here.

        // We don't bother to call fireContentsChanged(...) because
        // nothing can have registered at this point
        lowerEventSuppresionAndTest();
        lowerNormalizeProtection();
    }

    /**
     * The log stream
     */
    private static final Logger log = Logger.getLogger(AbstractPassage.class);

    /**
     * Serialization type constant for a BitWise layout
     */
    protected static final int BITWISE = 0;

    /**
     * Serialization type constant for a Distinct layout
     */
    protected static final int DISTINCT = 1;

    /**
     * Serialization type constant for a Ranged layout
     */
    protected static final int RANGED = 2;

    /**
     * Count of serializations methods
     */
    protected static final int METHOD_COUNT = 3;

    /**
     * The parent key. See the key interface for more information.
     * NOTE(joe): These keys are not serialized, should we?
     * @see Key
     */
    private transient Key parent;

    /**
     * Support for change notification
     */
    protected transient List listeners = new Vector();

    /**
     * The original string for picky users
     */
    protected transient String originalName = null;

    /**
     * If we have several changes to make then we increment this and then
     * decrement it when done (and fire an event off). If the cost of
     * calculating the parameters to the fire is high then we can check that
     * this is 0 before doing the calculation.
     */
    protected transient int suppressEvents = 0;

    /**
     * Do we skip normalization for now - if we want to skip then we increment
     * this, and the decrement it when done.
     */
    protected transient int skipNormalization = 0;
}
