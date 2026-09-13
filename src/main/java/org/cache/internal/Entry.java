package org.cache.internal;

/**
 * A key-value pair that can be linked into an {@link EntryList}.
 *
 * <p>This is an implementation detail shared by the linked list based caches. It is not part
 * of the public API and may change without notice.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
public final class Entry<K, V> {

    /** The key of this entry, {@code null} for the sentinel head and tail nodes. */
    public final K key;

    /** The value of this entry. */
    public V value;

    /** Access counter, used by the frequency based policies. */
    public int frequency;

    /** Reference bit, used by the clock (second chance) policy. */
    public boolean referenced;

    /** Expiration timestamp in nanoseconds, used by the time based policies. */
    public long expiresAtNanos;

    Entry<K, V> prev;
    Entry<K, V> next;
    EntryList<K, V> owner;

    /**
     * Creates a detached entry with a frequency of one.
     *
     * @param key   the key of the entry
     * @param value the value of the entry
     */
    public Entry(K key, V value) {
        this.key = key;
        this.value = value;
        this.frequency = 1;
    }

    /**
     * Returns the list this entry currently belongs to.
     *
     * @return the owning list, or {@code null} when the entry is detached
     */
    public EntryList<K, V> owner() {
        return owner;
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}
