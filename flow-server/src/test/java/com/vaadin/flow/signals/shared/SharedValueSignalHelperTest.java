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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.signals.SignalTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SharedValueSignalHelperTest extends SignalTestBase {

    record ImmutablePerson(String name, int age) {
        ImmutablePerson withName(String name) {
            return new ImmutablePerson(name, this.age);
        }

        ImmutablePerson withAge(int age) {
            return new ImmutablePerson(this.name, age);
        }
    }

    @Test
    void updater_nullUpdater_throwsNullPointerException() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        assertThrows(NullPointerException.class, () -> signal.updater(null));
    }

    @Test
    void updater_returnsCallback() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        SerializableConsumer<String> callback = signal
                .updater(ImmutablePerson::withName);
        assertNotNull(callback);
    }

    @Test
    void updater_callbackUpdatesSignal() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        SerializableConsumer<String> callback = signal
                .updater(ImmutablePerson::withName);

        callback.accept("Bob");

        assertEquals("Bob", signal.peek().name());
        assertEquals(30, signal.peek().age());
    }

    @Test
    void updater_multipleCallbackInvocations_updatesSignalEachTime() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
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
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        SerializableConsumer<String> nameCallback = signal
                .updater(ImmutablePerson::withName);

        nameCallback.accept("Bob");

        assertEquals("Bob", signal.peek().name());
        assertEquals(30, signal.peek().age());
    }

    @Test
    void updater_withNullValue_works() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        SerializableConsumer<String> callback = signal
                .updater(ImmutablePerson::withName);

        callback.accept(null);

        assertEquals(null, signal.peek().name());
        assertEquals(30, signal.peek().age());
    }

    @Test
    void updater_multipleDifferentUpdaters_canBeCreated() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        SerializableConsumer<String> nameCallback = signal
                .updater(ImmutablePerson::withName);
        SerializableConsumer<Integer> ageCallback = signal
                .updater(ImmutablePerson::withAge);

        nameCallback.accept("Bob");
        assertEquals("Bob", signal.peek().name());
        assertEquals(30, signal.peek().age());

        ageCallback.accept(35);
        assertEquals("Bob", signal.peek().name());
        assertEquals(35, signal.peek().age());
    }
}
