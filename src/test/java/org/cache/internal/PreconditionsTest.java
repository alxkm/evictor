package org.cache.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreconditionsTest {

    @Test
    @DisplayName("a positive capacity passes through")
    void positiveCapacityPassesThrough() {
        assertEquals(5, Preconditions.positiveCapacity(5));
    }

    @Test
    @DisplayName("a non-positive capacity is reported with its value")
    void nonPositiveCapacityIsRejected() {
        IllegalArgumentException zero =
                assertThrows(IllegalArgumentException.class, () -> Preconditions.positiveCapacity(0));
        assertTrue(zero.getMessage().contains("0"), zero.getMessage());

        assertThrows(IllegalArgumentException.class, () -> Preconditions.positiveCapacity(-7));
    }

    @Test
    @DisplayName("a null reference is reported with its argument name")
    void nullReferenceIsRejected() {
        assertEquals("value", Preconditions.requireNonNull("value", "name"));

        NullPointerException error =
                assertThrows(NullPointerException.class, () -> Preconditions.requireNonNull(null, "delegate"));
        assertTrue(error.getMessage().contains("delegate"), error.getMessage());
    }
}
