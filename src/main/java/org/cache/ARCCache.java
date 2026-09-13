package org.cache;

import org.cache.internal.Entry;
import org.cache.internal.EntryList;
import org.cache.internal.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * ARC (Adaptive Replacement Cache) by Megiddo and Modha.
 *
 * <p>ARC runs LRU and LFU side by side and lets the workload decide how much room each one gets.
 * Four lists carry the state: {@code T1} holds entries seen once, {@code T2} holds entries seen
 * more than once, and {@code B1} and {@code B2} are ghost lists that remember the keys recently
 * evicted from {@code T1} and {@code T2} without keeping their values.
 *
 * <p>The ghosts are the feedback channel. A hit in {@code B1} means recency was starved, so the
 * target size of {@code T1} grows; a hit in {@code B2} means frequency was starved and it shrinks.
 * The tuning parameter moves on every ghost hit, so the cache follows a workload that changes
 * shape instead of needing to be configured for one.
 *
 * <p>Memory overhead is the price: up to {@code 2 * capacity} keys are tracked, although only
 * {@code capacity} values are held.
 *
 * <p>All operations run in O(1). Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class ARCCache<K, V> implements CacheService<K, V> {

    private final int capacity;
    private final Map<K, Entry<K, V>> entries;

    /** Entries seen once and still resident. */
    private final EntryList<K, V> t1 = new EntryList<>();
    /** Entries seen at least twice and still resident. */
    private final EntryList<K, V> t2 = new EntryList<>();
    /** Keys recently evicted from {@code T1}, values dropped. */
    private final EntryList<K, V> b1 = new EntryList<>();
    /** Keys recently evicted from {@code T2}, values dropped. */
    private final EntryList<K, V> b2 = new EntryList<>();

    /** Target size of {@code T1}, moved by ghost hits. */
    private int target;

    /**
     * Creates a cache holding at most {@code capacity} values.
     *
     * @param capacity the maximum number of resident entries, must be positive
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public ARCCache(int capacity) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        this.entries = new HashMap<>(capacity);
    }

    @Override
    public void put(K id, V value) {
        Entry<K, V> entry = entries.get(id);
        if (entry != null) {
            EntryList<K, V> owner = entry.owner();
            if (owner == t1 || owner == t2) {
                entry.value = value;
                owner.remove(entry);
                t2.addFirst(entry);
                return;
            }
            if (owner == b1) {
                growTarget();
                replace(false);
                b1.remove(entry);
            } else {
                shrinkTarget();
                replace(true);
                b2.remove(entry);
            }
            entry.value = value;
            t2.addFirst(entry);
            return;
        }

        admitNewKey();
        Entry<K, V> fresh = new Entry<>(id, value);
        entries.put(id, fresh);
        t1.addFirst(fresh);
    }

    @Override
    public V get(K id) {
        Entry<K, V> entry = entries.get(id);
        if (entry == null) {
            return null;
        }
        EntryList<K, V> owner = entry.owner();
        if (owner == t1) {
            // Second sighting: the entry graduates to the frequency list.
            t1.remove(entry);
            t2.addFirst(entry);
            return entry.value;
        }
        if (owner == t2) {
            t2.moveToFirst(entry);
            return entry.value;
        }
        // Ghost entry: the key is remembered but the value is gone.
        return null;
    }

    @Override
    public void evict(K id) {
        Entry<K, V> entry = entries.remove(id);
        if (entry == null) {
            return;
        }
        EntryList<K, V> owner = entry.owner();
        if (owner != null) {
            owner.remove(entry);
        }
    }

    @Override
    public int size() {
        return t1.size() + t2.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public boolean containsKey(K id) {
        Entry<K, V> entry = entries.get(id);
        return entry != null && (entry.owner() == t1 || entry.owner() == t2);
    }

    @Override
    public void clear() {
        entries.clear();
        t1.clear();
        t2.clear();
        b1.clear();
        b2.clear();
        target = 0;
    }

    /**
     * Returns the current target size of the recency list. It grows on {@code B1} hits and shrinks
     * on {@code B2} hits, and is exposed to make the adaptation observable in tests.
     *
     * @return the target size of {@code T1}, between zero and the capacity
     */
    public int recencyTarget() {
        return target;
    }

    /** Makes room for a key that is in none of the four lists. */
    private void admitNewKey() {
        int recencySide = t1.size() + b1.size();
        if (recencySide == capacity) {
            if (t1.size() < capacity) {
                forget(b1.pollLast());
                replace(false);
            } else {
                forget(t1.pollLast());
            }
            return;
        }
        int tracked = recencySide + t2.size() + b2.size();
        if (recencySide < capacity && tracked >= capacity) {
            if (tracked >= 2 * capacity) {
                forget(b2.pollLast());
            }
            replace(false);
        }
    }

    /**
     * Evicts one resident entry into the matching ghost list.
     *
     * @param keyInB2 whether the key being admitted came from the {@code B2} ghost list, which
     *                tips a tie in favour of shrinking {@code T1}
     */
    private void replace(boolean keyInB2) {
        boolean evictFromT1 = !t1.isEmpty()
                && (t1.size() > target || (keyInB2 && t1.size() == target));
        if (evictFromT1) {
            demote(t1.pollLast(), b1);
        } else if (!t2.isEmpty()) {
            demote(t2.pollLast(), b2);
        } else {
            demote(t1.pollLast(), b1);
        }
    }

    private void demote(Entry<K, V> entry, EntryList<K, V> ghostList) {
        if (entry == null) {
            return;
        }
        entry.value = null;
        ghostList.addFirst(entry);
    }

    private void forget(Entry<K, V> entry) {
        if (entry != null) {
            entries.remove(entry.key);
        }
    }

    /** A hit in B1 says recency is under-served, so T1 is allowed to grow. */
    private void growTarget() {
        int delta = b1.size() >= b2.size() ? 1 : b2.size() / Math.max(1, b1.size());
        target = Math.min(capacity, target + delta);
    }

    /** A hit in B2 says frequency is under-served, so T1 has to give room back. */
    private void shrinkTarget() {
        int delta = b2.size() >= b1.size() ? 1 : b1.size() / Math.max(1, b2.size());
        target = Math.max(0, target - delta);
    }
}
