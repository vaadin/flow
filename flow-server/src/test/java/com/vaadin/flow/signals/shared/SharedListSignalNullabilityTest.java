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

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.SignalTestBase;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compile-time nullability tests for {@link SharedListSignal}.
 * <p>
 * Each test method exercises a nullability pattern that NullAway checks at
 * compile time. The file compiling without errors IS the test. If nullability
 * annotations regress, compilation will fail.
 * <p>
 * The tests also pass at runtime as basic smoke tests.
 */
@NullMarked
public class SharedListSignalNullabilityTest extends SignalTestBase {

    @Test
    void get_returnsNonNullList() {
        var signal = new SharedListSignal<>(String.class);
        // NullAway verifies get() returns non-null List, so .size()
        // is safe without a null check
        List<SharedValueSignal<String>> list = signal.get();
        assertTrue(list.isEmpty());
    }

    @Test
    void insertLast_nullable_acceptsNull() {
        var signal = new SharedListSignal<@Nullable String>(String.class);
        // NullAway verifies that insertLast(null) is valid for a nullable
        // list signal
        signal.insertLast(null);
    }
}
