package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SLRUCacheTest {

    @Test
    @DisplayName("a new entry starts on probation")
    void newEntryStartsOnProbation() {
        SLRUCache<Integer, String> cache = new SLRUCache<>(4, 2);
        cache.put(1, "one");

        assertEquals(1, cache.probationarySize());
        assertEquals(0, cache.protectedSize());
    }

    @Test
    @DisplayName("a second access promotes an entry to the protected segment")
    void secondAccessPromotes() {
        SLRUCache<Integer, String> cache = new SLRUCache<>(4, 2);
        cache.put(1, "one");

        cache.get(1);

        assertEquals(0, cache.probationarySize());
        assertEquals(1, cache.protectedSize());
    }

    @Test
    @DisplayName("an overflowing protected segment demotes its oldest entry")
    void protectedOverflowDemotesOldest() {
        SLRUCache<Integer, String> cache = new SLRUCache<>(4, 2);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.get(1);
        cache.get(2);
        assertEquals(2, cache.protectedSize());

        cache.put(3, "three");
        cache.get(3);

        assertEquals(2, cache.protectedSize());
        assertEquals(1, cache.probationarySize());
        assertEquals("one", cache.get(1));
    }

    @Test
    @DisplayName("eviction takes the oldest probationary entry")
    void evictsOldestProbationaryEntry() {
        SLRUCache<Integer, String> cache = new SLRUCache<>(4, 2);
        cache.put(1, "one");
        cache.get(1);
        cache.put(2, "two");
        cache.put(3, "three");
        cache.put(4, "four");

        cache.put(5, "five");

        assertNull(cache.get(2));
        assertEquals("one", cache.get(1));
        assertEquals("five", cache.get(5));
    }

    @Test
    @DisplayName("a scan of one-off keys does not flush the protected segment")
    void scanDoesNotFlushProtectedSegment() {
        SLRUCache<Integer, String> cache = new SLRUCache<>(6, 3);
        for (int key = 1; key <= 3; key++) {
            cache.put(key, "hot-" + key);
            cache.get(key);
        }
        assertEquals(3, cache.protectedSize());

        for (int key = 100; key < 200; key++) {
            cache.put(key, "scan-" + key);
        }

        assertEquals("hot-1", cache.get(1));
        assertEquals("hot-2", cache.get(2));
        assertEquals("hot-3", cache.get(3));
    }

    @Test
    @DisplayName("a zero sized protected segment degrades to plain LRU")
    void zeroProtectedSegmentBehavesLikeLru() {
        SLRUCache<Integer, String> cache = new SLRUCache<>(3, 0);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.get(1);
        cache.put(4, "four");

        assertEquals("one", cache.get(1));
        assertNull(cache.get(2));
        assertEquals(0, cache.protectedSize());
    }
}
