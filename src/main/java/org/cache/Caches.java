package org.cache;

import java.time.Duration;

/**
 * Factory methods for the cache implementations and decorators in this library.
 *
 * <p>Using the factory instead of a constructor keeps call sites readable and makes the choice of
 * policy easy to change:
 *
 * <pre>{@code
 * CacheService<Long, User> users = Caches.synchronizedCache(Caches.lru(10_000));
 * }</pre>
 */
public final class Caches {

    private Caches() {
    }

    /**
     * Creates the recommended LRU implementation, backed by an access ordered
     * {@link java.util.LinkedHashMap}.
     *
     * @param capacity the maximum number of entries
     * @param <K>      the type of keys
     * @param <V>      the type of values
     * @return a new LRU cache
     */
    public static <K, V> CacheService<K, V> lru(int capacity) {
        return new LRULinkedHashMapCache<>(capacity);
    }

    /**
     * Creates the constant time LFU implementation.
     *
     * @param capacity the maximum number of entries
     * @param <K>      the type of keys
     * @param <V>      the type of values
     * @return a new LFU cache
     */
    public static <K, V> CacheService<K, V> lfu(int capacity) {
        return new LFUDoublyLinkedListCache<>(capacity);
    }

    /**
     * Creates an MRU cache, which evicts the entry that was used last.
     *
     * @param capacity the maximum number of entries
     * @param <K>      the type of keys
     * @param <V>      the type of values
     * @return a new MRU cache
     */
    public static <K, V> CacheService<K, V> mru(int capacity) {
        return new MRUCache<>(capacity);
    }

    /**
     * Creates a FIFO cache, which ignores reads entirely.
     *
     * @param capacity the maximum number of entries
     * @param <K>      the type of keys
     * @param <V>      the type of values
     * @return a new FIFO cache
     */
    public static <K, V> CacheService<K, V> fifo(int capacity) {
        return new FIFOCache<>(capacity);
    }

    /**
     * Creates a clock (second chance) cache, a cheap approximation of LRU.
     *
     * @param capacity the maximum number of entries
     * @param <K>      the type of keys
     * @param <V>      the type of values
     * @return a new clock cache
     */
    public static <K, V> CacheService<K, V> clock(int capacity) {
        return new ClockCache<>(capacity);
    }

    /**
     * Creates a random replacement cache, useful as a baseline when comparing policies.
     *
     * @param capacity the maximum number of entries
     * @param <K>      the type of keys
     * @param <V>      the type of values
     * @return a new random replacement cache
     */
    public static <K, V> CacheService<K, V> random(int capacity) {
        return new RandomReplacementCache<>(capacity);
    }

    /**
     * Creates a scan resistant segmented LRU cache.
     *
     * @param capacity the maximum number of entries
     * @param <K>      the type of keys
     * @param <V>      the type of values
     * @return a new SLRU cache
     */
    public static <K, V> CacheService<K, V> slru(int capacity) {
        return new SLRUCache<>(capacity);
    }

    /**
     * Creates a 2Q cache, which admits an entry to the main queue only after a second request.
     *
     * @param capacity the maximum number of entries
     * @param <K>      the type of keys
     * @param <V>      the type of values
     * @return a new 2Q cache
     */
    public static <K, V> CacheService<K, V> twoQueue(int capacity) {
        return new TwoQueueCache<>(capacity);
    }

    /**
     * Creates an adaptive replacement cache, which tunes the balance between recency and frequency
     * as the workload changes.
     *
     * @param capacity the maximum number of entries
     * @param <K>      the type of keys
     * @param <V>      the type of values
     * @return a new ARC cache
     */
    public static <K, V> CacheService<K, V> arc(int capacity) {
        return new ARCCache<>(capacity);
    }

    /**
     * Creates a cache that bounds entries by count and by age.
     *
     * @param capacity   the maximum number of entries
     * @param timeToLive how long an entry stays valid after it was written
     * @param <K>        the type of keys
     * @param <V>        the type of values
     * @return a new TTL cache
     */
    public static <K, V> CacheService<K, V> expiring(int capacity, Duration timeToLive) {
        return new TTLCache<>(capacity, timeToLive);
    }

    /**
     * Wraps a cache so that every operation is guarded by a lock.
     *
     * @param cache the cache to guard
     * @param <K>   the type of keys
     * @param <V>   the type of values
     * @return a thread safe view of the cache
     */
    public static <K, V> CacheService<K, V> synchronizedCache(CacheService<K, V> cache) {
        return new SynchronizedCache<>(cache);
    }

    /**
     * Wraps a cache so that hits, misses, writes and evictions are counted.
     *
     * @param cache the cache to observe
     * @param <K>   the type of keys
     * @param <V>   the type of values
     * @return a counting view of the cache
     */
    public static <K, V> MonitoredCache<K, V> monitored(CacheService<K, V> cache) {
        return new MonitoredCache<>(cache);
    }
}
