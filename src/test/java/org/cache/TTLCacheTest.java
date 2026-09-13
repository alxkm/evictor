package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TTLCacheTest {

    private final AtomicLong clock = new AtomicLong();

    private TTLCache<Integer, String> cacheWith(int capacity, Duration ttl) {
        return new TTLCache<>(capacity, ttl, clock::get);
    }

    private void advance(Duration duration) {
        clock.addAndGet(duration.toNanos());
    }

    @Test
    @DisplayName("serves an entry until its deadline")
    void servesEntryBeforeDeadline() {
        TTLCache<Integer, String> cache = cacheWith(3, Duration.ofSeconds(10));
        cache.put(1, "one");

        advance(Duration.ofSeconds(9));

        assertEquals("one", cache.get(1));
    }

    @Test
    @DisplayName("treats an expired entry as a miss")
    void expiredEntryIsAMiss() {
        TTLCache<Integer, String> cache = cacheWith(3, Duration.ofSeconds(10));
        cache.put(1, "one");

        advance(Duration.ofSeconds(10));

        assertNull(cache.get(1));
        assertFalse(cache.containsKey(1));
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("overwriting a value restarts the deadline")
    void writeRestartsTheDeadline() {
        TTLCache<Integer, String> cache = cacheWith(3, Duration.ofSeconds(10));
        cache.put(1, "one");

        advance(Duration.ofSeconds(9));
        cache.put(1, "uno");
        advance(Duration.ofSeconds(9));

        assertEquals("uno", cache.get(1));
    }

    @Test
    @DisplayName("reading an entry does not extend its lifetime")
    void readDoesNotExtendLifetime() {
        TTLCache<Integer, String> cache = cacheWith(3, Duration.ofSeconds(10));
        cache.put(1, "one");

        advance(Duration.ofSeconds(5));
        assertEquals("one", cache.get(1));
        advance(Duration.ofSeconds(5));

        assertNull(cache.get(1));
    }

    @Test
    @DisplayName("purgeExpired reports how many entries it dropped")
    void purgeExpiredReportsCount() {
        TTLCache<Integer, String> cache = cacheWith(5, Duration.ofSeconds(10));
        cache.put(1, "one");
        cache.put(2, "two");
        advance(Duration.ofSeconds(6));
        cache.put(3, "three");

        advance(Duration.ofSeconds(5));

        assertEquals(2, cache.purgeExpired());
        assertEquals(1, cache.size());
        assertEquals("three", cache.get(3));
    }

    @Test
    @DisplayName("falls back to LRU eviction while nothing has expired")
    void evictsLeastRecentlyUsedWhenFull() {
        TTLCache<Integer, String> cache = cacheWith(3, Duration.ofHours(1));
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.get(1);
        cache.put(4, "four");

        assertEquals("one", cache.get(1));
        assertNull(cache.get(2));
        assertEquals(3, cache.size());
    }

    @Test
    @DisplayName("an expired entry frees a slot instead of causing an eviction")
    void expiredEntryFreesASlot() {
        TTLCache<Integer, String> cache = cacheWith(2, Duration.ofSeconds(10));
        cache.put(1, "one");
        advance(Duration.ofSeconds(11));

        cache.put(2, "two");
        cache.put(3, "three");

        assertEquals(2, cache.size());
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
    }

    @Test
    @DisplayName("rejects a non-positive time to live")
    void rejectsNonPositiveTimeToLive() {
        assertThrows(IllegalArgumentException.class, () -> new TTLCache<>(3, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new TTLCache<>(3, Duration.ofSeconds(-1)));
    }
}
