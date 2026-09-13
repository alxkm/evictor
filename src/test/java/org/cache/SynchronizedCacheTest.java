package org.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynchronizedCacheTest {

    @Test
    @DisplayName("stays consistent under concurrent writes")
    void staysConsistentUnderConcurrentWrites() throws InterruptedException {
        CacheService<Integer, String> cache =
                Caches.synchronizedCache(new LRUDoublyLinkedListCache<>(64));
        int threads = 8;
        int operationsPerThread = 5_000;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            int offset = t;
            pool.execute(() -> {
                try {
                    start.await();
                    for (int i = 0; i < operationsPerThread; i++) {
                        int key = (offset * operationsPerThread + i) % 500;
                        cache.put(key, "value-" + key);
                        cache.get(key);
                        if (i % 50 == 0) {
                            cache.evict(key);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "workers did not finish in time");
        pool.shutdownNow();

        assertTrue(cache.size() <= 64, "cache grew to " + cache.size() + " entries");
        for (int key = 0; key < 500; key++) {
            String value = cache.get(key);
            if (value != null) {
                assertEquals("value-" + key, value);
            }
        }
    }

    @Test
    @DisplayName("computeIfAbsent loads a key once across threads")
    void computeIfAbsentLoadsOnceAcrossThreads() throws InterruptedException {
        CacheService<Integer, String> cache =
                Caches.synchronizedCache(new LRULinkedHashMapCache<>(16));
        AtomicInteger loads = new AtomicInteger();
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            pool.execute(() -> {
                try {
                    start.await();
                    cache.computeIfAbsent(1, key -> {
                        loads.incrementAndGet();
                        return "loaded";
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "workers did not finish in time");
        pool.shutdownNow();

        assertEquals(1, loads.get());
        assertEquals("loaded", cache.get(1));
    }

    @Test
    @DisplayName("rejects a null delegate")
    void rejectsNullDelegate() {
        assertThrows(NullPointerException.class, () -> new SynchronizedCache<Integer, String>(null));
        assertThrows(NullPointerException.class, () -> new MonitoredCache<Integer, String>(null));
    }

    @Test
    @DisplayName("toString names the wrapped implementation")
    void toStringNamesDelegate() {
        CacheService<Integer, String> cache =
                Caches.synchronizedCache(new LRULinkedHashMapCache<>(4));

        assertTrue(cache.toString().contains("LRULinkedHashMapCache"), cache.toString());
    }
}
