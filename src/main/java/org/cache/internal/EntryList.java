package org.cache.internal;

/**
 * An intrusive doubly linked list of {@link Entry} nodes with sentinel head and tail.
 *
 * <p>Every operation runs in constant time because each entry carries its own links, so a node
 * can be unlinked without searching for it. The list is the ordering backbone of the LRU, MRU,
 * LFU, FIFO and segmented caches in this library.
 *
 * <p>This is an implementation detail and not part of the public API.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public final class EntryList<K, V> {

    private final Entry<K, V> head = new Entry<>(null, null);
    private final Entry<K, V> tail = new Entry<>(null, null);
    private int size;

    /** Creates an empty list. */
    public EntryList() {
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Links the entry right after the head, making it the newest element.
     *
     * @param entry a detached entry
     */
    public void addFirst(Entry<K, V> entry) {
        linkBetween(entry, head, head.next);
    }

    /**
     * Links the entry right before the tail, making it the oldest element.
     *
     * @param entry a detached entry
     */
    public void addLast(Entry<K, V> entry) {
        linkBetween(entry, tail.prev, tail);
    }

    /**
     * Unlinks the entry from this list. Calling it for a detached entry is a no-op.
     *
     * @param entry the entry to unlink
     */
    public void remove(Entry<K, V> entry) {
        if (entry.owner != this) {
            return;
        }
        entry.prev.next = entry.next;
        entry.next.prev = entry.prev;
        entry.prev = null;
        entry.next = null;
        entry.owner = null;
        size--;
    }

    /**
     * Moves an already linked entry to the front of the list.
     *
     * @param entry the entry to promote
     */
    public void moveToFirst(Entry<K, V> entry) {
        if (head.next == entry) {
            return;
        }
        remove(entry);
        addFirst(entry);
    }

    /**
     * Returns the newest entry without unlinking it.
     *
     * @return the first entry, or {@code null} when the list is empty
     */
    public Entry<K, V> first() {
        return head.next == tail ? null : head.next;
    }

    /**
     * Returns the oldest entry without unlinking it.
     *
     * @return the last entry, or {@code null} when the list is empty
     */
    public Entry<K, V> last() {
        return tail.prev == head ? null : tail.prev;
    }

    /**
     * Unlinks and returns the newest entry.
     *
     * @return the removed entry, or {@code null} when the list is empty
     */
    public Entry<K, V> pollFirst() {
        Entry<K, V> entry = first();
        if (entry != null) {
            remove(entry);
        }
        return entry;
    }

    /**
     * Unlinks and returns the oldest entry.
     *
     * @return the removed entry, or {@code null} when the list is empty
     */
    public Entry<K, V> pollLast() {
        Entry<K, V> entry = last();
        if (entry != null) {
            remove(entry);
        }
        return entry;
    }

    /**
     * Returns the number of linked entries.
     *
     * @return the list size
     */
    public int size() {
        return size;
    }

    /**
     * Returns whether the list holds no entries.
     *
     * @return {@code true} when empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Unlinks every entry. */
    public void clear() {
        Entry<K, V> current = head.next;
        while (current != tail) {
            Entry<K, V> next = current.next;
            current.prev = null;
            current.next = null;
            current.owner = null;
            current = next;
        }
        head.next = tail;
        tail.prev = head;
        size = 0;
    }

    private void linkBetween(Entry<K, V> entry, Entry<K, V> prev, Entry<K, V> next) {
        if (entry.owner != null) {
            throw new IllegalStateException("entry is already linked into a list: " + entry);
        }
        entry.prev = prev;
        entry.next = next;
        prev.next = entry;
        next.prev = entry;
        entry.owner = this;
        size++;
    }
}
