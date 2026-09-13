package org.cache;

import org.cache.internal.Preconditions;

import java.util.function.Function;

/**
 * A decorator that serialises every call to the wrapped cache through a single lock.
 *
 * <p>None of the cache implementations in this library are thread safe: they move list nodes and
 * update several maps per operation, and a concurrent access corrupts that state silently. This
 * wrapper makes a cache usable from several threads at the cost of no parallelism at all.
 *
 * <p>{@link #computeIfAbsent(Object, Function)} holds the lock while the mapping function runs, so
 * a value is loaded only once, but a slow loader blocks every other thread. Keep the loader short
 * or load outside the cache.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class SynchronizedCache<K, V> implements CacheService<K, V> {

    private final CacheService<K, V> delegate;
    private final Object lock = new Object();

    /**
     * Wraps the given cache.
     *
     * @param delegate the cache to guard
     * @throws NullPointerException when the delegate is null
     */
    public SynchronizedCache(CacheService<K, V> delegate) {
        this.delegate = Preconditions.requireNonNull(delegate, "delegate");
    }

    @Override
    public void put(K id, V value) {
        synchronized (lock) {
            delegate.put(id, value);
        }
    }

    @Override
    public V get(K id) {
        synchronized (lock) {
            return delegate.get(id);
        }
    }

    @Override
    public void evict(K id) {
        synchronized (lock) {
            delegate.evict(id);
        }
    }

    @Override
    public int size() {
        synchronized (lock) {
            return delegate.size();
        }
    }

    @Override
    public int capacity() {
        return delegate.capacity();
    }

    @Override
    public boolean containsKey(K id) {
        synchronized (lock) {
            return delegate.containsKey(id);
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            delegate.clear();
        }
    }

    @Override
    public V computeIfAbsent(K id, Function<? super K, ? extends V> mappingFunction) {
        synchronized (lock) {
            return delegate.computeIfAbsent(id, mappingFunction);
        }
    }

    @Override
    public String toString() {
        return "SynchronizedCache{" + delegate.getClass().getSimpleName() + "}";
    }
}
