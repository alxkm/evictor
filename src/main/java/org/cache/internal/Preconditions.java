package org.cache.internal;

/**
 * Small argument check helpers shared by the cache implementations.
 *
 * <p>This is an implementation detail and not part of the public API.
 */
public final class Preconditions {

    private Preconditions() {
    }

    /**
     * Verifies that a capacity is at least one.
     *
     * @param capacity the capacity to validate
     * @return the validated capacity
     * @throws IllegalArgumentException when the capacity is not positive
     */
    public static int positiveCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive, got " + capacity);
        }
        return capacity;
    }

    /**
     * Verifies that a reference is not null.
     *
     * @param value the reference to validate
     * @param name  the argument name used in the error message
     * @param <T>   the type of the reference
     * @return the validated reference
     * @throws NullPointerException when the reference is null
     */
    public static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new NullPointerException(name + " must not be null");
        }
        return value;
    }
}
