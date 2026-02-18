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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedMapSignal;
import com.vaadin.flow.signals.shared.SharedNumberSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests that signals whose values should never be null actually return non-null
 * values from get(), peek(), and peekConfirmed().
 */
public class SignalNullabilityTest extends SignalTestBase {

    @Test
    void listSignal_get_nonNull() {
        ListSignal<String> signal = new ListSignal<>();
        List<ValueSignal<String>> value = signal.get();
        assertNotNull(value);
    }

    @Test
    void listSignal_peek_nonNull() {
        ListSignal<String> signal = new ListSignal<>();
        assertNotNull(signal.peek());
    }

    @Test
    void sharedListSignal_get_nonNull() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        List<SharedValueSignal<String>> value = signal.get();
        assertNotNull(value);
    }

    @Test
    void sharedListSignal_peek_nonNull() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        assertNotNull(signal.peek());
    }

    @Test
    void sharedListSignal_peekConfirmed_nonNull() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        assertNotNull(signal.peekConfirmed());
    }

    @Test
    void sharedMapSignal_get_nonNull() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        Map<String, SharedValueSignal<String>> value = signal.get();
        assertNotNull(value);
    }

    @Test
    void sharedMapSignal_peek_nonNull() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        assertNotNull(signal.peek());
    }

    @Test
    void sharedMapSignal_peekConfirmed_nonNull() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        assertNotNull(signal.peekConfirmed());
    }

    @Test
    void sharedNumberSignal_get_nonNull() {
        SharedNumberSignal signal = new SharedNumberSignal();
        assertNotNull(signal.get());
    }

    @Test
    void sharedNumberSignal_peek_nonNull() {
        SharedNumberSignal signal = new SharedNumberSignal();
        assertNotNull(signal.peek());
    }

    @Test
    void sharedNumberSignal_peekConfirmed_nonNull() {
        SharedNumberSignal signal = new SharedNumberSignal();
        assertNotNull(signal.peekConfirmed());
    }
}
