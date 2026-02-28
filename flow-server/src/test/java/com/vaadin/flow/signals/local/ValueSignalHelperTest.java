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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.signals.SignalTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValueSignalHelperTest extends SignalTestBase {

    record ImmutablePerson(String name, int age) {
        ImmutablePerson withName(String name) {
            return new ImmutablePerson(name, this.age);
        }

        ImmutablePerson withAge(int age) {
            return new ImmutablePerson(this.name, age);
        }
    }

    static class MutablePerson {
        private String name;
        private int age;

        MutablePerson(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void updater_nullUpdater_throwsNullPointerException() {
        ValueSignal<ImmutablePerson> signal = new ValueSignal<>(
                new ImmutablePerson("Alice", 30));
        assertThrows(NullPointerException.class, () -> signal.updater(null));
    }

    @Test
    void updater_returnsCallback() {
        ValueSignal<ImmutablePerson> signal = new ValueSignal<>(
                new ImmutablePerson("Alice", 30));
        SerializableConsumer<String> callback = signal
                .updater(ImmutablePerson::withName);
        assertNotNull(callback);
    }

    @Test
    void updater_callbackUpdatesSignal() {
        ValueSignal<ImmutablePerson> signal = new ValueSignal<>(
                new ImmutablePerson("Alice", 30));
        SerializableConsumer<String> callback = signal
                .updater(ImmutablePerson::withName);

        callback.accept("Bob");

        assertEquals("Bob", signal.peek().name());
        assertEquals(30, signal.peek().age());
    }

    @Test
    void updater_multipleCallbackInvocations_updatesSignalEachTime() {
        ValueSignal<ImmutablePerson> signal = new ValueSignal<>(
                new ImmutablePerson("Alice", 30));
        SerializableConsumer<String> callback = signal
                .updater(ImmutablePerson::withName);

        callback.accept("Bob");
        assertEquals("Bob", signal.peek().name());

        callback.accept("Charlie");
        assertEquals("Charlie", signal.peek().name());

        callback.accept("Diana");
        assertEquals("Diana", signal.peek().name());
    }

    @Test
    void updater_preservesOtherFields() {
        ValueSignal<ImmutablePerson> signal = new ValueSignal<>(
                new ImmutablePerson("Alice", 30));
        SerializableConsumer<String> nameCallback = signal
                .updater(ImmutablePerson::withName);

        nameCallback.accept("Bob");

        assertEquals("Bob", signal.peek().name());
        assertEquals(30, signal.peek().age());
    }

    @Test
    void updater_withNullValue_works() {
        ValueSignal<ImmutablePerson> signal = new ValueSignal<>(
                new ImmutablePerson("Alice", 30));
        SerializableConsumer<String> callback = signal
                .updater(ImmutablePerson::withName);

        callback.accept(null);

        assertNull(signal.peek().name());
        assertEquals(30, signal.peek().age());
    }

    @Test
    @SuppressWarnings("NullAway")
    void modifier_nullModifier_throwsNullPointerException() {
        ValueSignal<MutablePerson> signal = new ValueSignal<>(
                new MutablePerson("Alice", 30));
        assertThrows(NullPointerException.class, () -> signal.modifier(null));
    }

    @Test
    void modifier_returnsCallback() {
        ValueSignal<MutablePerson> signal = new ValueSignal<>(
                new MutablePerson("Alice", 30));
        SerializableConsumer<String> callback = signal
                .modifier(MutablePerson::setName);
        assertNotNull(callback);
    }

    @Test
    void modifier_callbackModifiesSignalInPlace() throws Exception {
        MutablePerson person = new MutablePerson("Alice", 30);
        ValueSignal<MutablePerson> signal = new ValueSignal<>(person);
        SerializableConsumer<String> callback = signal
                .modifier(MutablePerson::setName);

        new MockVaadinSession().runWithLock(() -> {
            callback.accept("Bob");

            assertEquals("Bob", signal.peek().getName());
            assertEquals(30, signal.peek().getAge());
            assertEquals(person, signal.peek());
            return null;
        });
    }

    @Test
    void modifier_multipleCallbackInvocations_modifiesSignalEachTime()
            throws Exception {
        MutablePerson person = new MutablePerson("Alice", 30);
        ValueSignal<MutablePerson> signal = new ValueSignal<>(person);
        SerializableConsumer<String> callback = signal
                .modifier(MutablePerson::setName);

        new MockVaadinSession().runWithLock(() -> {
            callback.accept("Bob");
            assertEquals("Bob", signal.peek().getName());

            callback.accept("Charlie");
            assertEquals("Charlie", signal.peek().getName());

            callback.accept("Diana");
            assertEquals("Diana", signal.peek().getName());
            return null;
        });
    }

    @Test
    void modifier_preservesOtherFields() throws Exception {
        MutablePerson person = new MutablePerson("Alice", 30);
        ValueSignal<MutablePerson> signal = new ValueSignal<>(person);
        SerializableConsumer<String> nameCallback = signal
                .modifier(MutablePerson::setName);

        new MockVaadinSession().runWithLock(() -> {
            nameCallback.accept("Bob");

            assertEquals("Bob", signal.peek().getName());
            assertEquals(30, signal.peek().getAge());
            return null;
        });
    }

    @Test
    void modifier_withNullValue_works() throws Exception {
        MutablePerson person = new MutablePerson("Alice", 30);
        ValueSignal<MutablePerson> signal = new ValueSignal<>(person);
        SerializableConsumer<String> callback = signal
                .modifier(MutablePerson::setName);

        new MockVaadinSession().runWithLock(() -> {
            callback.accept(null);

            assertNull(signal.peek().getName());
            assertEquals(30, signal.peek().getAge());
            return null;
        });
    }

}
