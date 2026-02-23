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
package com.vaadin.flow.signals.shared;

import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.SignalTestBase;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compile-time nullability tests for {@link SharedMapSignal}.
 * <p>
 * Each test method exercises a nullability pattern that NullAway checks at
 * compile time. The file compiling without errors IS the test. If nullability
 * annotations regress, compilation will fail.
 * <p>
 * The tests also pass at runtime as basic smoke tests.
 */
@NullMarked
public class SharedMapSignalNullabilityTest extends SignalTestBase {

    @Test
    void get_returnsNonNullMap() {
        var signal = new SharedMapSignal<>(String.class);
        // NullAway verifies get() returns non-null Map, so .size()
        // is safe without a null check
        Map<String, SharedValueSignal<String>> map = signal.peek();
        assertTrue(map.isEmpty());
    }

    @Test
    void put_nullable_acceptsNull() {
        SharedMapSignal<@Nullable String> signal = new SharedMapSignal<>(
                String.class);
        // NullAway verifies that put(key, null) is valid for a nullable
        // map signal
        signal.put("key", null);
    }
}
