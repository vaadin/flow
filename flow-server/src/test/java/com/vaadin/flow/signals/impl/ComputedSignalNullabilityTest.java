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

import org.jspecify.annotations.NullMarked;
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
@NullMarked
public class ComputedSignalNullabilityTest extends SignalTestBase {

    @Test
    void map_nonNullToNonNull_resultIsNonNull() {
        var signal = new ValueSignal<>("hello");
        Signal<Integer> mapped = signal.map(String::length);
        // NullAway verifies mapped.get() returns non-null Integer
        int length = mapped.get();
        assertEquals(5, length);
    }

    @Test
    void map_nonNullToNullable_resultIsNullable() {
        var signal = new ValueSignal<>("hello");
        Signal<@Nullable String> mapped = signal
                .map(s -> s.isEmpty() ? null : s);
        @Nullable
        String value = mapped.get();
        // Must null-check — NullAway enforces this
        if (value != null) {
            assertEquals("hello", value);
        }
    }

    @Test
    void computed_nonNull_returnsNonNull() {
        var signal = new ValueSignal<>("hello");
        Signal<Integer> computed = Signal.computed(() -> signal.get().length());
        // NullAway verifies computed.get() returns non-null Integer
        int length = computed.get();
        assertEquals(5, length);
    }

    @Test
    void not_booleanSignal_returnsNonNull() {
        var signal = new ValueSignal<>(true);
        Signal<Boolean> negated = Signal.not(signal);
        // NullAway verifies not() returns Signal<Boolean> (non-null),
        // so unboxing is safe
        boolean value = negated.get();
        assertFalse(value);
    }
}
