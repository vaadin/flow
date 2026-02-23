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
package com.vaadin.flow.signals.local;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.SignalTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Compile-time nullability tests for {@link ValueSignal}.
 * <p>
 * Each test method exercises a nullability pattern that NullAway checks at
 * compile time. The file compiling without errors IS the test. If nullability
 * annotations regress, compilation will fail.
 * <p>
 * The tests also pass at runtime as basic smoke tests.
 */
@NullMarked
public class ValueSignalNullabilityTest extends SignalTestBase {

    @Test
    void get_nonNull_returnsNonNull() {
        var signal = new ValueSignal<>("hello");
        // NullAway verifies get() returns non-null String, so .length()
        // is safe without a null check
        int length = signal.get().length();
        assertEquals(5, length);
    }

    @Test
    void get_nullable_returnsNullable() {
        var signal = new ValueSignal<@Nullable String>("hello");
        @Nullable
        String value = signal.get();
        // Must null-check before calling .length() — NullAway enforces this
        if (value != null) {
            assertEquals(5, value.length());
        }
    }

    @Test
    void set_nonNull_requiresNonNull() {
        var signal = new ValueSignal<>("hello");
        // NullAway verifies that set() accepts a non-null String
        signal.set("world");
        assertEquals("world", signal.get());
    }

    @Test
    void set_nullable_acceptsNull() {
        var signal = new ValueSignal<@Nullable String>("hello");
        // NullAway verifies that set(null) is valid for a nullable signal
        signal.set(null);
        assertNull(signal.get());
    }

    @Test
    void replace_nullable_acceptsNull() {
        var signal = new ValueSignal<@Nullable String>("hello");
        // NullAway verifies that replace(null, ...) is valid for a nullable
        // signal
        signal.replace(null, "world");
        signal.replace("world", null);
    }
}
