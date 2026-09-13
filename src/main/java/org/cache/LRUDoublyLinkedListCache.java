package org.cache;

import java.util.HashMap;

/**
 * LRU (Least Recently Used) Cache implementation using a custom doubly linked list and {@link HashMap}.
 * This cache automatically removes the least recently used entries when the capacity is exceeded.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class LRUDoublyLinkedListCache<K, V> implements CacheService<K, V> {

    /**
     * The maximum number of elements the cache can hold.
     */
    private final int capacity;

    /**
     * The HashMap that stores the cache entries.
     */
    private final HashMap<K, Node<K, V>> cacheMap;

    /**
     * Dummy head of the doubly linked list.
     */
    private Node<K, V> head;

    /**
     * Dummy tail of the doubly linked list.
     */
    private Node<K, V> tail;

    /**
     * Constructs a new LRUCacheNodeBased with the specified capacity.
     *
     * @param capacity the maximum number of elements the cache can hold
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public LRUDoublyLinkedListCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive, got " + capacity);
        }
        this.capacity = capacity;
        this.cacheMap = new HashMap<>();
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Inserts the specified key-value pair into the cache.
     * If the cache previously contained a mapping for the key, the old value is replaced.
     * If inserting the new pair exceeds the cache's capacity, the least recently used entry is removed.
     *
     * @param id    the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    @Override
    public void put(K id, V value) {
        if (cacheMap.containsKey(id)) {
            Node<K, V> node = cacheMap.get(id);
            node.value = value;
            removeNode(node);
            addNodeToHead(node);
        } else {
            if (cacheMap.size() == capacity) {
                cacheMap.remove(tail.prev.key);
                removeNode(tail.prev);
            }
            Node<K, V> newNode = new Node<>(id, value);
            cacheMap.put(id, newNode);
            addNodeToHead(newNode);
        }
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this cache contains no mapping for the key.
     * Accessing the key marks it as recently used.
     *
     * @param id the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if this cache contains no mapping for the key
     */
    @Override
    public V get(K id) {
        if (cacheMap.containsKey(id)) {
            Node<K, V> node = cacheMap.get(id);
            removeNode(node);
            addNodeToHead(node);
            return node.value;
        }
        return null;
    }

    /**
     * Removes the mapping for a key from this cache if it is present.
     *
     * @param id the key whose mapping is to be removed from the cache
     */
    @Override
    public void evict(K id) {
        if (cacheMap.containsKey(id)) {
            Node<K, V> node = cacheMap.get(id);
            removeNode(node);
            cacheMap.remove(id);
        }
    }

    /**
     * Adds the specified node to the head of the doubly linked list.
     *
     * @param node the node to be added to the head of the list
     */
    private void addNodeToHead(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    /**
     * Removes the specified node from the doubly linked list.
     *
     * @param node the node to be removed from the list
     */
    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
}