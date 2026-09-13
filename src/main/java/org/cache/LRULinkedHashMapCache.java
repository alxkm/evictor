package org.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU (Least Recently Used) Cache implementation using {@link LinkedHashMap}.
 * This cache automatically removes the least recently used entries when the capacity is exceeded.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class LRULinkedHashMapCache<K, V> implements CacheService<K, V> {

    /**
     * The underlying LinkedHashMap that stores the cache entries.
     */
    private final LinkedHashMap<K, V> linkHashMap;

    /**
     * Constructor to initialize LRU Cache with the specified capacity.
     * The cache will automatically remove the least recently used entries when the capacity is exceeded.
     *
     * @param capacity the maximum number of elements the cache can hold
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public LRULinkedHashMapCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive, got " + capacity);
        }
        linkHashMap = new LinkedHashMap<>(capacity, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    /**
     * Inserts the specified key-value pair into the cache.
     * If the cache previously contained a mapping for the key, the old value is replaced.
     * If inserting the new pair exceeds the cache's capacity, the least recently used entry is removed.
     *
     * @param id    the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    @Override
    public void put(K id, V value) {
        linkHashMap.put(id, value);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this cache contains no mapping for the key.
     * Accessing the key marks it as recently used.
     *
     * @param id the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if this cache contains no mapping for the key
     */
    @Override
    public V get(K id) {
        return linkHashMap.get(id);
    }

    /**
     * Removes the mapping for a key from this cache if it is present.
     *
     * @param id the key whose mapping is to be removed from the cache
     */
    @Override
    public void evict(K id) {
        linkHashMap.remove(id);
    }
}