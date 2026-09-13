package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.IntFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Eviction order checks shared by the three LRU implementations.
 */
class LRUCacheTest {

    private static Stream<Arguments> lruCaches() {
        return Stream.of(
                Arguments.of(Named.of("LRUDoublyLinkedListCache",
                        (IntFunction<CacheService<Integer, String>>) LRUDoublyLinkedListCache::new)),
                Arguments.of(Named.of("LRUHashMapQueueCache",
                        (IntFunction<CacheService<Integer, String>>) LRUHashMapQueueCache::new)),
                Arguments.of(Named.of("LRULinkedHashMapCache",
                        (IntFunction<CacheService<Integer, String>>) LRULinkedHashMapCache::new))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lruCaches")
    @DisplayName("evicts the oldest entry first")
    void evictsOldestFirst(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.put(4, "four");

        assertNull(cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lruCaches")
    @DisplayName("a read protects an entry from the next eviction")
    void readRefreshesRecency(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        assertEquals("one", cache.get(1));
        cache.put(4, "four");

        assertEquals("one", cache.get(1));
        assertNull(cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lruCaches")
    @DisplayName("overwriting a key also refreshes its recency")
    void writeRefreshesRecency(IntFunction<CacheService<Integer, String>> factory) {
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
    @MethodSource("lruCaches")
    @DisplayName("a manually evicted key frees a slot instead of triggering an eviction")
    void manualEvictionFreesSlot(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.evict(2);
        cache.put(4, "four");

        assertEquals("one", cache.get(1));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
        assertNull(cache.get(2));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lruCaches")
    @DisplayName("repeated eviction keeps the newest window of keys")
    void keepsNewestWindow(IntFunction<CacheService<Integer, String>> factory) {
        CacheService<Integer, String> cache = factory.apply(3);

        for (int i = 1; i <= 6; i++) {
            cache.put(i, "value-" + i);
        }

        assertNull(cache.get(1));
        assertNull(cache.get(2));
        assertNull(cache.get(3));
        assertEquals("value-4", cache.get(4));
        assertEquals("value-5", cache.get(5));
        assertEquals("value-6", cache.get(6));
    }
}
