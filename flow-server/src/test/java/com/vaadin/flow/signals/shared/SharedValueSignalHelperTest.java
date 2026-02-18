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

import java.util.Objects;

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
    @SuppressWarnings("NullAway")
    void updater_nullUpdater_throwsNullPointerException() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        assertThrows(NullPointerException.class, () -> signal.updater(null));
    }

    @Test
    void updater_returnsCallback() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        @SuppressWarnings("NullAway")
        SerializableConsumer<String> callback = signal.updater((person,
                name) -> Objects.requireNonNull(person).withName(name));
        assertNotNull(callback);
    }

    @Test
    void updater_callbackUpdatesSignal() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        @SuppressWarnings("NullAway")
        SerializableConsumer<String> callback = signal.updater((person,
                name) -> Objects.requireNonNull(person).withName(name));

        callback.accept("Bob");

        assertEquals("Bob", Objects.requireNonNull(signal.get()).name());
        assertEquals(30, Objects.requireNonNull(signal.get()).age());
    }

    @Test
    void updater_multipleCallbackInvocations_updatesSignalEachTime() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        @SuppressWarnings("NullAway")
        SerializableConsumer<String> callback = signal.updater((person,
                name) -> Objects.requireNonNull(person).withName(name));

        callback.accept("Bob");
        assertEquals("Bob", Objects.requireNonNull(signal.get()).name());

        callback.accept("Charlie");
        assertEquals("Charlie", Objects.requireNonNull(signal.get()).name());

        callback.accept("Diana");
        assertEquals("Diana", Objects.requireNonNull(signal.get()).name());
    }

    @Test
    void updater_preservesOtherFields() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        @SuppressWarnings("NullAway")
        SerializableConsumer<String> nameCallback = signal.updater((person,
                name) -> Objects.requireNonNull(person).withName(name));

        nameCallback.accept("Bob");

        assertEquals("Bob", Objects.requireNonNull(signal.get()).name());
        assertEquals(30, Objects.requireNonNull(signal.get()).age());
    }

    @Test
    void updater_withNullValue_works() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        @SuppressWarnings("NullAway")
        SerializableConsumer<String> callback = signal.updater((person,
                name) -> Objects.requireNonNull(person).withName(name));

        callback.accept(null);

        assertEquals(null, Objects.requireNonNull(signal.get()).name());
        assertEquals(30, Objects.requireNonNull(signal.get()).age());
    }

    @Test
    void updater_multipleDifferentUpdaters_canBeCreated() {
        SharedValueSignal<ImmutablePerson> signal = new SharedValueSignal<>(
                new ImmutablePerson("Alice", 30));
        @SuppressWarnings("NullAway")
        SerializableConsumer<String> nameCallback = signal.updater((person,
                name) -> Objects.requireNonNull(person).withName(name));
        @SuppressWarnings("NullAway")
        SerializableConsumer<Integer> ageCallback = signal.updater(
                (person, age) -> Objects.requireNonNull(person).withAge(age));

        nameCallback.accept("Bob");
        assertEquals("Bob", Objects.requireNonNull(signal.get()).name());
        assertEquals(30, Objects.requireNonNull(signal.get()).age());

        ageCallback.accept(35);
        assertEquals("Bob", Objects.requireNonNull(signal.get()).name());
        assertEquals(35, Objects.requireNonNull(signal.get()).age());
    }
}
