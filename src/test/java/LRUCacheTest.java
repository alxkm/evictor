import org.cache.CacheService;
import org.cache.LRUDoublyLinkedListCache;
import org.cache.LRULinkedHashMapCache;
import org.cache.LRUHashMapQueueCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LRUCacheTest {

    public static Stream<CacheService<Integer, String>> cacheServices() {
        return Stream.of(
                new LRUDoublyLinkedListCache<>(3),
                new LRUHashMapQueueCache<>(3),
                new LRULinkedHashMapCache<>(3)
        );
    }

    @ParameterizedTest
    @MethodSource("cacheServices")
    public void testPutAndGet(CacheService<Integer, String> cache) {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
    }

    @ParameterizedTest
    @MethodSource("cacheServices")
    public void testEvictionPolicy(CacheService<Integer, String> cache) {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.put(4, "four");  // This should evict "one"

        assertNull(cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));

        cache.put(5, "five");  // This should evict "two"

        assertNull(cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
        assertEquals("five", cache.get(5));
    }

    @ParameterizedTest
    @MethodSource("cacheServices")
    public void testGetUpdatesOrder(CacheService<Integer, String> cache) {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Access "one" so it becomes most recently used
        assertEquals("one", cache.get(1));
        cache.put(4, "four");  // This should evict "two"

        assertEquals("one", cache.get(1));
        assertNull(cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
    }

    @ParameterizedTest
    @MethodSource("cacheServices")
    public void testEvict(CacheService<Integer, String> cache) {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.evict(2);
        assertNull(cache.get(2));
        assertEquals("one", cache.get(1));
        assertEquals("three", cache.get(3));
    }

    @Test
    public void testRejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new LRUDoublyLinkedListCache<>(0));
        assertThrows(IllegalArgumentException.class, () -> new LRUHashMapQueueCache<>(0));
        assertThrows(IllegalArgumentException.class, () -> new LRULinkedHashMapCache<>(0));
        assertThrows(IllegalArgumentException.class, () -> new LRUDoublyLinkedListCache<>(-1));
    }
}
