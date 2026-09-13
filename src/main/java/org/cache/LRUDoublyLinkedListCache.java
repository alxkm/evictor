package org.cache;

import org.cache.internal.Entry;
import org.cache.internal.EntryList;
import org.cache.internal.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * LRU (Least Recently Used) cache built on a {@link HashMap} plus an intrusive doubly linked list.
 *
 * <p>The map gives constant time lookup by key, the list keeps the entries ordered from the most
 * recently used at the front to the least recently used at the back. Every access moves the entry
 * to the front, so the eviction candidate is always the tail.
 *
 * <p>All operations run in O(1). Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class LRUDoublyLinkedListCache<K, V> implements CacheService<K, V> {

    private final int capacity;
    private final Map<K, Entry<K, V>> cacheMap;
    private final EntryList<K, V> accessOrder = new EntryList<>();

    /**
     * Creates a cache holding at most {@code capacity} entries.
     *
     * @param capacity the maximum number of entries, must be positive
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public LRUDoublyLinkedListCache(int capacity) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        this.cacheMap = new HashMap<>();
    }

    @Override
    public void put(K id, V value) {
        Entry<K, V> existing = cacheMap.get(id);
        if (existing != null) {
            existing.value = value;
            accessOrder.moveToFirst(existing);
            return;
        }
        if (cacheMap.size() == capacity) {
            Entry<K, V> evicted = accessOrder.pollLast();
            if (evicted != null) {
                cacheMap.remove(evicted.key);
            }
        }
        Entry<K, V> entry = new Entry<>(id, value);
        cacheMap.put(id, entry);
        accessOrder.addFirst(entry);
    }

    @Override
    public V get(K id) {
        Entry<K, V> entry = cacheMap.get(id);
        if (entry == null) {
            return null;
        }
        accessOrder.moveToFirst(entry);
        return entry.value;
    }

    @Override
    public void evict(K id) {
        Entry<K, V> entry = cacheMap.remove(id);
        if (entry != null) {
            accessOrder.remove(entry);
        }
    }

    @Override
    public int size() {
        return cacheMap.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public boolean containsKey(K id) {
        return cacheMap.containsKey(id);
    }

    @Override
    public void clear() {
        cacheMap.clear();
        accessOrder.clear();
    }
}
