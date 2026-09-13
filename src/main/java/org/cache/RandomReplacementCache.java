package org.cache;

import org.cache.internal.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Random replacement cache: when the cache is full a uniformly random entry is dropped.
 *
 * <p>No recency or frequency is tracked, so reads are pure map lookups and there is no metadata to
 * update or contend on. On workloads without a strong locality pattern its hit rate lands
 * surprisingly close to LRU, which makes it a fair baseline: a policy that cannot beat random is
 * not paying for itself. ARM and x86 caches use the same idea in hardware.
 *
 * <p>The key list is kept in a flat array; removing a key swaps the last element into the freed
 * position, so every operation stays O(1). Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class RandomReplacementCache<K, V> implements CacheService<K, V> {

    private final int capacity;
    private final Map<K, V> values;
    private final Map<K, Integer> positionByKey;
    private final Object[] keys;
    private final Random random;
    private int size;

    /**
     * Creates a cache holding at most {@code capacity} entries.
     *
     * @param capacity the maximum number of entries, must be positive
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public RandomReplacementCache(int capacity) {
        this(capacity, new Random());
    }

    /**
     * Creates a cache with an explicit random source, which makes eviction reproducible in tests.
     *
     * @param capacity the maximum number of entries, must be positive
     * @param random   the source of randomness used to pick a victim
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public RandomReplacementCache(int capacity, Random random) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        this.random = Preconditions.requireNonNull(random, "random");
        this.values = new HashMap<>(capacity);
        this.positionByKey = new HashMap<>(capacity);
        this.keys = new Object[capacity];
    }

    @Override
    public void put(K id, V value) {
        if (values.containsKey(id)) {
            values.put(id, value);
            return;
        }
        if (size == capacity) {
            evictRandom();
        }
        keys[size] = id;
        positionByKey.put(id, size);
        values.put(id, value);
        size++;
    }

    @Override
    public V get(K id) {
        return values.get(id);
    }

    @Override
    public void evict(K id) {
        Integer position = positionByKey.remove(id);
        if (position == null) {
            return;
        }
        values.remove(id);
        removeAt(position);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public boolean containsKey(K id) {
        return values.containsKey(id);
    }

    @Override
    public void clear() {
        values.clear();
        positionByKey.clear();
        for (int i = 0; i < size; i++) {
            keys[i] = null;
        }
        size = 0;
    }

    @SuppressWarnings("unchecked")
    private void evictRandom() {
        int position = random.nextInt(size);
        K victim = (K) keys[position];
        values.remove(victim);
        positionByKey.remove(victim);
        removeAt(position);
    }

    /** Fills the freed position with the last key so the array stays dense. */
    @SuppressWarnings("unchecked")
    private void removeAt(int position) {
        int last = size - 1;
        if (position != last) {
            K moved = (K) keys[last];
            keys[position] = moved;
            positionByKey.put(moved, position);
        }
        keys[last] = null;
        size--;
    }
}
