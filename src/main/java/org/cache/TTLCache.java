package org.cache;

import org.cache.internal.Entry;
import org.cache.internal.EntryList;
import org.cache.internal.Preconditions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * A cache that bounds entries both by count and by age.
 *
 * <p>Every entry carries an expiration stamp set when it is written. A read that lands on an
 * expired entry treats it as a miss and removes it, and a write first drops whatever expired at
 * the head of the queue. When the cache is still full after that, the least recently used entry is
 * evicted, so the policy degrades to plain LRU once nothing has expired yet.
 *
 * <p>Expiration is lazy: nothing runs in the background, so an entry can outlive its deadline in
 * memory until the cache is touched again. {@link #purgeExpired()} forces the cleanup when that
 * matters.
 *
 * <p>All operations run in O(1) amortised. Not thread safe.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class TTLCache<K, V> implements CacheService<K, V> {

    private final int capacity;
    private final long ttlNanos;
    private final LongSupplier nanoClock;
    private final Map<K, Entry<K, V>> cache;
    private final EntryList<K, V> accessOrder = new EntryList<>();
    /** Entries in write order, which for a fixed time to live is also expiration order. */
    private final LinkedHashMap<K, Entry<K, V>> expirationOrder = new LinkedHashMap<>();

    /**
     * Creates a cache holding at most {@code capacity} entries for at most {@code timeToLive}.
     *
     * @param capacity   the maximum number of entries, must be positive
     * @param timeToLive how long an entry stays valid after it was written, must be positive
     * @throws IllegalArgumentException when the capacity or the time to live is not positive
     */
    public TTLCache(int capacity, Duration timeToLive) {
        this(capacity, timeToLive, System::nanoTime);
    }

    /**
     * Creates a cache with an explicit time source, which makes expiration testable without
     * sleeping.
     *
     * @param capacity   the maximum number of entries, must be positive
     * @param timeToLive how long an entry stays valid after it was written, must be positive
     * @param nanoClock  a monotonic nanosecond time source, normally {@code System::nanoTime}
     * @throws IllegalArgumentException when the capacity or the time to live is not positive
     */
    public TTLCache(int capacity, Duration timeToLive, LongSupplier nanoClock) {
        this.capacity = Preconditions.positiveCapacity(capacity);
        Preconditions.requireNonNull(timeToLive, "timeToLive");
        if (timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException("timeToLive must be positive, got " + timeToLive);
        }
        this.ttlNanos = timeToLive.toNanos();
        this.nanoClock = Preconditions.requireNonNull(nanoClock, "nanoClock");
        this.cache = new HashMap<>();
    }

    @Override
    public void put(K id, V value) {
        purgeExpired();
        Entry<K, V> existing = cache.get(id);
        if (existing != null) {
            existing.value = value;
            existing.expiresAtNanos = nanoClock.getAsLong() + ttlNanos;
            accessOrder.moveToFirst(existing);
            expirationOrder.remove(id);
            expirationOrder.put(id, existing);
            return;
        }
        if (cache.size() == capacity) {
            Entry<K, V> victim = accessOrder.pollLast();
            if (victim != null) {
                cache.remove(victim.key);
                expirationOrder.remove(victim.key);
            }
        }
        Entry<K, V> entry = new Entry<>(id, value);
        entry.expiresAtNanos = nanoClock.getAsLong() + ttlNanos;
        cache.put(id, entry);
        accessOrder.addFirst(entry);
        expirationOrder.put(id, entry);
    }

    @Override
    public V get(K id) {
        Entry<K, V> entry = cache.get(id);
        if (entry == null) {
            return null;
        }
        if (isExpired(entry)) {
            remove(entry);
            return null;
        }
        accessOrder.moveToFirst(entry);
        return entry.value;
    }

    @Override
    public void evict(K id) {
        Entry<K, V> entry = cache.get(id);
        if (entry != null) {
            remove(entry);
        }
    }

    @Override
    public int size() {
        purgeExpired();
        return cache.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public boolean containsKey(K id) {
        Entry<K, V> entry = cache.get(id);
        if (entry == null) {
            return false;
        }
        if (isExpired(entry)) {
            remove(entry);
            return false;
        }
        return true;
    }

    @Override
    public void clear() {
        cache.clear();
        accessOrder.clear();
        expirationOrder.clear();
    }

    /**
     * Drops every entry whose time to live has passed.
     *
     * @return the number of entries removed
     */
    public int purgeExpired() {
        int removed = 0;
        Iterator<Entry<K, V>> oldestFirst = expirationOrder.values().iterator();
        while (oldestFirst.hasNext()) {
            Entry<K, V> oldest = oldestFirst.next();
            if (!isExpired(oldest)) {
                break;
            }
            oldestFirst.remove();
            cache.remove(oldest.key);
            accessOrder.remove(oldest);
            removed++;
        }
        return removed;
    }

    private boolean isExpired(Entry<K, V> entry) {
        return nanoClock.getAsLong() - entry.expiresAtNanos >= 0;
    }

    private void remove(Entry<K, V> entry) {
        cache.remove(entry.key);
        accessOrder.remove(entry);
        expirationOrder.remove(entry.key);
    }
}
