package org.cache;

import org.cache.internal.Entry;
import org.cache.internal.EntryList;
import org.cache.internal.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * 2Q cache by Johnson and Shasha: LRU quality at FIFO cost, without the scan sensitivity.
 *
 * <p>Three queues do the work. New keys enter {@code A1in}, a FIFO buffer for entries that have
 * been seen once. When {@code A1in} overflows, the evicted key is not forgotten: it is recorded in
 * {@code A1out}, a ghost queue that keeps keys without values. A key that comes back while its
 * ghost is still around has proven it is worth keeping and goes straight into {@code Am}, the
 * main LRU queue.
 *
 * <p>That second sighting is the whole trick. Keys touched exactly once never reach the main
 * queue, so a scan cannot flush the working set, and the cost per access stays constant.
 *
 * <p>All operations run in O(1). Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class TwoQueueCache<K, V> implements CacheService<K, V> {

    private static final double DEFAULT_IN_RATIO = 0.25;
    private static final double DEFAULT_GHOST_RATIO = 0.5;

    private final int capacity;
    private final int inCapacity;
    private final int ghostCapacity;

    private final Map<K, Entry<K, V>> resident;
    private final Map<K, Entry<K, V>> ghosts;
    private final EntryList<K, V> a1in = new EntryList<>();
    private final EntryList<K, V> a1out = new EntryList<>();
    private final EntryList<K, V> am = new EntryList<>();

    /**
     * Creates a cache with the ratios from the original paper: a quarter of the capacity for the
     * FIFO buffer and half of the capacity worth of ghost keys.
     *
     * @param capacity the maximum number of resident entries, must be positive
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public TwoQueueCache(int capacity) {
        this(capacity,
                Math.max(1, (int) (Preconditions.positiveCapacity(capacity) * DEFAULT_IN_RATIO)),
                Math.max(1, (int) (capacity * DEFAULT_GHOST_RATIO)));
    }

    /**
     * Creates a cache with an explicit queue sizing.
     *
     * @param capacity      the maximum number of resident entries, must be positive
     * @param inCapacity    the size of the {@code A1in} FIFO buffer, clamped to {@code [1, capacity]}
     * @param ghostCapacity the number of remembered keys in {@code A1out}, at least one
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public TwoQueueCache(int capacity, int inCapacity, int ghostCapacity) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        this.inCapacity = Math.max(1, Math.min(inCapacity, capacity));
        this.ghostCapacity = Math.max(1, ghostCapacity);
        this.resident = new HashMap<>(capacity);
        this.ghosts = new HashMap<>();
    }

    @Override
    public void put(K id, V value) {
        Entry<K, V> existing = resident.get(id);
        if (existing != null) {
            existing.value = value;
            if (existing.owner() == am) {
                am.moveToFirst(existing);
            }
            return;
        }

        Entry<K, V> ghost = ghosts.remove(id);
        if (ghost != null) {
            // Seen before and requested again: this key belongs in the main queue.
            a1out.remove(ghost);
            makeRoom();
            Entry<K, V> entry = new Entry<>(id, value);
            resident.put(id, entry);
            am.addFirst(entry);
            return;
        }

        makeRoom();
        Entry<K, V> entry = new Entry<>(id, value);
        resident.put(id, entry);
        a1in.addFirst(entry);
    }

    @Override
    public V get(K id) {
        Entry<K, V> entry = resident.get(id);
        if (entry == null) {
            return null;
        }
        // A hit in A1in deliberately does not promote: only a hit after eviction proves value.
        if (entry.owner() == am) {
            am.moveToFirst(entry);
        }
        return entry.value;
    }

    @Override
    public void evict(K id) {
        Entry<K, V> entry = resident.remove(id);
        if (entry != null) {
            a1in.remove(entry);
            am.remove(entry);
            return;
        }
        Entry<K, V> ghost = ghosts.remove(id);
        if (ghost != null) {
            a1out.remove(ghost);
        }
    }

    @Override
    public int size() {
        return resident.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public boolean containsKey(K id) {
        return resident.containsKey(id);
    }

    @Override
    public void clear() {
        resident.clear();
        ghosts.clear();
        a1in.clear();
        a1out.clear();
        am.clear();
    }

    /**
     * Returns the number of keys remembered in the ghost queue. These hold no value and do not
     * count towards the capacity.
     *
     * @return the current size of {@code A1out}
     */
    public int ghostSize() {
        return ghosts.size();
    }

    /** Frees one slot, preferring the FIFO buffer over the main queue. */
    private void makeRoom() {
        if (resident.size() < capacity) {
            return;
        }
        if (a1in.size() > inCapacity || am.isEmpty()) {
            Entry<K, V> victim = a1in.pollLast();
            if (victim != null) {
                resident.remove(victim.key);
                rememberGhost(victim.key);
                return;
            }
        }
        Entry<K, V> victim = am.pollLast();
        if (victim != null) {
            resident.remove(victim.key);
        }
    }

    private void rememberGhost(K key) {
        while (ghosts.size() >= ghostCapacity) {
            Entry<K, V> oldest = a1out.pollLast();
            if (oldest == null) {
                break;
            }
            ghosts.remove(oldest.key);
        }
        Entry<K, V> ghost = new Entry<>(key, null);
        ghosts.put(key, ghost);
        a1out.addFirst(ghost);
    }
}
