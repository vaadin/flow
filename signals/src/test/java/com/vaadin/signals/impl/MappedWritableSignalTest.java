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
package com.vaadin.signals.impl;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalTestBase;
import com.vaadin.signals.TestUtil;
import com.vaadin.signals.WritableSignal;
import com.vaadin.signals.local.ValueSignal;
import com.vaadin.signals.operations.CancelableOperation;
import com.vaadin.signals.operations.SignalOperation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    record Pair<A, B>(A first, B second) {
        Pair<A, B> withFirst(A first) {
            return new Pair<>(first, this.second);
        }

        Pair<A, B> withSecond(B second) {
            return new Pair<>(this.first, second);
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
        ValueSignal<Pair<Integer, String>> pairSignal = new ValueSignal<>(
                new Pair<>(5, "hello"));
        WritableSignal<Integer> firstSignal = pairSignal.map(Pair::first,
                Pair::withFirst);

        CancelableOperation<Integer> operation = firstSignal
                .update(value -> value * 2);

        Integer oldValue = TestUtil.assertSuccess(operation);
        assertEquals(5, oldValue);
        assertEquals(10, firstSignal.value());
        assertEquals(new Pair<>(10, "hello"), pairSignal.value());
    }

    @Test
    void map_update_preservesOtherFields() {
        ValueSignal<Pair<Integer, String>> pairSignal = new ValueSignal<>(
                new Pair<>(5, "hello"));
        WritableSignal<Integer> firstSignal = pairSignal.map(Pair::first,
                Pair::withFirst);

        firstSignal.update(value -> value + 10);

        assertEquals("hello", pairSignal.value().second());
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
    void map_withNullParentValue_handlesGracefully() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(null);
        WritableSignal<Boolean> doneSignal = todoSignal.map(
                todo -> todo == null ? null : todo.done(),
                (todo, done) -> done == null ? null
                        : new Todo(todo == null ? "" : todo.text(), done));

        assertNull(doneSignal.value());

        doneSignal.value(true);
        assertNotNull(todoSignal.value());
        assertTrue(todoSignal.value().done());
    }

    @Test
    void map_stringField_works() {
        ValueSignal<Todo> todoSignal = new ValueSignal<>(
                new Todo("Buy milk", false));
        WritableSignal<String> textSignal = todoSignal.map(Todo::text,
                Todo::withText);

        assertEquals("Buy milk", textSignal.value());

        textSignal.value("Buy bread");
        assertEquals("Buy bread", todoSignal.value().text());
        assertFalse(todoSignal.value().done());
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
