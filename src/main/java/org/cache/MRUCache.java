package org.cache;

import org.cache.internal.Entry;
import org.cache.internal.EntryList;
import org.cache.internal.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * MRU (Most Recently Used) cache: the entry touched last is the first one to go.
 *
 * <p>The policy looks backwards at first sight, and for a general workload it is. It pays off for
 * cyclic scans over a data set larger than the cache, where LRU evicts exactly the block that is
 * needed next. Database buffer pools use it for that reason.
 *
 * <p>All operations run in O(1). Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class MRUCache<K, V> implements CacheService<K, V> {

    private final int capacity;
    private final Map<K, Entry<K, V>> cache;
    private final EntryList<K, V> accessOrder = new EntryList<>();

    /**
     * Creates a cache holding at most {@code capacity} entries.
     *
     * @param capacity the maximum number of entries, must be positive
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public MRUCache(int capacity) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        this.cache = new HashMap<>(capacity);
    }

    @Override
    public void put(K id, V value) {
        Entry<K, V> existing = cache.get(id);
        if (existing != null) {
            existing.value = value;
            accessOrder.moveToFirst(existing);
            return;
        }
        if (cache.size() == capacity) {
            Entry<K, V> victim = accessOrder.pollFirst();
            if (victim != null) {
                cache.remove(victim.key);
            }
        }
        Entry<K, V> entry = new Entry<>(id, value);
        cache.put(id, entry);
        accessOrder.addFirst(entry);
    }

    @Override
    public V get(K id) {
        Entry<K, V> entry = cache.get(id);
        if (entry == null) {
            return null;
        }
        accessOrder.moveToFirst(entry);
        return entry.value;
    }

    @Override
    public void evict(K id) {
        Entry<K, V> entry = cache.remove(id);
        if (entry != null) {
            accessOrder.remove(entry);
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
        accessOrder.clear();
    }
}
