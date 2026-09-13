package org.cache.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryListTest {

    @Test
    @DisplayName("a fresh list is empty")
    void freshListIsEmpty() {
        EntryList<String, String> list = new EntryList<>();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertNull(list.first());
        assertNull(list.last());
        assertNull(list.pollFirst());
        assertNull(list.pollLast());
    }

    @Test
    @DisplayName("addFirst and addLast put entries on the right end")
    void addsOnBothEnds() {
        EntryList<String, String> list = new EntryList<>();
        Entry<String, String> a = new Entry<>("a", "1");
        Entry<String, String> b = new Entry<>("b", "2");
        Entry<String, String> c = new Entry<>("c", "3");

        list.addFirst(b);
        list.addFirst(a);
        list.addLast(c);

        assertEquals(3, list.size());
        assertSame(a, list.first());
        assertSame(c, list.last());
    }

    @Test
    @DisplayName("remove unlinks an entry and leaves the list consistent")
    void removeUnlinksEntry() {
        EntryList<String, String> list = new EntryList<>();
        Entry<String, String> a = new Entry<>("a", "1");
        Entry<String, String> b = new Entry<>("b", "2");
        Entry<String, String> c = new Entry<>("c", "3");
        list.addLast(a);
        list.addLast(b);
        list.addLast(c);

        list.remove(b);

        assertEquals(2, list.size());
        assertSame(a, list.first());
        assertSame(c, list.last());
        assertNull(b.owner());
    }

    @Test
    @DisplayName("removing an entry that belongs to another list is ignored")
    void removeIgnoresForeignEntry() {
        EntryList<String, String> first = new EntryList<>();
        EntryList<String, String> second = new EntryList<>();
        Entry<String, String> entry = new Entry<>("a", "1");
        first.addFirst(entry);

        second.remove(entry);

        assertEquals(1, first.size());
        assertEquals(0, second.size());
        assertSame(first, entry.owner());
    }

    @Test
    @DisplayName("moveToFirst promotes an entry, and is a no-op for the head")
    void moveToFirstPromotesEntry() {
        EntryList<String, String> list = new EntryList<>();
        Entry<String, String> a = new Entry<>("a", "1");
        Entry<String, String> b = new Entry<>("b", "2");
        list.addLast(a);
        list.addLast(b);

        list.moveToFirst(b);
        assertSame(b, list.first());

        list.moveToFirst(b);
        assertSame(b, list.first());
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("an entry cannot be linked into two lists at once")
    void doubleLinkingIsRejected() {
        EntryList<String, String> first = new EntryList<>();
        EntryList<String, String> second = new EntryList<>();
        Entry<String, String> entry = new Entry<>("a", "1");
        first.addFirst(entry);

        assertThrows(IllegalStateException.class, () -> second.addFirst(entry));
    }

    @Test
    @DisplayName("clear detaches every entry")
    void clearDetachesEntries() {
        EntryList<String, String> list = new EntryList<>();
        Entry<String, String> a = new Entry<>("a", "1");
        Entry<String, String> b = new Entry<>("b", "2");
        list.addLast(a);
        list.addLast(b);

        list.clear();

        assertEquals(0, list.size());
        assertNull(a.owner());
        assertNull(b.owner());

        list.addFirst(a);
        assertSame(a, list.first());
    }

    @Test
    @DisplayName("an entry renders as a key value pair")
    void entryToString() {
        assertEquals("a=1", new Entry<>("a", "1").toString());
    }
}
