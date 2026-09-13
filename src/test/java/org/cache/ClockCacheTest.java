package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockCacheTest {

    @Test
    @DisplayName("evicts an untouched entry before a referenced one")
    void referencedEntryGetsASecondChance() {
        CacheService<Integer, String> cache = new ClockCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Only key 1 carries a reference bit, so the hand passes over it.
        cache.get(1);
        cache.put(4, "four");

        assertEquals("one", cache.get(1));
        assertNull(cache.get(2));
        assertEquals("four", cache.get(4));
    }

    @Test
    @DisplayName("a second chance is used up after one pass of the hand")
    void secondChanceIsConsumed() {
        CacheService<Integer, String> cache = new ClockCache<>(2);
        cache.put(1, "one");
        cache.put(2, "two");

        cache.get(1);
        cache.get(2);

        // Both bits are set: the first eviction clears them, the second one takes a victim.
        cache.put(3, "three");
        cache.put(4, "four");

        assertEquals(2, cache.size());
        assertEquals("four", cache.get(4));
    }

    @Test
    @DisplayName("reuses the slot freed by a manual eviction")
    void reusesFreedSlot() {
        CacheService<Integer, String> cache = new ClockCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.evict(2);
        assertEquals(2, cache.size());

        cache.put(4, "four");

        assertEquals(3, cache.size());
        assertEquals("one", cache.get(1));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
    }

    @Test
    @DisplayName("keeps a hot key across a long scan")
    void hotKeySurvivesScan() {
        CacheService<Integer, String> cache = new ClockCache<>(4);
        cache.put(0, "hot");

        for (int key = 1; key <= 100; key++) {
            cache.put(key, "cold-" + key);
            cache.get(0);
        }

        assertEquals("hot", cache.get(0));
        assertTrue(cache.size() <= 4);
    }
}
