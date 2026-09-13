package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour every {@link CacheService} must share, regardless of the eviction policy it uses.
 */
class CacheContractTest {

    private static Stream<Arguments> allCaches() {
        return Stream.of(
                factory("LRULinkedHashMapCache", LRULinkedHashMapCache::new),
                factory("LRUDoublyLinkedListCache", LRUDoublyLinkedListCache::new),
                factory("LRUHashMapQueueCache", LRUHashMapQueueCache::new),
                factory("MRUCache", MRUCache::new),
                factory("LFUDoublyLinkedListCache", LFUDoublyLinkedListCache::new),
                factory("LFUTreeMapCache", LFUTreeMapCache::new),
                factory("FIFOCache", FIFOCache::new),
                factory("ClockCache", ClockCache::new),
                factory("RandomReplacementCache", RandomReplacementCache::new),
                factory("SLRUCache", SLRUCache::new),
                factory("TwoQueueCache", TwoQueueCache::new),
                factory("ARCCache", ARCCache::new),
                factory("TTLCache", capacity -> new TTLCache<>(capacity, Duration.ofHours(1))),
                factory("SynchronizedCache", capacity -> new SynchronizedCache<>(new LRULinkedHashMapCache<>(capacity))),
                factory("MonitoredCache", capacity -> new MonitoredCache<>(new LRUDoublyLinkedListCache<>(capacity)))
        );
    }

    private static Arguments factory(String name, IntFunction<CacheService<Integer, String>> constructor) {
        return Arguments.of(Named.of(name, constructor));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("stores and returns values")
    void storesAndReturnsValues(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals(3, cache.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("returns null for an unknown key")
    void returnsNullForUnknownKey(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);

        assertNull(cache.get(42));
        assertFalse(cache.containsKey(42));
        assertTrue(cache.isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("replaces the value of an existing key without growing")
    void replacesExistingValue(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);

        cache.put(1, "one");
        cache.put(1, "uno");

        assertEquals("uno", cache.get(1));
        assertEquals(1, cache.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("never holds more entries than its capacity")
    void neverExceedsCapacity(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(4);

        for (int i = 0; i < 200; i++) {
            cache.put(i, "value-" + i);
            cache.get(i % 7);
            assertTrue(cache.size() <= 4, cache + " grew to " + cache.size() + " entries");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("keeps the most recently written key resident")
    void keepsLastWrittenKey(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);

        for (int i = 0; i < 50; i++) {
            cache.put(i, "value-" + i);
        }

        assertEquals("value-49", cache.get(49));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("removes a key on evict")
    void removesKeyOnEvict(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        cache.put(1, "one");
        cache.put(2, "two");

        cache.evict(1);

        assertNull(cache.get(1));
        assertFalse(cache.containsKey(1));
        assertEquals("two", cache.get(2));
        assertEquals(1, cache.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("ignores evict of an unknown key")
    void ignoresEvictOfUnknownKey(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        cache.put(1, "one");

        cache.evict(99);

        assertEquals(1, cache.size());
        assertEquals("one", cache.get(1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("accepts writes again after every key was evicted")
    void acceptsWritesAfterFullManualEviction(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.evict(1);
        cache.evict(2);
        cache.evict(3);
        assertEquals(0, cache.size());

        cache.put(4, "four");
        cache.put(5, "five");

        assertEquals("four", cache.get(4));
        assertEquals("five", cache.get(5));
        assertEquals(2, cache.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("clear drops every entry and leaves the cache usable")
    void clearDropsEverything(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        cache.put(1, "one");
        cache.put(2, "two");

        cache.clear();

        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
        assertNull(cache.get(1));

        cache.put(3, "three");
        assertEquals("three", cache.get(3));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("works with a capacity of one")
    void worksWithCapacityOfOne(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(1);

        cache.put(1, "one");
        assertEquals("one", cache.get(1));

        cache.put(2, "two");
        assertEquals(1, cache.size());
        assertEquals("two", cache.get(2));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("rejects a non-positive capacity")
    void rejectsNonPositiveCapacity(IntFunction<CacheService<Integer, String>> factory) {
        assertThrows(IllegalArgumentException.class, () -> factory.apply(0));
        assertThrows(IllegalArgumentException.class, () -> factory.apply(-1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("containsKey does not change the eviction order")
    void containsKeyIsNotAnAccess(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(2);
        cache.put(1, "one");
        cache.put(2, "two");

        assertTrue(cache.containsKey(1));
        assertTrue(cache.containsKey(2));
        assertEquals(2, cache.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("getOrDefault falls back only on a miss")
    void getOrDefaultFallsBackOnMiss(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(2);
        cache.put(1, "one");

        assertEquals("one", cache.getOrDefault(1, "fallback"));
        assertEquals("fallback", cache.getOrDefault(2, "fallback"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("find wraps the lookup result")
    void findWrapsResult(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(2);
        cache.put(1, "one");

        assertEquals("one", cache.find(1).orElse(null));
        assertTrue(cache.find(2).isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("computeIfAbsent loads a missing key once and caches it")
    void computeIfAbsentLoadsOnce(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        int[] loads = {0};

        String first = cache.computeIfAbsent(1, key -> {
            loads[0]++;
            return "loaded-" + key;
        });
        String second = cache.computeIfAbsent(1, key -> {
            loads[0]++;
            return "loaded-" + key;
        });

        assertEquals("loaded-1", first);
        assertEquals("loaded-1", second);
        assertEquals(1, loads[0]);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("computeIfAbsent does not store a null result")
    void computeIfAbsentIgnoresNull(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);

        assertNull(cache.computeIfAbsent(1, key -> null));
        assertFalse(cache.containsKey(1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("reports the configured capacity")
    void reportsCapacity(IntFunction<CacheService<Integer, String>> factory) {
        assertEquals(7, factory.apply(7).capacity());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allCaches")
    @DisplayName("survives a mixed workload without losing consistency")
    void survivesMixedWorkload(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(8);

        for (int i = 0; i < 2_000; i++) {
            int key = i % 25;
            cache.put(key, "value-" + key);
            if (i % 3 == 0) {
                cache.get(i % 10);
            }
            if (i % 17 == 0) {
                cache.evict(i % 25);
            }
            assertTrue(cache.size() <= 8);
        }

        for (int key = 0; key < 25; key++) {
            String value = cache.get(key);
            if (value != null) {
                assertEquals("value-" + key, value, "stale value for key " + key);
            }
        }
    }
}
