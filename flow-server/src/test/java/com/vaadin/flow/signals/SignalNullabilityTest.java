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
package com.vaadin.flow.signals;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedMapSignal;
import com.vaadin.flow.signals.shared.SharedNumberSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests null safety contracts for signals that guarantee non-null return
 * values.
 */
public class SignalNullabilityTest extends SignalTestBase {

    @Test
    void sharedNumberSignal_get_returnsZeroAfterSetNull() {
        SharedNumberSignal signal = new SharedNumberSignal();
        signal.set(null);
        assertEquals(0.0, signal.get());
    }

    @Test
    void sharedNumberSignal_peek_returnsZeroAfterSetNull() {
        SharedNumberSignal signal = new SharedNumberSignal();
        signal.set(null);
        assertEquals(0.0, signal.peek());
    }

    @Test
    void sharedNumberSignal_peekConfirmed_returnsZeroAfterSetNull() {
        SharedNumberSignal signal = new SharedNumberSignal();
        signal.set(null);
        assertEquals(0.0, signal.peekConfirmed());
    }

    // The following tests verify the compile-time non-null return type
    // contract for collection-based signals. Calling methods like .size()
    // directly on the return value would produce null-safety warnings if
    // the return type were changed to @Nullable.

    @Test
    void listSignal_get_nonNull() {
        ListSignal<String> signal = new ListSignal<>();
        signal.get().size();
    }

    @Test
    void sharedListSignal_get_nonNull() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        signal.get().size();
    }

    @Test
    void sharedMapSignal_get_nonNull() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.get().size();
    }
}
