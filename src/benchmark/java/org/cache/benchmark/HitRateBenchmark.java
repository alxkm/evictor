package org.cache.benchmark;

import org.cache.ARCCache;
import org.cache.CacheService;
import org.cache.ClockCache;
import org.cache.FIFOCache;
import org.cache.LFUDoublyLinkedListCache;
import org.cache.LRULinkedHashMapCache;
import org.cache.MRUCache;
import org.cache.RandomReplacementCache;
import org.cache.SLRUCache;
import org.cache.TwoQueueCache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntFunction;

/**
 * Runs every eviction policy over the same access patterns and reports the hit rate of each.
 *
 * <p>The output is a markdown table, so it can be pasted into the README as is. Run it with
 * {@code ./gradlew benchmark}.
 *
 * <p>This measures hit rate, not throughput. Which policy keeps the right entries is a property of
 * the workload and is reproducible; how fast it does so depends on the machine and belongs in a
 * JMH harness.
 */
public final class HitRateBenchmark {

    private static final int CAPACITY = 500;
    private static final int REQUESTS = 500_000;

    /** Fixed so the random replacement row is reproducible run to run. */
    private static final long RANDOM_SEED = 7;

    private HitRateBenchmark() {
    }

    /**
     * Runs the benchmark and prints the result table.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        List<AccessPattern> patterns = List.of(
                AccessPattern.zipfian(50_000, REQUESTS, 1.0, 1),
                AccessPattern.hotSetWithScan(400, 50_000, REQUESTS, 0.8, 2),
                AccessPattern.loop(600, REQUESTS),
                AccessPattern.sequentialScan(50_000, REQUESTS)
        );

        Map<String, IntFunction<CacheService<Integer, Integer>>> policies = new LinkedHashMap<>();
        policies.put("LRU", LRULinkedHashMapCache::new);
        policies.put("LFU", LFUDoublyLinkedListCache::new);
        policies.put("FIFO", FIFOCache::new);
        policies.put("Clock", ClockCache::new);
        policies.put("Random", capacity -> new RandomReplacementCache<>(capacity, new Random(RANDOM_SEED)));
        policies.put("MRU", MRUCache::new);
        policies.put("SLRU", SLRUCache::new);
        policies.put("2Q", TwoQueueCache::new);
        policies.put("ARC", ARCCache::new);

        System.out.printf("Cache capacity %d, %,d requests per pattern.%n%n", CAPACITY, REQUESTS);

        StringBuilder header = new StringBuilder("| Policy |");
        StringBuilder divider = new StringBuilder("|---|");
        for (AccessPattern pattern : patterns) {
            header.append(' ').append(pattern.name()).append(" |");
            divider.append("---|");
        }
        System.out.println(header);
        System.out.println(divider);

        for (Map.Entry<String, IntFunction<CacheService<Integer, Integer>>> policy : policies.entrySet()) {
            StringBuilder row = new StringBuilder("| ").append(policy.getKey()).append(" |");
            for (AccessPattern pattern : patterns) {
                double hitRate = measure(policy.getValue().apply(CAPACITY), pattern);
                row.append(String.format(" %.1f%% |", hitRate * 100));
            }
            System.out.println(row);
        }

        System.out.println();
        for (AccessPattern pattern : patterns) {
            System.out.printf("%s: %s%n", pattern.name(), pattern.description());
        }
    }

    /** Replays a pattern through a cache, loading on every miss, and returns the hit rate. */
    private static double measure(CacheService<Integer, Integer> cache, AccessPattern pattern) {
        long hits = 0;
        int[] keys = pattern.keys();
        for (int key : keys) {
            if (cache.get(key) != null) {
                hits++;
            } else {
                cache.put(key, key);
            }
        }
        return (double) hits / keys.length;
    }
}
