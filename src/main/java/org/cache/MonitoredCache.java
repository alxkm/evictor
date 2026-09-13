package org.cache;

import org.cache.internal.Preconditions;

import java.util.concurrent.atomic.LongAdder;

/**
 * A decorator that counts hits, misses, writes and evictions of any {@link CacheService}.
 *
 * <p>Hit rate is the number that tells whether a cache earns its memory, and it is the number
 * nobody has until something measures it. Wrap a cache once at construction and read
 * {@link #stats()} from a metrics endpoint or a test.
 *
 * <p>Counters use {@link LongAdder}, so recording stays cheap under concurrency. The decorator
 * itself adds no locking: combine it with {@link Caches#synchronizedCache(CacheService)} when the
 * underlying cache is shared between threads.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class MonitoredCache<K, V> implements CacheService<K, V> {

    private final CacheService<K, V> delegate;
    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder puts = new LongAdder();
    private final LongAdder evictions = new LongAdder();

    /**
     * Wraps the given cache.
     *
     * @param delegate the cache to observe
     * @throws NullPointerException when the delegate is null
     */
    public MonitoredCache(CacheService<K, V> delegate) {
        this.delegate = Preconditions.requireNonNull(delegate, "delegate");
    }

    @Override
    public void put(K id, V value) {
        int sizeBefore = delegate.size();
        boolean replacing = delegate.containsKey(id);
        delegate.put(id, value);
        puts.increment();
        if (!replacing && delegate.size() <= sizeBefore) {
            evictions.increment();
        }
    }

    @Override
    public V get(K id) {
        V value = delegate.get(id);
        if (value != null) {
            hits.increment();
        } else {
            misses.increment();
        }
        return value;
    }

    @Override
    public void evict(K id) {
        delegate.evict(id);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public int capacity() {
        return delegate.capacity();
    }

    @Override
    public boolean containsKey(K id) {
        return delegate.containsKey(id);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    /**
     * Returns a snapshot of the counters collected so far.
     *
     * @return the current statistics
     */
    public CacheStats stats() {
        return new CacheStats(hits.sum(), misses.sum(), puts.sum(), evictions.sum());
    }

    /** Resets every counter to zero without touching the cached entries. */
    public void resetStats() {
        hits.reset();
        misses.reset();
        puts.reset();
        evictions.reset();
    }

    @Override
    public String toString() {
        return "MonitoredCache{" + delegate.getClass().getSimpleName() + ", " + stats() + "}";
    }
}
