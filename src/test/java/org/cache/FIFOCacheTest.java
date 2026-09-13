package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FIFOCacheTest {

    @Test
    @DisplayName("evicts in insertion order")
    void evictsInInsertionOrder() {
        CacheService<Integer, String> cache = new FIFOCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.put(4, "four");

        assertNull(cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
    }

    @Test
    @DisplayName("reading an entry does not save it from eviction")
    void readDoesNotChangeOrder() {
        CacheService<Integer, String> cache = new FIFOCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        for (int i = 0; i < 10; i++) {
            cache.get(1);
        }
        cache.put(4, "four");

        assertNull(cache.get(1));
    }

    @Test
    @DisplayName("overwriting a value keeps the original insertion position")
    void overwriteKeepsPosition() {
        CacheService<Integer, String> cache = new FIFOCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.put(1, "uno");
        cache.put(4, "four");

        assertNull(cache.get(1));
        assertEquals("two", cache.get(2));
    }

    @Test
    @DisplayName("a manually evicted key frees a slot")
    void manualEvictionFreesSlot() {
        CacheService<Integer, String> cache = new FIFOCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.evict(1);
        cache.put(4, "four");

        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
        assertEquals(3, cache.size());
    }
}
