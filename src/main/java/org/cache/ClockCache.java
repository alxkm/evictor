package org.cache;

import org.cache.internal.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Clock cache, also known as second chance replacement.
 *
 * <p>Entries live in a fixed ring of slots. Each slot carries a reference bit that a read sets to
 * true. On eviction a hand walks the ring: a slot with the bit set gets a second chance, the bit
 * is cleared and the hand moves on; the first slot found with the bit already cleared is the
 * victim. The result approximates LRU while touching a single boolean per read instead of
 * relinking a list, which is why operating system page replacement is built on it.
 *
 * <p>Complexity: {@code get} and {@code evict} are O(1), {@code put} is O(1) amortised. A single
 * eviction scans at most two full turns of the hand. Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class ClockCache<K, V> implements CacheService<K, V> {

    private final int capacity;
    private final Map<K, Integer> slotByKey;
    private final Object[] keys;
    private final Object[] values;
    private final boolean[] referenced;
    private int hand;
    private int size;

    /**
     * Creates a cache holding at most {@code capacity} entries.
     *
     * @param capacity the maximum number of entries, must be positive
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public ClockCache(int capacity) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        this.slotByKey = new HashMap<>(capacity);
        this.keys = new Object[capacity];
        this.values = new Object[capacity];
        this.referenced = new boolean[capacity];
    }

    @Override
    public void put(K id, V value) {
        Integer slot = slotByKey.get(id);
        if (slot != null) {
            values[slot] = value;
            referenced[slot] = true;
            return;
        }
        int target = size < capacity ? firstFreeSlot() : evictVictim();
        keys[target] = id;
        values[target] = value;
        referenced[target] = false;
        slotByKey.put(id, target);
        size++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(K id) {
        Integer slot = slotByKey.get(id);
        if (slot == null) {
            return null;
        }
        referenced[slot] = true;
        return (V) values[slot];
    }

    @Override
    public void evict(K id) {
        Integer slot = slotByKey.remove(id);
        if (slot != null) {
            free(slot);
        }
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
        return slotByKey.containsKey(id);
    }

    @Override
    public void clear() {
        slotByKey.clear();
        for (int i = 0; i < capacity; i++) {
            free(i);
        }
        hand = 0;
        size = 0;
    }

    /** Finds a slot that holds no entry. Only called while the ring has room. */
    private int firstFreeSlot() {
        for (int offset = 0; offset < capacity; offset++) {
            int slot = (hand + offset) % capacity;
            if (keys[slot] == null) {
                hand = (slot + 1) % capacity;
                return slot;
            }
        }
        throw new IllegalStateException("no free slot while size " + size + " is below capacity " + capacity);
    }

    /** Advances the hand until an unreferenced slot shows up and frees it. */
    private int evictVictim() {
        while (true) {
            int slot = hand;
            hand = (hand + 1) % capacity;
            if (keys[slot] == null) {
                return slot;
            }
            if (referenced[slot]) {
                referenced[slot] = false;
                continue;
            }
            slotByKey.remove(keys[slot]);
            free(slot);
            return slot;
        }
    }

    private void free(int slot) {
        if (keys[slot] != null) {
            size--;
        }
        keys[slot] = null;
        values[slot] = null;
        referenced[slot] = false;
    }
}
