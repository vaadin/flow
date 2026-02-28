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
package com.vaadin.flow.signals.impl;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Compile-time nullability tests for {@link Signal} interface methods
 * ({@code map}, {@code computed}, {@code not}).
 * <p>
 * Each test method exercises a nullability pattern that NullAway checks at
 * compile time. The file compiling without errors IS the test. If nullability
 * annotations regress, compilation will fail.
 * <p>
 * The tests also pass at runtime as basic smoke tests.
 */
public class ComputedSignalNullabilityTest extends SignalTestBase {

    @Test
    void map_returnsNullable() {
        var signal = new ValueSignal<>("hello");
        Signal<Integer> mapped = signal.map(String::length);
        @Nullable
        Integer length = mapped.peek();
        // get() returns @Nullable T — NullAway enforces a null-check
        if (length != null) {
            assertEquals(5, (int) length);
        }
    }

    @Test
    void map_nonNullToNullable_resultIsNullable() {
        var signal = new ValueSignal<>("hello");
        Signal<@Nullable String> mapped = signal
                .map(s -> s.isEmpty() ? null : s);
        @Nullable
        String value = mapped.peek();
        // Must null-check — NullAway enforces this
        if (value != null) {
            assertEquals("hello", value);
        }
    }

    @Test
    void computed_returnsNullable() {
        var signal = new ValueSignal<>("hello");
        Signal<Integer> computed = Signal.computed(() -> {
            @Nullable
            String value = signal.get();
            return value != null ? value.length() : 0;
        });
        @Nullable
        Integer length = computed.peek();
        // get() returns @Nullable T — NullAway enforces a null-check
        if (length != null) {
            assertEquals(5, (int) length);
        }
    }

    @Test
    void not_returnsNullable() {
        var signal = new ValueSignal<>(true);
        Signal<Boolean> negated = Signal.not(signal);
        @Nullable
        Boolean value = negated.peek();
        // get() returns @Nullable T — NullAway enforces a null-check
        if (value != null) {
            assertFalse(value);
        }
    }
}
