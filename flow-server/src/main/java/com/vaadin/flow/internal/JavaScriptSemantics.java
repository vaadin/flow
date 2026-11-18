/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import tools.jackson.databind.JsonNode;

/**
 * Helpers for replicating JavaScript semantics in Java.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class JavaScriptSemantics {
    private JavaScriptSemantics() {
        // Only static helpers
    }

    /**
     * Gets the boolean value of the provided value, with the same rules as in
     * JavaScript. The value can be a Java value or a JSON value:
     * <ul>
     * <li><code>null</code>, a JSON <code>null</code> and a missing JSON value
     * are <code>false</code>.
     * <li>A Java or JSON boolean is its own value.
     * <li>A Java or JSON string is <code>true</code>, except for the empty
     * string. The string <code>"false"</code> is also <code>true</code>.
     * <li>A Java or JSON number is <code>true</code>, except for 0 and
     * <code>NaN</code>.
     * <li>A JSON object or JSON array is always <code>true</code>.
     * </ul>
     *
     * @param value
     *            the value to check for truthness
     * @return <code>true</code> if the provided value is trueish according to
     *         JavaScript semantics, otherwise <code>false</code>
     */
    public static boolean isTrueish(Object value) {
        if (value == null) {
            return false;
        } else if (value instanceof Boolean bool) {
            return bool.booleanValue();
        } else if (value instanceof JsonNode node) {
            return isTrueish(node);
        } else if (value instanceof Number number) {
            return isTrueish(number.doubleValue());
        } else if (value instanceof String string) {
            return !string.isEmpty();
        } else {
            throw new IllegalStateException(
                    "Unsupported type: " + value.getClass());
        }
    }

    private static boolean isTrueish(JsonNode node) {
        // Not asBoolean(): in Jackson 3 it throws for most strings, for
        // numbers that are not integers and for arrays
        if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isNumber()) {
            return isTrueish(node.doubleValue());
        } else if (node.isString()) {
            return !node.stringValue().isEmpty();
        } else {
            // An object or an array is true, null and a missing value are
            // false
            return node.isContainer();
        }
    }

    private static boolean isTrueish(double number) {
        // False for 0, -0 and NaN
        return Math.abs(number) > 0;
    }

}
