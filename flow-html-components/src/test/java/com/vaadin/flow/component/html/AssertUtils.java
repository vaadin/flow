package com.vaadin.flow.component.html;

import org.junit.Assert;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

import java.util.Optional;

/**
 * Assert utility class.
 */
class AssertUtils {

    /**
     * Asserts that two objects are equal. If they are not, an
     * {@link AssertionError} is thrown with the given message. If
     * <code>expected</code> and <code>actual</code> are <code>null</code>,
     * they are considered equal.
     *
     * If both <code>expected</code> and <code>actual</code> are arrays or are
     * <code>Optional</code>s wrapping arrays, <code>Assert#assertArrayEquals</code> is used
     * to assert that the arrays content is equal.
     *
     * @param message the identifying message for the {@link AssertionError} (<code>null</code>
     * okay)
     * @param expected expected value
     * @param actual actual value
     */
    static public void assertEquals(String message, Object expected, Object actual) {

        if (expected instanceof Optional && actual instanceof Optional) {
            expected = ((Optional) expected).orElseGet(() -> null);
            actual = ((Optional) actual).orElseGet(() -> null);
        }

        if (expected == actual) {
            // Null check.
            return;

        } else if (expected == null || actual == null) {
            Assert.assertEquals(message, expected, actual);
        }

        if (expected.getClass().isArray() && actual.getClass().isArray()) {
            Assert.assertArrayEquals(message, (Object[]) expected, (Object[]) actual);

        } else {
            Assert.assertEquals(message, expected, actual);
        }
    }

}
