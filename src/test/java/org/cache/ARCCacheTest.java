package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ARCCacheTest {

    @Test
    @DisplayName("an entry read twice lands in the frequency list and survives a scan")
    void frequentEntrySurvivesScan() {
        ARCCache<Integer, String> cache = new ARCCache<>(4);
        cache.put(0, "hot");
        cache.get(0);

        for (int key = 1; key <= 200; key++) {
            cache.put(key, "scan-" + key);
        }

        assertEquals("hot", cache.get(0));
        assertTrue(cache.size() <= 4);
    }

    @Test
    @DisplayName("the recency target grows when evicted keys come back")
    void recencyTargetGrowsOnGhostHits() {
        ARCCache<Integer, String> cache = new ARCCache<>(4);
        for (int key = 1; key <= 4; key++) {
            cache.put(key, "value-" + key);
            cache.get(key);
        }
        assertEquals(0, cache.recencyTarget());

        // Cycling over a second working set pushes its keys into the ghost list and straight back
        // in, which is exactly the signal that tells ARC recency is under-served.
        for (int round = 0; round < 3; round++) {
            for (int key = 10; key <= 13; key++) {
                cache.put(key, "value-" + key);
            }
        }

        assertTrue(cache.recencyTarget() > 0,
                "expected the recency target to grow, got " + cache.recencyTarget());
    }

    @Test
    @DisplayName("a ghost key holds no value")
    void ghostKeysAreNotResident() {
        ARCCache<Integer, String> cache = new ARCCache<>(2);
        cache.put(1, "one");
        cache.get(1);
        cache.put(2, "two");
        cache.put(3, "three");
        cache.put(4, "four");

        assertEquals(2, cache.size());
        assertFalse(cache.containsKey(2));
        assertNull(cache.get(2));
    }

    @Test
    @DisplayName("re-inserting a ghost key makes it resident again")
    void ghostKeyCanBeReadmitted() {
        ARCCache<Integer, String> cache = new ARCCache<>(3);
        cache.put(1, "one");
        cache.get(1);
        cache.put(2, "two");
        cache.put(3, "three");
        cache.put(4, "four");
        cache.put(5, "five");

        cache.put(2, "two again");

        assertEquals("two again", cache.get(2));
        assertTrue(cache.containsKey(2));
        assertTrue(cache.size() <= 3);
    }

    @Test
    @DisplayName("evicting a key drops it from the ghost lists as well")
    void evictRemovesGhostBookkeeping() {
        ARCCache<Integer, String> cache = new ARCCache<>(2);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.evict(1);
        cache.put(1, "one again");

        assertEquals("one again", cache.get(1));
        assertTrue(cache.size() <= 2);
    }

    @Test
    @DisplayName("alternating hot and cold keys keeps the hot half resident")
    void adaptsToAlternatingWorkload() {
        ARCCache<Integer, String> cache = new ARCCache<>(10);

        for (int round = 0; round < 50; round++) {
            for (int key = 0; key < 5; key++) {
                cache.put(key, "hot-" + key);
                cache.get(key);
            }
            for (int key = 1000 + round * 20; key < 1000 + round * 20 + 20; key++) {
                cache.put(key, "cold-" + key);
            }
        }

        for (int key = 0; key < 5; key++) {
            assertEquals("hot-" + key, cache.get(key), "hot key " + key + " was evicted");
        }
    }
}
