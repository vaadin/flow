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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.TestUtil;
import com.vaadin.flow.signals.WritableSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.operations.CancelableOperation;
import com.vaadin.flow.signals.operations.SignalOperation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MappedWritableSignalTest extends SignalTestBase {

    record Todo(String text, boolean done) {
        Todo withText(String text) {
            return new Todo(text, this.done);
        }

        Todo withDone(boolean done) {
            return new Todo(this.text, done);
        }
    }

    @Test
    void map_readValue_returnsMappedValue() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Buy milk", false));
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);

        assertFalse(doneSignal.value());

        todoSignal.value(new Todo("Buy milk", true));
        assertTrue(doneSignal.value());
    }

    @Test
    void map_peek_returnsMappedValue() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Buy milk", false));
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);

        assertFalse(doneSignal.peek());
    }

    @Test
    void map_setValue_updatesParentSignal() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Buy milk", false));
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);

        doneSignal.value(true);

        assertEquals(new Todo("Buy milk", true), todoSignal.value());
    }

    @Test
    void map_setValue_returnsPreviousValue() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Buy milk", false));
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);

        SignalOperation<Boolean> operation = doneSignal.value(true);

        Boolean oldValue = TestUtil.assertSuccess(operation);
        assertFalse(oldValue);
    }

    @Test
    void map_setValuePreservesOtherFields() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Original text", false));
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);

        doneSignal.value(true);

        assertEquals("Original text", todoSignal.value().text());
    }

    @Test
    void map_replaceWithExpectedValue_updatesSignal() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Buy milk", false));
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);

        SignalOperation<Void> operation = doneSignal.replace(false, true);

        TestUtil.assertSuccess(operation);
        assertTrue(doneSignal.value());
    }

    @Test
    void map_replaceWithUnexpectedValue_fails() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Buy milk", false));
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);

        SignalOperation<Void> operation = doneSignal.replace(true, false);

        TestUtil.assertFailure(operation);
        assertFalse(doneSignal.value());
    }

    @Test
    void map_update_updatesValue() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Task", false));
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);

        CancelableOperation<Boolean> operation = doneSignal
                .update(value -> !value);

        Boolean oldValue = TestUtil.assertSuccess(operation);
        assertFalse(oldValue);
        assertTrue(doneSignal.value());
        assertEquals(new Todo("Task", true), todoSignal.value());
    }

    @Test
    void map_update_preservesOtherFields() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Task", false));
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);

        doneSignal.update(value -> !value);

        assertEquals("Task", todoSignal.value().text());
    }

    @Test
    void map_nestedMapping_works() {
        record Outer(Todo inner) {
            Outer withInner(Todo inner) {
                return new Outer(inner);
            }
        }

        ValueSignal<Outer> outerSignal = new ValueSignal<>(
                new Outer(new Todo("Task", false)));
        WritableSignal<Todo> todoSignal = outerSignal.map(Outer::inner,
                Outer::withInner);
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);

        doneSignal.value(true);

        assertTrue(outerSignal.value().inner().done());
        assertEquals("Task", outerSignal.value().inner().text());
    }

    @Test
    void map_effectTracking_tracksChanges() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Buy milk", false));
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);

        AtomicInteger effectCount = new AtomicInteger();
        Signal.effect(() -> {
            doneSignal.value();
            effectCount.incrementAndGet();
        });

        assertEquals(1, effectCount.get());

        doneSignal.value(true);
        assertEquals(2, effectCount.get());

        todoSignal.value(new Todo("Changed", true));
        assertEquals(3, effectCount.get());
    }

    @Test
    void map_asReadonly_createsReadonlyView() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Buy milk", false));
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);
        Signal<Boolean> readonlySignal = doneSignal.asReadonly();

        assertFalse(readonlySignal instanceof WritableSignal);

        doneSignal.value(true);
        assertTrue(readonlySignal.value());
    }

    @Test
    void map_multipleFieldMappings_independent() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Task", false));
        WritableSignal<String> textSignal = todoSignal.map(Todo::text,
                Todo::withText);
        WritableSignal<Boolean> doneSignal = todoSignal.map(Todo::done,
                Todo::withDone);

        textSignal.value("Updated task");
        assertEquals("Updated task", todoSignal.value().text());
        assertFalse(todoSignal.value().done());

        doneSignal.value(true);
        assertEquals("Updated task", todoSignal.value().text());
        assertTrue(todoSignal.value().done());
    }
}
