package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwoQueueCacheTest {

    @Test
    @DisplayName("a key requested again after eviction moves into the main queue")
    void secondRequestPromotesToMainQueue() {
        TwoQueueCache<Integer, String> cache = new TwoQueueCache<>(4, 2, 4);
        for (int key = 1; key <= 5; key++) {
            cache.put(key, "value-" + key);
        }
        // Key 1 was pushed out of the FIFO buffer and is now only a ghost.
        assertNull(cache.get(1));

        cache.put(1, "value-1");

        // It is resident again, and this time in the main queue, so a scan cannot remove it.
        for (int key = 100; key < 200; key++) {
            cache.put(key, "scan-" + key);
        }
        assertEquals("value-1", cache.get(1));
    }

    @Test
    @DisplayName("keys seen once are evicted before keys in the main queue")
    void oneOffKeysLeaveFirst() {
        TwoQueueCache<Integer, String> cache = new TwoQueueCache<>(4, 2, 4);
        cache.put(1, "hot");
        cache.put(9, "filler");
        cache.put(8, "filler");
        cache.put(7, "filler");
        cache.put(6, "filler");
        cache.put(1, "hot");

        for (int key = 100; key < 150; key++) {
            cache.put(key, "scan-" + key);
        }

        assertEquals("hot", cache.get(1));
    }

    @Test
    @DisplayName("a hit in the FIFO buffer does not promote the entry")
    void hitInFifoBufferDoesNotPromote() {
        TwoQueueCache<Integer, String> cache = new TwoQueueCache<>(4, 2, 4);
        cache.put(1, "one");

        for (int i = 0; i < 10; i++) {
            assertEquals("one", cache.get(1));
        }

        for (int key = 2; key <= 40; key++) {
            cache.put(key, "value-" + key);
        }

        assertNull(cache.get(1));
    }

    @Test
    @DisplayName("the ghost queue stays within its bound and holds no values")
    void ghostQueueIsBounded() {
        TwoQueueCache<Integer, String> cache = new TwoQueueCache<>(4, 2, 3);

        for (int key = 1; key <= 100; key++) {
            cache.put(key, "value-" + key);
            assertTrue(cache.ghostSize() <= 3, "ghost queue grew to " + cache.ghostSize());
            assertTrue(cache.size() <= 4);
        }
    }

    @Test
    @DisplayName("evicting a ghost key removes it from the ghost queue")
    void evictRemovesGhost() {
        TwoQueueCache<Integer, String> cache = new TwoQueueCache<>(4, 2, 4);
        for (int key = 1; key <= 5; key++) {
            cache.put(key, "value-" + key);
        }
        int ghostsBefore = cache.ghostSize();
        assertTrue(ghostsBefore > 0);

        cache.evict(1);

        assertEquals(ghostsBefore - 1, cache.ghostSize());
    }

    @Test
    @DisplayName("the default constructor picks the ratios from the paper")
    void defaultRatios() {
        TwoQueueCache<Integer, String> cache = new TwoQueueCache<>(100);

        for (int key = 1; key <= 1_000; key++) {
            cache.put(key, "value-" + key);
        }

        assertTrue(cache.size() <= 100);
        assertTrue(cache.ghostSize() <= 50);
    }
}
