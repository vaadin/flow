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

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.SignalTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Compile-time nullability tests for {@link SharedNumberSignal}.
 * <p>
 * Each test method exercises a nullability pattern that NullAway checks at
 * compile time. The file compiling without errors IS the test. If nullability
 * annotations regress, compilation will fail.
 * <p>
 * The tests also pass at runtime as basic smoke tests.
 */
@NullMarked
public class SharedNumberSignalNullabilityTest extends SignalTestBase {

    @Test
    void get_returnsNonNull() {
        var signal = new SharedNumberSignal(42);
        // NullAway verifies get() returns non-null Double, so unboxing
        // is safe without a null check
        double value = signal.get();
        assertEquals(42.0, value);
    }
}
