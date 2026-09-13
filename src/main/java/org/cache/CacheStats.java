package org.cache;

/**
 * An immutable snapshot of the counters collected by {@link MonitoredCache}.
 *
 * @param hitCount      number of lookups that found a value
 * @param missCount     number of lookups that found nothing
 * @param putCount      number of write operations
 * @param evictionCount number of entries dropped by the eviction policy, not counting explicit
 *                      removals through {@link CacheService#evict(Object)}
 */
public record CacheStats(long hitCount, long missCount, long putCount, long evictionCount) {

    /** An empty snapshot with every counter at zero. */
    public static final CacheStats EMPTY = new CacheStats(0, 0, 0, 0);

    /**
     * Returns the total number of lookups.
     *
     * @return hits plus misses
     */
    public long requestCount() {
        return hitCount + missCount;
    }

    /**
     * Returns the share of lookups that were served from the cache.
     *
     * @return a value between 0.0 and 1.0, and 1.0 when no lookup has happened yet
     */
    public double hitRate() {
        long requests = requestCount();
        return requests == 0 ? 1.0 : (double) hitCount / requests;
    }

    /**
     * Returns the share of lookups that had to go to the source of truth.
     *
     * @return a value between 0.0 and 1.0
     */
    public double missRate() {
        return 1.0 - hitRate();
    }

    @Override
    public String toString() {
        return String.format(
                "CacheStats{hits=%d, misses=%d, hitRate=%.3f, puts=%d, evictions=%d}",
                hitCount, missCount, hitRate(), putCount, evictionCount);
    }
}
