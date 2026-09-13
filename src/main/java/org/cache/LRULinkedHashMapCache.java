package org.cache;

import org.cache.internal.Preconditions;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU (Least Recently Used) cache backed by an access ordered {@link LinkedHashMap}.
 *
 * <p>The map itself maintains the access order and reports the eldest entry through
 * {@code removeEldestEntry}, so the cache is a thin wrapper with no extra bookkeeping.
 * This is the shortest of the three LRU variants and the one to reach for in production code.
 *
 * <p>All operations run in O(1). Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class LRULinkedHashMapCache<K, V> implements CacheService<K, V> {

    private static final float LOAD_FACTOR = 0.75F;

    private final int capacity;
    private final LinkedHashMap<K, V> entries;

    /**
     * Creates a cache holding at most {@code capacity} entries.
     *
     * @param capacity the maximum number of entries, must be positive
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public LRULinkedHashMapCache(int capacity) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        this.entries = new LinkedHashMap<>(capacity, LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                // super.size() is the map's own size. Unqualified it would still bind to the
                // inherited method, but the enclosing cache declares size() too, so spell it out.
                return super.size() > LRULinkedHashMapCache.this.capacity;
            }
        };
    }

    @Override
    public void put(K id, V value) {
        entries.put(id, value);
    }

    @Override
    public V get(K id) {
        return entries.get(id);
    }

    @Override
    public void evict(K id) {
        entries.remove(id);
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public boolean containsKey(K id) {
        return entries.containsKey(id);
    }

    @Override
    public void clear() {
        entries.clear();
    }
}
