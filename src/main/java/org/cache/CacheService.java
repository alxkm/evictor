package org.cache;

import java.util.Optional;
import java.util.function.Function;

/**
 * Common contract for every bounded cache in this library.
 *
 * <p>A cache holds at most {@link #capacity()} entries. When a new entry does not fit,
 * the implementation drops one of the resident entries according to its eviction policy
 * (least recently used, least frequently used, first in first out, and so on).
 *
 * <p>Implementations are not thread safe unless stated otherwise. Wrap an instance with
 * {@link Caches#synchronizedCache(CacheService)} when it is shared between threads.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public interface CacheService<K, V> {

    /**
     * Inserts the specified key-value pair into the cache.
     * If the cache previously contained a mapping for the key, the old value is replaced.
     * If inserting the new pair exceeds the cache capacity, one entry is evicted according
     * to the eviction policy of the implementation.
     *
     * @param id    the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    void put(K id, V value);

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this cache
     * contains no mapping for the key. A successful lookup counts as an access and updates
     * the recency or frequency bookkeeping of the implementation.
     *
     * @param id the key whose associated value is to be returned
     * @return the mapped value, or {@code null} if the key is absent
     */
    V get(K id);

    /**
     * Removes the mapping for a key from this cache if it is present.
     *
     * @param id the key whose mapping is to be removed from the cache
     */
    void evict(K id);

    /**
     * Returns the number of entries currently held by the cache.
     *
     * @return the current number of entries, never negative
     */
    int size();

    /**
     * Returns the maximum number of entries the cache can hold.
     *
     * @return the configured capacity
     */
    int capacity();

    /**
     * Returns whether the cache holds a mapping for the key. Unlike {@link #get(Object)}
     * this method does not count as an access and leaves the eviction order untouched.
     *
     * @param id the key to look up
     * @return {@code true} if the key is present
     */
    boolean containsKey(K id);

    /**
     * Removes every entry from the cache.
     */
    void clear();

    /**
     * Returns {@code true} when the cache holds no entries.
     *
     * @return {@code true} if {@link #size()} is zero
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns the mapped value, or {@code defaultValue} when the key is absent.
     *
     * @param id           the key whose associated value is to be returned
     * @param defaultValue the value to return when the key is absent
     * @return the mapped value or {@code defaultValue}
     */
    default V getOrDefault(K id, V defaultValue) {
        V value = get(id);
        return value != null ? value : defaultValue;
    }

    /**
     * Returns the mapped value wrapped in an {@link Optional}.
     *
     * @param id the key whose associated value is to be returned
     * @return an optional holding the mapped value, empty when the key is absent
     */
    default Optional<V> find(K id) {
        return Optional.ofNullable(get(id));
    }

    /**
     * Returns the value for the key, computing and caching it with {@code mappingFunction}
     * when the key is absent. A {@code null} result of the mapping function is not stored.
     *
     * @param id              the key whose associated value is to be returned
     * @param mappingFunction the function computing a value for a missing key
     * @return the cached or freshly computed value, possibly {@code null}
     */
    default V computeIfAbsent(K id, Function<? super K, ? extends V> mappingFunction) {
        V value = get(id);
        if (value != null) {
            return value;
        }
        V computed = mappingFunction.apply(id);
        if (computed != null) {
            put(id, computed);
        }
        return computed;
    }
}
