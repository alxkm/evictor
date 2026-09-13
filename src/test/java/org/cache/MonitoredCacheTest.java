package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitoredCacheTest {

    @Test
    @DisplayName("counts hits and misses")
    void countsHitsAndMisses() {
        MonitoredCache<Integer, String> cache = new MonitoredCache<>(new LRULinkedHashMapCache<>(2));
        cache.put(1, "one");

        cache.get(1);
        cache.get(1);
        cache.get(2);

        CacheStats stats = cache.stats();
        assertEquals(2, stats.hitCount());
        assertEquals(1, stats.missCount());
        assertEquals(3, stats.requestCount());
        assertEquals(2.0 / 3.0, stats.hitRate(), 1e-9);
    }

    @Test
    @DisplayName("counts writes and evictions")
    void countsWritesAndEvictions() {
        MonitoredCache<Integer, String> cache = new MonitoredCache<>(new LRULinkedHashMapCache<>(2));

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.put(3, "three again");

        CacheStats stats = cache.stats();
        assertEquals(4, stats.putCount());
        assertEquals(1, stats.evictionCount());
    }

    @Test
    @DisplayName("resetStats clears the counters but keeps the entries")
    void resetStatsKeepsEntries() {
        MonitoredCache<Integer, String> cache = new MonitoredCache<>(new LRULinkedHashMapCache<>(2));
        cache.put(1, "one");
        cache.get(1);

        cache.resetStats();

        assertEquals(CacheStats.EMPTY, cache.stats());
        assertEquals("one", cache.get(1));
    }

    @Test
    @DisplayName("delegates the cache behaviour unchanged")
    void delegatesBehaviour() {
        MonitoredCache<Integer, String> cache = new MonitoredCache<>(new LRULinkedHashMapCache<>(2));

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        assertEquals(2, cache.size());
        assertEquals(2, cache.capacity());
        assertTrue(cache.containsKey(3));
        assertEquals("two", cache.get(2));

        cache.clear();
        assertTrue(cache.isEmpty());
    }

    @Test
    @DisplayName("an empty snapshot reports a full hit rate")
    void emptyStatsReportFullHitRate() {
        assertEquals(1.0, CacheStats.EMPTY.hitRate(), 1e-9);
        assertEquals(0.0, CacheStats.EMPTY.missRate(), 1e-9);
        assertEquals(0, CacheStats.EMPTY.requestCount());
    }

    @Test
    @DisplayName("toString shows the counters")
    void toStringShowsCounters() {
        MonitoredCache<Integer, String> cache = new MonitoredCache<>(new LRULinkedHashMapCache<>(2));
        cache.put(1, "one");
        cache.get(1);

        String text = cache.toString();

        assertTrue(text.contains("LRULinkedHashMapCache"), text);
        assertTrue(text.contains("hits=1"), text);
    }
}
