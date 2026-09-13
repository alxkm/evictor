package org.cache;

import org.cache.internal.Entry;
import org.cache.internal.EntryList;
import org.cache.internal.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * LFU (Least Frequently Used) cache with constant time operations.
 *
 * <p>Two maps carry the state: one from key to entry for lookups, and one from an access count to
 * the list of entries with exactly that count. Each bucket keeps its entries in recency order, so
 * when several entries share the lowest frequency the least recently used of them is evicted.
 * The lowest populated frequency is tracked incrementally, which is what keeps eviction O(1).
 *
 * <p>Complexity: {@code put}, {@code get} and {@code evict} are O(1). The only exception is an
 * eviction that directly follows an explicit {@link #evict(Object)} which emptied the lowest
 * bucket. That case needs a one off rescan of the populated frequencies.
 *
 * <p>Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class LFUDoublyLinkedListCache<K, V> implements CacheService<K, V> {

    private final int capacity;
    private final Map<K, Entry<K, V>> cache;
    private final Map<Integer, EntryList<K, V>> frequencyBuckets;
    private int minFrequency;

    /**
     * Creates a cache holding at most {@code capacity} entries.
     *
     * @param capacity the maximum number of entries, must be positive
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public LFUDoublyLinkedListCache(int capacity) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        this.cache = new HashMap<>();
        this.frequencyBuckets = new HashMap<>();
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
        minFrequency = 1;
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
        minFrequency = 0;
    }

    /** Moves the entry into the next frequency bucket and keeps the minimum frequency in sync. */
    private void touch(Entry<K, V> entry) {
        int currentFrequency = entry.frequency;
        unlink(entry);
        entry.frequency = currentFrequency + 1;
        bucket(entry.frequency).addFirst(entry);
        if (currentFrequency == minFrequency && !frequencyBuckets.containsKey(currentFrequency)) {
            minFrequency = entry.frequency;
        }
    }

    /** Removes the entry from its bucket and drops the bucket once it runs empty. */
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
        EntryList<K, V> list = frequencyBuckets.get(minFrequency);
        if (list == null) {
            // The lowest bucket was drained by an explicit evict, so find the new minimum.
            minFrequency = frequencyBuckets.keySet().stream()
                    .mapToInt(Integer::intValue)
                    .min()
                    .orElse(0);
            list = frequencyBuckets.get(minFrequency);
            if (list == null) {
                return;
            }
        }
        Entry<K, V> victim = list.pollLast();
        if (victim != null) {
            cache.remove(victim.key);
            if (list.isEmpty()) {
                frequencyBuckets.remove(minFrequency);
            }
        }
    }

    private EntryList<K, V> bucket(int frequency) {
        return frequencyBuckets.computeIfAbsent(frequency, unused -> new EntryList<>());
    }
}
