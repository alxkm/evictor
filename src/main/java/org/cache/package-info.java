/**
 * Bounded cache implementations with a shared {@link org.cache.CacheService} contract.
 *
 * <p>Each class is a self contained implementation of one eviction policy, written to be read:
 *
 * <ul>
 *   <li>Recency: {@link org.cache.LRULinkedHashMapCache}, {@link org.cache.LRUDoublyLinkedListCache},
 *       {@link org.cache.LRUHashMapQueueCache}, {@link org.cache.MRUCache}</li>
 *   <li>Frequency: {@link org.cache.LFUDoublyLinkedListCache}, {@link org.cache.LFUTreeMapCache}</li>
 *   <li>Insertion order and approximations: {@link org.cache.FIFOCache}, {@link org.cache.ClockCache},
 *       {@link org.cache.RandomReplacementCache}</li>
 *   <li>Scan resistant and adaptive: {@link org.cache.SLRUCache}, {@link org.cache.TwoQueueCache},
 *       {@link org.cache.ARCCache}</li>
 *   <li>Time based: {@link org.cache.TTLCache}</li>
 *   <li>Decorators: {@link org.cache.SynchronizedCache}, {@link org.cache.MonitoredCache}</li>
 * </ul>
 *
 * <p>{@link org.cache.Caches} holds factory methods for all of them. No implementation is thread
 * safe on its own.
 */
package org.cache;
