package org.cache;

import org.cache.internal.Preconditions;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * LRU (Least Recently Used) cache built on a {@link HashMap} for the values and a {@link Deque}
 * for the access order.
 *
 * <p>This is the straightforward variant: the deque holds the keys with the most recently used
 * one at the front, and every access moves the key back to the front. The catch is that moving a
 * key means removing it from the middle of the deque, which is a linear scan. It is kept here as
 * a reference point against {@link LRUDoublyLinkedListCache}, which pays constant time for the
 * same job by storing the list node next to the value.
 *
 * <p>Complexity: {@code put} and {@code get} are O(n) on a hit because of the deque scan,
 * {@code put} of a new key is O(1) amortised. Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class LRUHashMapQueueCache<K, V> implements CacheService<K, V> {

    private final int capacity;
    private final Map<K, V> cacheMap;
    private final Deque<K> accessOrder;

    /**
     * Creates a cache holding at most {@code capacity} entries.
     *
     * @param capacity the maximum number of entries, must be positive
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public LRUHashMapQueueCache(int capacity) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        this.cacheMap = new HashMap<>();
        this.accessOrder = new ArrayDeque<>(capacity);
    }

    @Override
    public void put(K id, V value) {
        if (cacheMap.containsKey(id)) {
            accessOrder.remove(id);
        } else if (cacheMap.size() == capacity) {
            K leastUsedKey = accessOrder.removeLast();
            cacheMap.remove(leastUsedKey);
        }
        accessOrder.addFirst(id);
        cacheMap.put(id, value);
    }

    @Override
    public V get(K id) {
        V value = cacheMap.get(id);
        if (value == null) {
            return null;
        }
        accessOrder.remove(id);
        accessOrder.addFirst(id);
        return value;
    }

    @Override
    public void evict(K id) {
        if (cacheMap.remove(id) != null) {
            accessOrder.remove(id);
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
