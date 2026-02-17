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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.SignalTestBase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalSignalSessionCheckTest extends SignalTestBase {

    @AfterEach
    void clearSession() {
        VaadinSession.setCurrent(null);
    }

    // ValueSignal - same session

    @Test
    void valueSignal_sameSession_get() {
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinSession.setCurrent(session);

        ValueSignal<String> signal = new ValueSignal<>("hello");
        signal.get();

        assertDoesNotThrow(() -> signal.get());
    }

    @Test
    void valueSignal_sameSession_allOperations() {
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinSession.setCurrent(session);

        ValueSignal<String> signal = new ValueSignal<>("hello");

        assertDoesNotThrow(() -> signal.get());
        assertDoesNotThrow(() -> signal.peek());
        assertDoesNotThrow(() -> signal.set("world"));
        assertDoesNotThrow(() -> signal.replace("world", "foo"));
        assertDoesNotThrow(() -> signal.update(v -> v));
        assertDoesNotThrow(() -> signal.modify(v -> {
        }));
    }

    // ValueSignal - different session

    @Test
    void valueSignal_differentSession_throws() {
        VaadinSession session1 = Mockito.mock(VaadinSession.class);
        VaadinSession session2 = Mockito.mock(VaadinSession.class);

        VaadinSession.setCurrent(session1);
        ValueSignal<String> signal = new ValueSignal<>("hello");
        signal.get();

        VaadinSession.setCurrent(session2);

        // All operations should throw when accessed from different session
        assertThrows(IllegalStateException.class, () -> signal.get());
        assertThrows(IllegalStateException.class, () -> signal.peek());
        assertThrows(IllegalStateException.class, () -> signal.set("world"));
        assertThrows(IllegalStateException.class,
                () -> signal.replace("hello", "world"));
        assertThrows(IllegalStateException.class, () -> signal.update(v -> v));
        assertThrows(IllegalStateException.class, () -> signal.modify(v -> {
        }));
    }

    // ValueSignal - no session

    @Test
    void valueSignal_noSession_works() {
        ValueSignal<String> signal = new ValueSignal<>("hello");

        assertDoesNotThrow(() -> signal.get());
        assertDoesNotThrow(() -> signal.peek());
        assertDoesNotThrow(() -> signal.set("world"));
        assertDoesNotThrow(() -> signal.replace("world", "foo"));
        assertDoesNotThrow(() -> signal.update(v -> v));
        assertDoesNotThrow(() -> signal.modify(v -> {
        }));
    }

    // ValueSignal - error message

    @Test
    void valueSignal_differentSession_errorMessageContainsGuidance() {
        VaadinSession session1 = Mockito.mock(VaadinSession.class);
        VaadinSession session2 = Mockito.mock(VaadinSession.class);

        VaadinSession.setCurrent(session1);
        ValueSignal<String> signal = new ValueSignal<>("hello");
        signal.get();

        VaadinSession.setCurrent(session2);
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> signal.get());

        String message = exception.getMessage();
        assertTrue(message.contains("ValueSignal"));
        assertTrue(message.contains("SharedValueSignal"));
        assertTrue(message.contains("static field"));
    }

    // ListSignal - same session

    @Test
    void listSignal_sameSession_allOperations() {
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinSession.setCurrent(session);

        ListSignal<String> signal = new ListSignal<>();

        assertDoesNotThrow(() -> signal.get());
        assertDoesNotThrow(() -> signal.peek());

        ValueSignal<String> entry = signal.insertFirst("first");
        assertDoesNotThrow(() -> signal.insertLast("last"));
        assertDoesNotThrow(() -> signal.insertAt(0, "at"));
        assertDoesNotThrow(() -> signal.remove(entry));
        assertDoesNotThrow(() -> signal.clear());
    }

    // ListSignal - different session

    @Test
    void listSignal_differentSession_throws() {
        VaadinSession session1 = Mockito.mock(VaadinSession.class);
        VaadinSession session2 = Mockito.mock(VaadinSession.class);

        VaadinSession.setCurrent(session1);
        ListSignal<String> signal = new ListSignal<>();
        ValueSignal<String> entry = signal.insertFirst("value");

        VaadinSession.setCurrent(session2);

        // All operations should throw when accessed from different session
        assertThrows(IllegalStateException.class, () -> signal.get());
        assertThrows(IllegalStateException.class,
                () -> signal.insertFirst("value"));
        assertThrows(IllegalStateException.class,
                () -> signal.insertLast("value"));
        assertThrows(IllegalStateException.class,
                () -> signal.insertAt(0, "value"));
        assertThrows(IllegalStateException.class, () -> signal.remove(entry));
        assertThrows(IllegalStateException.class, () -> signal.clear());
    }

    // ListSignal - error message

    @Test
    void listSignal_differentSession_errorMessageContainsGuidance() {
        VaadinSession session1 = Mockito.mock(VaadinSession.class);
        VaadinSession session2 = Mockito.mock(VaadinSession.class);

        VaadinSession.setCurrent(session1);
        ListSignal<String> signal = new ListSignal<>();
        signal.get();

        VaadinSession.setCurrent(session2);
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> signal.get());

        String message = exception.getMessage();
        assertTrue(message.contains("ListSignal"));
        assertTrue(message.contains("static field"));
    }

    // ListSignal - no session

    @Test
    void listSignal_noSession_works() {
        ListSignal<String> signal = new ListSignal<>();

        assertDoesNotThrow(() -> signal.get());
        assertDoesNotThrow(() -> signal.peek());

        ValueSignal<String> entry = signal.insertFirst("first");
        assertDoesNotThrow(() -> signal.insertLast("last"));
        assertDoesNotThrow(() -> signal.insertAt(0, "at"));
        assertDoesNotThrow(() -> signal.remove(entry));
        assertDoesNotThrow(() -> signal.clear());
    }
}
