/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.component.html;

import java.util.Optional;

import org.junit.Assert;

/**
 * Assert utility class.
 */
class AssertUtils {

    /**
     * Asserts that two objects are equal. If they are not, an
     * {@link AssertionError} is thrown with the given message. If
     * <code>expected</code> and <code>actual</code> are <code>null</code>, they
     * are considered equal.
     *
     * If both <code>expected</code> and <code>actual</code> are arrays or are
     * <code>Optional</code>s wrapping arrays,
     * <code>Assert#assertArrayEquals</code> is used to assert that the arrays
     * content is equal.
     *
     * @param message
     *            the identifying message for the {@link AssertionError}
     *            (<code>null</code> okay)
     * @param expected
     *            expected value
     * @param actual
     *            actual value
     */
    static public void assertEquals(String message, Object expected,
            Object actual) {

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
            Assert.assertArrayEquals(message, (Object[]) expected,
                    (Object[]) actual);

        } else {
            Assert.assertEquals(message, expected, actual);
        }
    }

}
