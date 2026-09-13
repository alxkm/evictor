package org.cache;

import java.util.HashMap;
import java.util.Map;

public class MRUCache<K, V> implements CacheService<K, V> {

    private final int capacity;
    private final Map<K, Node<K, V>> cache;
    private final DoublyLinkedList<K, V> accessOrderList;

    /**
     * Constructs an MRU Cache with the specified capacity.
     *
     * @param capacity the capacity of the cache
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public MRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive, got " + capacity);
        }
        this.capacity = capacity;
        this.cache = new HashMap<>(capacity);
        this.accessOrderList = new DoublyLinkedList<>();
    }

    @Override
    public void put(K id, V value) {
        if (cache.containsKey(id)) {
            Node<K, V> node = cache.get(id);
            node.value = value;
            accessOrderList.moveToFront(node);
        } else {
            if (cache.size() == capacity) {
                Node<K, V> nodeToEvict = accessOrderList.head.next;
                accessOrderList.remove(nodeToEvict);
                cache.remove(nodeToEvict.key);
            }
            Node<K, V> newNode = new Node<>(id, value);
            accessOrderList.addFirst(newNode);
            cache.put(id, newNode);
        }
    }

    @Override
    public V get(K id) {
        if (!cache.containsKey(id)) return null;

        Node<K, V> node = cache.get(id);
        accessOrderList.moveToFront(node);
        return node.value;
    }

    @Override
    public void evict(K id) {
        if (!cache.containsKey(id)) return;

        Node<K, V> node = cache.get(id);
        accessOrderList.remove(node);
        cache.remove(id);
    }

    /**
     * Node class representing a key-value pair with pointers to the previous and next nodes.
     *
     * @param <T> the type of key
     * @param <V> the type of value
     */
    private static class Node<T, V> {
        T key;
        V value;
        Node<T, V> prev;
        Node<T, V> next;

        /**
         * Constructs a new node with the specified key and value.
         *
         * @param key   the key of the node
         * @param value the value of the node
         */
        Node(T key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * Doubly linked list class to maintain the access order of nodes.
     *
     * @param <T> the type of key
     * @param <V> the type of value
     */
    private static class DoublyLinkedList<T, V> {
        Node<T, V> head;
        Node<T, V> tail;

        /**
         * Constructs an empty doubly linked list.
         */
        DoublyLinkedList() {
            head = new Node<>(null, null);
            tail = new Node<>(null, null);
            head.next = tail;
            tail.prev = head;
        }

        /**
         * Adds a node to the front of the list.
         *
         * @param node the node to be added
         */
        void addFirst(Node<T, V> node) {
            Node<T, V> next = head.next;
            head.next = node;
            node.prev = head;
            node.next = next;
            next.prev = node;
        }

        /**
         * Removes a node from the list.
         *
         * @param node the node to be removed
         */
        void remove(Node<T, V> node) {
            Node<T, V> prev = node.prev;
            Node<T, V> next = node.next;
            prev.next = next;
            next.prev = prev;
        }

        /**
         * Moves a node to the front of the list.
         *
         * @param node the node to be moved
         */
        void moveToFront(Node<T, V> node) {
            remove(node);
            addFirst(node);
        }
    }

    public static void main(String[] args) {
        CacheService<Integer, String> cache = new MRUCache<>(3);

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        System.out.println(cache.get(1)); // Outputs: one

        cache.put(4, "four"); // Evicts key 1, which is the most recently used

        System.out.println(cache.get(1)); // Outputs: null (since key 1 has been evicted)

        cache.evict(3); // Manually evicts key 3

        System.out.println(cache.get(3)); // Outputs: null (since key 3 has been evicted)
    }
}
