package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomReplacementCacheTest {

    @Test
    @DisplayName("drops exactly one entry per eviction")
    void dropsOneEntryPerEviction() {
        CacheService<Integer, String> cache = new RandomReplacementCache<>(3, new Random(42));
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.put(4, "four");

        assertEquals(3, cache.size());
        assertNotNull(cache.get(4));
    }

    @Test
    @DisplayName("a fixed seed makes eviction reproducible")
    void evictionIsReproducibleWithAFixedSeed() {
        Set<Integer> firstRun = survivorsWithSeed(7);
        Set<Integer> secondRun = survivorsWithSeed(7);

        assertEquals(firstRun, secondRun);
    }

    @Test
    @DisplayName("every surviving key still maps to its own value")
    void survivorsKeepTheirValues() {
        CacheService<Integer, String> cache = new RandomReplacementCache<>(5, new Random(1));

        for (int key = 0; key < 500; key++) {
            cache.put(key, "value-" + key);
            assertTrue(cache.size() <= 5);
        }

        for (int key = 0; key < 500; key++) {
            String value = cache.get(key);
            if (value != null) {
                assertEquals("value-" + key, value);
            }
        }
    }

    @Test
    @DisplayName("manual eviction keeps the internal key array dense")
    void manualEvictionKeepsArrayDense() {
        CacheService<Integer, String> cache = new RandomReplacementCache<>(4, new Random(3));
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.put(4, "four");

        cache.evict(1);
        cache.evict(3);
        assertEquals(2, cache.size());

        cache.put(5, "five");
        cache.put(6, "six");
        cache.put(7, "seven");

        assertEquals(4, cache.size());
        assertFalse(cache.containsKey(1));
        assertFalse(cache.containsKey(3));
    }

    private static Set<Integer> survivorsWithSeed(long seed) {
        CacheService<Integer, String> cache = new RandomReplacementCache<>(3, new Random(seed));
        for (int key = 1; key <= 10; key++) {
            cache.put(key, "value-" + key);
        }
        Set<Integer> survivors = new HashSet<>();
        for (int key = 1; key <= 10; key++) {
            if (cache.containsKey(key)) {
                survivors.add(key);
            }
        }
        return survivors;
    }
}
