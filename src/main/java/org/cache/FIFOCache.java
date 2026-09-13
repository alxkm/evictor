package org.cache;

import org.cache.internal.Entry;
import org.cache.internal.EntryList;
import org.cache.internal.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * FIFO (First In First Out) cache: the entry that was inserted first is evicted first.
 *
 * <p>Reads do not change the eviction order, which is the whole point of the policy. It ignores
 * how useful an entry is, so its hit rate is below LRU on most workloads, but it needs no
 * bookkeeping on the read path at all. That makes it a reasonable pick for caches that are
 * written once and read many times, and a common baseline when comparing policies.
 *
 * <p>Re-inserting an existing key replaces the value and keeps the original insertion position.
 *
 * <p>All operations run in O(1). Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class FIFOCache<K, V> implements CacheService<K, V> {

    private final int capacity;
    private final Map<K, Entry<K, V>> cache;
    private final EntryList<K, V> insertionOrder = new EntryList<>();

    /**
     * Creates a cache holding at most {@code capacity} entries.
     *
     * @param capacity the maximum number of entries, must be positive
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public FIFOCache(int capacity) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        this.cache = new HashMap<>();
    }

    @Override
    public void put(K id, V value) {
        Entry<K, V> existing = cache.get(id);
        if (existing != null) {
            existing.value = value;
            return;
        }
        if (cache.size() == capacity) {
            Entry<K, V> victim = insertionOrder.pollLast();
            if (victim != null) {
                cache.remove(victim.key);
            }
        }
        Entry<K, V> entry = new Entry<>(id, value);
        cache.put(id, entry);
        insertionOrder.addFirst(entry);
    }

    @Override
    public V get(K id) {
        Entry<K, V> entry = cache.get(id);
        return entry == null ? null : entry.value;
    }

    @Override
    public void evict(K id) {
        Entry<K, V> entry = cache.remove(id);
        if (entry != null) {
            insertionOrder.remove(entry);
        }
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
        insertionOrder.clear();
    }
}
