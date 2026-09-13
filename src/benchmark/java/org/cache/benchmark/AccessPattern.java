package org.cache.benchmark;

import java.util.Random;

/**
 * A recorded sequence of key requests, used to compare eviction policies on the same traffic.
 *
 * <p>The four patterns here are the ones that separate the policies. Every other workload is some
 * mixture of them.
 */
public final class AccessPattern {

    private final String name;
    private final String description;
    private final int[] keys;

    private AccessPattern(String name, String description, int[] keys) {
        this.name = name;
        this.description = description;
        this.keys = keys;
    }

    /**
     * A few keys take most of the traffic and a long tail takes the rest, which is what real read
     * traffic looks like. Frequency aware policies should win here.
     *
     * @param universe the number of distinct keys
     * @param length   the number of requests to generate
     * @param skew     the Zipf exponent, higher means more concentrated
     * @param seed     the random seed
     * @return the generated pattern
     */
    public static AccessPattern zipfian(int universe, int length, double skew, long seed) {
        double[] cumulative = new double[universe];
        double total = 0;
        for (int rank = 1; rank <= universe; rank++) {
            total += 1.0 / Math.pow(rank, skew);
            cumulative[rank - 1] = total;
        }

        Random random = new Random(seed);
        int[] keys = new int[length];
        for (int i = 0; i < length; i++) {
            double target = random.nextDouble() * total;
            int position = binarySearch(cumulative, target);
            keys[i] = position;
        }
        return new AccessPattern("zipfian", "skewed popularity, a hot head and a long tail", keys);
    }

    /**
     * Every key is read once, in order, over and over. Nothing read now will be read again soon,
     * so recency carries no information at all.
     *
     * @param universe the number of distinct keys
     * @param length   the number of requests to generate
     * @return the generated pattern
     */
    public static AccessPattern sequentialScan(int universe, int length) {
        int[] keys = new int[length];
        for (int i = 0; i < length; i++) {
            keys[i] = i % universe;
        }
        return new AccessPattern("scan", "read every key once, in order, repeatedly", keys);
    }

    /**
     * A loop over a working set slightly larger than the cache. This is the case that takes LRU to
     * exactly zero hits: it always evicts the entry needed next.
     *
     * @param workingSet the number of distinct keys in the loop
     * @param length     the number of requests to generate
     * @return the generated pattern
     */
    public static AccessPattern loop(int workingSet, int length) {
        int[] keys = new int[length];
        for (int i = 0; i < length; i++) {
            keys[i] = i % workingSet;
        }
        return new AccessPattern("loop", "cycle over a working set just larger than the cache", keys);
    }

    /**
     * A small hot set carries most of the traffic while a batch job scans through cold keys that
     * are never read again. Scan resistant policies should keep the hot set, plain LRU should not.
     *
     * @param hotSize      the number of keys in the hot set
     * @param scanUniverse the number of distinct cold keys
     * @param length       the number of requests to generate
     * @param hotShare     the share of requests that go to the hot set
     * @param seed         the random seed
     * @return the generated pattern
     */
    public static AccessPattern hotSetWithScan(int hotSize, int scanUniverse, int length,
                                               double hotShare, long seed) {
        Random random = new Random(seed);
        int[] keys = new int[length];
        int coldKey = hotSize;
        for (int i = 0; i < length; i++) {
            if (random.nextDouble() < hotShare) {
                keys[i] = random.nextInt(hotSize);
            } else {
                keys[i] = coldKey;
                coldKey = hotSize + (coldKey - hotSize + 1) % scanUniverse;
            }
        }
        return new AccessPattern("hot set + scan", "a hot working set crossed by a cold batch scan", keys);
    }

    /**
     * Returns the short name of the pattern.
     *
     * @return the pattern name
     */
    public String name() {
        return name;
    }

    /**
     * Returns a one line description of what the pattern models.
     *
     * @return the pattern description
     */
    public String description() {
        return description;
    }

    /**
     * Returns the recorded key sequence.
     *
     * @return the keys, in request order
     */
    public int[] keys() {
        return keys;
    }

    private static int binarySearch(double[] cumulative, double target) {
        int low = 0;
        int high = cumulative.length - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (cumulative[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }
}
