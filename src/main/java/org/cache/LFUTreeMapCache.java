package org.cache;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A class representing a Least Frequently Used (LFU) Cache using a HashMap and a TreeMap.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class LFUTreeMapCache<K, V> implements CacheService<K, V> {

    private final int capacity;
    private int size;
    private final Map<K, CacheNode<K, V>> cache;
    private final TreeMap<Integer, Map<K, CacheNode<K, V>>> frequencyMap;

    /**
     * Constructs an LFU Cache with the specified capacity.
     *
     * @param capacity the capacity of the cache
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public LFUTreeMapCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive, got " + capacity);
        }
        this.capacity = capacity;
        this.size = 0;
        this.cache = new HashMap<>();
        this.frequencyMap = new TreeMap<>();
    }

    /**
     * Adds an item to the cache. If the cache is full, evicts the least frequently used item.
     * If an item with the same key already exists, updates its value and frequency.
     *
     * @param id    the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    @Override
    public void put(K id, V value) {
        if (cache.containsKey(id)) {
            CacheNode<K, V> node = cache.get(id);
            node.value = value;
            get(id); // Increase frequency
        } else {
            if (size == capacity) {
                // Evict the least frequently used item
                Map.Entry<Integer, Map<K, CacheNode<K, V>>> entry = frequencyMap.firstEntry();
                Map<K, CacheNode<K, V>> nodes = entry.getValue();
                CacheNode<K, V> nodeToEvict = nodes.values().iterator().next();
                nodes.remove(nodeToEvict.key);
                if (nodes.isEmpty()) {
                    frequencyMap.pollFirstEntry();
                }
                cache.remove(nodeToEvict.key);
                size--;
            }
            // Add new item
            CacheNode<K, V> newNode = new CacheNode<>(id, value);
            cache.put(id, newNode);
            frequencyMap.computeIfAbsent(1, k -> new LinkedHashMap<>()).put(id, newNode);
            size++;
        }
    }

    /**
     * Retrieves the value associated with the specified key. If the key is found,
     * increases its frequency.
     *
     * @param id the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or null if this cache contains no mapping for the key
     */
    @Override
    public V get(K id) {
        if (!cache.containsKey(id)) return null;

        CacheNode<K, V> node = cache.get(id);
        int currentFreq = node.frequency;
        Map<K, CacheNode<K, V>> nodes = frequencyMap.get(currentFreq);
        nodes.remove(id);

        if (nodes.isEmpty()) {
            frequencyMap.remove(currentFreq);
        }

        node.frequency++;
        frequencyMap.computeIfAbsent(node.frequency, k -> new LinkedHashMap<>()).put(id, node);
        return node.value;
    }

    /**
     * Evicts the item with the specified key from the cache.
     *
     * @param id the key whose mapping is to be removed from the cache
     */
    @Override
    public void evict(K id) {
        if (!cache.containsKey(id)) return;

        CacheNode<K, V> node = cache.get(id);
        int currentFreq = node.frequency;
        Map<K, CacheNode<K, V>> nodes = frequencyMap.get(currentFreq);
        nodes.remove(id);

        if (nodes.isEmpty()) {
            frequencyMap.remove(currentFreq);
        }

        cache.remove(id);
        size--;
    }

    /**
     * Node class representing a key-value pair with a frequency counter.
     *
     * @param <T> the type of key
     * @param <V> the type of value
     */
    private static class CacheNode<T, V> {
        T key;
        V value;
        int frequency;

        /**
         * Constructs a new node with the specified key and value. Initializes the frequency to 1.
         *
         * @param key   the key of the node
         * @param value the value of the node
         */
        CacheNode(T key, V value) {
            this.key = key;
            this.value = value;
            this.frequency = 1;
        }
    }
}

