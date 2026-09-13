package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.IntFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Eviction order checks shared by the two LFU implementations.
 */
class LFUCacheTest {

    private static Stream<Arguments> lfuCaches() {
        return Stream.of(
                Arguments.of(Named.of("LFUDoublyLinkedListCache",
                        (IntFunction<CacheService<Integer, String>>) LFUDoublyLinkedListCache::new)),
                Arguments.of(Named.of("LFUTreeMapCache",
                        (IntFunction<CacheService<Integer, String>>) LFUTreeMapCache::new))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lfuCaches")
    @DisplayName("evicts the entry with the lowest access count")
    void evictsLeastFrequent(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.get(1);
        cache.get(1);
        cache.get(3);

        cache.put(4, "four");

        assertNull(cache.get(2));
        assertEquals("one", cache.get(1));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lfuCaches")
    @DisplayName("breaks a frequency tie by evicting the least recently used entry")
    void breaksTieByRecency(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // All three sit at frequency one; key 1 was touched longest ago.
        cache.put(4, "four");

        assertNull(cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lfuCaches")
    @DisplayName("overwriting a value counts as an access")
    void writeCountsAsAccess(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.put(1, "uno");
        cache.put(4, "four");

        assertEquals("uno", cache.get(1));
        assertNull(cache.get(2));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lfuCaches")
    @DisplayName("a frequently used entry outlives a burst of new keys")
    void hotEntrySurvivesBurst(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        cache.put(1, "hot");
        for (int i = 0; i < 10; i++) {
            cache.get(1);
        }

        for (int key = 100; key < 120; key++) {
            cache.put(key, "cold-" + key);
        }

        assertEquals("hot", cache.get(1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lfuCaches")
    @DisplayName("evicting the last entry of the lowest frequency leaves the cache usable")
    void evictingLowestFrequencyBucketKeepsCacheUsable(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Lift two entries out of frequency one, then drain that bucket by hand.
        cache.get(1);
        cache.get(2);
        cache.evict(3);

        // The tracked minimum frequency now points at an empty bucket.
        cache.put(4, "four");
        cache.put(5, "five");

        assertEquals(3, cache.size());
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertNotNull(cache.get(5));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lfuCaches")
    @DisplayName("frequency counters are dropped together with the entry")
    void frequencyIsResetOnReinsert(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(2);
        cache.put(1, "one");
        for (int i = 0; i < 5; i++) {
            cache.get(1);
        }

        cache.evict(1);
        cache.put(1, "one again");
        cache.put(2, "two");
        cache.get(2);
        cache.get(2);
        cache.put(3, "three");

        assertNull(cache.get(1));
        assertEquals("two", cache.get(2));
    }
}
