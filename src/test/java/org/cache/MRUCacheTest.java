package org.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MRUCacheTest {

    private CacheService<Integer, String> cache;

    @BeforeEach
    void setUp() {
        cache = new MRUCache<>(3);
    }

    @Test
    @DisplayName("evicts the entry that was read last")
    void evictsMostRecentlyRead() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        assertEquals("one", cache.get(1));
        cache.put(4, "four");

        assertNull(cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
    }

    @Test
    @DisplayName("evicts the entry that was written last when there was no read")
    void evictsMostRecentlyWritten() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.put(4, "four");

        assertNull(cache.get(3));
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("four", cache.get(4));
    }

    @Test
    @DisplayName("overwriting a value makes the entry the next victim")
    void overwriteMakesEntryTheNextVictim() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.put(1, "ONE");
        cache.put(4, "four");

        assertNull(cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
    }

    @Test
    @DisplayName("cold entries survive a scan over new keys")
    void coldEntriesSurviveScan() {
        cache.put(1, "one");
        cache.put(2, "two");

        for (int key = 100; key < 110; key++) {
            cache.put(key, "scan-" + key);
        }

        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
    }
}
