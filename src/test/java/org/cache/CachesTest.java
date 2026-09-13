package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CachesTest {

    @Test
    @DisplayName("every factory method returns a usable cache of the expected type")
    void factoriesReturnExpectedTypes() {
        assertInstanceOf(LRULinkedHashMapCache.class, Caches.lru(4));
        assertInstanceOf(LFUDoublyLinkedListCache.class, Caches.lfu(4));
        assertInstanceOf(MRUCache.class, Caches.mru(4));
        assertInstanceOf(FIFOCache.class, Caches.fifo(4));
        assertInstanceOf(ClockCache.class, Caches.clock(4));
        assertInstanceOf(RandomReplacementCache.class, Caches.random(4));
        assertInstanceOf(SLRUCache.class, Caches.slru(4));
        assertInstanceOf(TwoQueueCache.class, Caches.twoQueue(4));
        assertInstanceOf(ARCCache.class, Caches.arc(4));
        assertInstanceOf(TTLCache.class, Caches.expiring(4, Duration.ofMinutes(1)));
        assertInstanceOf(SynchronizedCache.class, Caches.synchronizedCache(Caches.lru(4)));
        assertInstanceOf(MonitoredCache.class, Caches.monitored(Caches.lru(4)));
    }

    @Test
    @DisplayName("decorators can be stacked")
    void decoratorsCanBeStacked() {
        MonitoredCache<Integer, String> cache =
                Caches.monitored(Caches.synchronizedCache(Caches.slru(4)));

        cache.put(1, "one");
        cache.get(1);
        cache.get(2);

        assertEquals(1, cache.stats().hitCount());
        assertEquals(1, cache.stats().missCount());
        assertEquals(4, cache.capacity());
    }
}
