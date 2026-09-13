package org.cache;

import org.cache.internal.Entry;
import org.cache.internal.EntryList;
import org.cache.internal.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * LFU (Least Frequently Used) cache that keeps the frequency buckets in a {@link TreeMap}.
 *
 * <p>Compared with {@link LFUDoublyLinkedListCache} this variant does not track the lowest
 * frequency by hand: the sorted map always exposes it through {@code firstEntry}. That costs a
 * logarithmic factor on every operation but removes the trickiest part of the LFU bookkeeping,
 * which makes it a useful reference implementation. Within one frequency the entries are kept in
 * recency order, so ties are broken by evicting the least recently used entry.
 *
 * <p>Complexity: {@code put}, {@code get} and {@code evict} are O(log n) where n is the number of
 * distinct frequencies. Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class LFUTreeMapCache<K, V> implements CacheService<K, V> {

    private final int capacity;
    private final Map<K, Entry<K, V>> cache;
    private final TreeMap<Integer, EntryList<K, V>> frequencyBuckets;

    /**
     * Creates a cache holding at most {@code capacity} entries.
     *
     * @param capacity the maximum number of entries, must be positive
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public LFUTreeMapCache(int capacity) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        this.cache = new HashMap<>();
        this.frequencyBuckets = new TreeMap<>();
    }

    @Override
    public void put(K id, V value) {
        Entry<K, V> existing = cache.get(id);
        if (existing != null) {
            existing.value = value;
            touch(existing);
            return;
        }
        if (cache.size() == capacity) {
            evictLeastFrequent();
        }
        Entry<K, V> entry = new Entry<>(id, value);
        cache.put(id, entry);
        bucket(1).addFirst(entry);
    }

    @Override
    public V get(K id) {
        Entry<K, V> entry = cache.get(id);
        if (entry == null) {
            return null;
        }
        touch(entry);
        return entry.value;
    }

    @Override
    public void evict(K id) {
        Entry<K, V> entry = cache.remove(id);
        if (entry != null) {
            unlink(entry);
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
        frequencyBuckets.clear();
    }

    private void touch(Entry<K, V> entry) {
        unlink(entry);
        entry.frequency++;
        bucket(entry.frequency).addFirst(entry);
    }

    private void unlink(Entry<K, V> entry) {
        EntryList<K, V> list = frequencyBuckets.get(entry.frequency);
        if (list == null) {
            return;
        }
        list.remove(entry);
        if (list.isEmpty()) {
            frequencyBuckets.remove(entry.frequency);
        }
    }

    private void evictLeastFrequent() {
        Map.Entry<Integer, EntryList<K, V>> lowest = frequencyBuckets.firstEntry();
        if (lowest == null) {
            return;
        }
        EntryList<K, V> list = lowest.getValue();
        Entry<K, V> victim = list.pollLast();
        if (victim != null) {
            cache.remove(victim.key);
        }
        if (list.isEmpty()) {
            frequencyBuckets.remove(lowest.getKey());
        }
    }

    private EntryList<K, V> bucket(int frequency) {
        return frequencyBuckets.computeIfAbsent(frequency, unused -> new EntryList<>());
    }
}
