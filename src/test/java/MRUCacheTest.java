import static org.junit.jupiter.api.Assertions.*;

import org.cache.CacheService;
import org.cache.MRUCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MRUCacheTest {

    private CacheService<Integer, String> cache;

    @BeforeEach
    public void setUp() {
        cache = new MRUCache<>(3);
    }

    @Test
    public void testPutAndGet() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
    }

    @Test
    public void testEvictionOnCapacity() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Access the key 1 to make it the most recently used
        assertEquals("one", cache.get(1));

        // Adding a new entry should evict the most recently used entry (key 1)
        cache.put(4, "four");

        assertNull(cache.get(1)); // key 1 should be evicted
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
    }

    @Test
    public void testManualEviction() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.evict(2);

        assertNull(cache.get(2)); // key 2 should be evicted
        assertEquals("one", cache.get(1));
        assertEquals("three", cache.get(3));
    }

    @Test
    public void testUpdateValue() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Update value of key 1
        cache.put(1, "ONE");

        assertEquals("ONE", cache.get(1)); // key 1 should have the updated value
    }

    @Test
    public void testEvictAllEntries() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.evict(1);
        cache.evict(2);
        cache.evict(3);

        assertNull(cache.get(1));
        assertNull(cache.get(2));
        assertNull(cache.get(3));
    }

    @Test
    public void testEvictionOrder() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Access key 3 to make it the most recently used
        assertEquals("three", cache.get(3));

        // Adding a new entry should evict the most recently used entry (key 3)
        cache.put(4, "four");

        assertNull(cache.get(3)); // key 3 should be evicted
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("four", cache.get(4));
    }

    @Test
    public void testRejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new MRUCache<>(0));
        assertThrows(IllegalArgumentException.class, () -> new MRUCache<>(-1));
    }
}
