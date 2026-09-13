package org.cache;

import org.cache.internal.Entry;
import org.cache.internal.EntryList;
import org.cache.internal.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * SLRU (Segmented LRU) cache, an LRU split into a probationary and a protected segment.
 *
 * <p>A new entry always lands in the probationary segment. It is promoted to the protected segment
 * only when it is read a second time, and the protected segment never evicts directly: when it
 * overflows its oldest entry is demoted back to probation. Eviction always takes the oldest
 * probationary entry.
 *
 * <p>The effect is scan resistance. A burst of one-off keys, a full table scan for example, flows
 * through probation and leaves the working set in the protected segment untouched, which is where
 * plain LRU loses its whole contents.
 *
 * <p>All operations run in O(1). Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class SLRUCache<K, V> implements CacheService<K, V> {

    private static final double DEFAULT_PROTECTED_RATIO = 0.8;

    private final int capacity;
    private final int protectedCapacity;
    private final Map<K, Entry<K, V>> cache;
    private final EntryList<K, V> probationary = new EntryList<>();
    private final EntryList<K, V> protectedSegment = new EntryList<>();

    /**
     * Creates a cache that gives 80 percent of its capacity to the protected segment.
     *
     * @param capacity the maximum number of entries, must be positive
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public SLRUCache(int capacity) {
        this(capacity, (int) (Preconditions.positiveCapacity(capacity) * DEFAULT_PROTECTED_RATIO));
    }

    /**
     * Creates a cache with an explicit split between the two segments.
     *
     * @param capacity          the maximum number of entries, must be positive
     * @param protectedCapacity the size of the protected segment, clamped to the range
     *                          {@code [0, capacity - 1]} so that probation always keeps one slot
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public SLRUCache(int capacity, int protectedCapacity) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        this.protectedCapacity = Math.max(0, Math.min(protectedCapacity, capacity - 1));
        this.cache = new HashMap<>(capacity);
    }

    @Override
    public void put(K id, V value) {
        Entry<K, V> existing = cache.get(id);
        if (existing != null) {
            existing.value = value;
            promote(existing);
            return;
        }
        if (cache.size() == capacity) {
            evictOldest();
        }
        Entry<K, V> entry = new Entry<>(id, value);
        cache.put(id, entry);
        probationary.addFirst(entry);
    }

    @Override
    public V get(K id) {
        Entry<K, V> entry = cache.get(id);
        if (entry == null) {
            return null;
        }
        promote(entry);
        return entry.value;
    }

    @Override
    public void evict(K id) {
        Entry<K, V> entry = cache.remove(id);
        if (entry == null) {
            return;
        }
        probationary.remove(entry);
        protectedSegment.remove(entry);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public boolean containsKey(K id) {
        return cache.containsKey(id);
    }

    @Override
    public void clear() {
        cache.clear();
        probationary.clear();
        protectedSegment.clear();
    }

    /**
     * Returns the number of entries that have been read at least twice and now sit in the
     * protected segment.
     *
     * @return the current size of the protected segment
     */
    public int protectedSize() {
        return protectedSegment.size();
    }

    /**
     * Returns the number of entries still on probation.
     *
     * @return the current size of the probationary segment
     */
    public int probationarySize() {
        return probationary.size();
    }

    /** Moves an entry into the protected segment, demoting its oldest member when needed. */
    private void promote(Entry<K, V> entry) {
        if (entry.owner() == protectedSegment) {
            protectedSegment.moveToFirst(entry);
            return;
        }
        probationary.remove(entry);
        if (protectedCapacity == 0) {
            probationary.addFirst(entry);
            return;
        }
        if (protectedSegment.size() >= protectedCapacity) {
            Entry<K, V> demoted = protectedSegment.pollLast();
            if (demoted != null) {
                probationary.addFirst(demoted);
            }
        }
        protectedSegment.addFirst(entry);
    }

    private void evictOldest() {
        Entry<K, V> victim = probationary.pollLast();
        if (victim == null) {
            victim = protectedSegment.pollLast();
        }
        if (victim != null) {
            cache.remove(victim.key);
        }
    }
}
