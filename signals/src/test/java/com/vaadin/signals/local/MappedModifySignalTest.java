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
package com.vaadin.signals.local;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalTestBase;
import com.vaadin.signals.TestUtil;
import com.vaadin.signals.WritableSignal;
import com.vaadin.signals.operations.CancelableOperation;
import com.vaadin.signals.operations.SignalOperation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MappedModifySignalTest extends SignalTestBase {

    static class MutableTodo {
        private String text;
        private boolean done;

        MutableTodo(String text, boolean done) {
            this.text = text;
            this.done = done;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }

    @Test
    void map_readValue_returnsMappedValue() {
        MutableTodo todo = new MutableTodo("Buy milk", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);

        assertFalse(doneSignal.value());

        todoSignal.modify(t -> t.setDone(true));
        assertTrue(doneSignal.value());
    }

    @Test
    void map_peek_returnsMappedValue() {
        MutableTodo todo = new MutableTodo("Buy milk", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);

        assertFalse(doneSignal.peek());
    }

    @Test
    void map_setValue_modifiesParentValueInPlace() {
        MutableTodo todo = new MutableTodo("Buy milk", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);

        doneSignal.value(true);

        assertTrue(todo.isDone());
        assertSame(todo, todoSignal.value());
    }

    @Test
    void map_setValue_returnsPreviousValue() {
        MutableTodo todo = new MutableTodo("Buy milk", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);

        SignalOperation<Boolean> operation = doneSignal.value(true);

        Boolean oldValue = TestUtil.assertSuccess(operation);
        assertFalse(oldValue);
    }

    @Test
    void map_setValuePreservesOtherFields() {
        MutableTodo todo = new MutableTodo("Original text", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);

        doneSignal.value(true);

        assertEquals("Original text", todo.getText());
    }

    @Test
    void map_replaceWithExpectedValue_updatesSignal() {
        MutableTodo todo = new MutableTodo("Buy milk", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);

        SignalOperation<Void> operation = doneSignal.replace(false, true);

        TestUtil.assertSuccess(operation);
        assertTrue(doneSignal.value());
        assertTrue(todo.isDone());
    }

    @Test
    void map_replaceWithUnexpectedValue_fails() {
        MutableTodo todo = new MutableTodo("Buy milk", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);

        SignalOperation<Void> operation = doneSignal.replace(true, false);

        TestUtil.assertFailure(operation);
        assertFalse(doneSignal.value());
        assertFalse(todo.isDone());
    }

    @Test
    void map_update_modifiesInPlace() {
        MutableTodo todo = new MutableTodo("Task", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);

        CancelableOperation<Boolean> operation = doneSignal
                .update(value -> !value);

        Boolean oldValue = TestUtil.assertSuccess(operation);
        assertFalse(oldValue);
        assertTrue(doneSignal.value());
        assertTrue(todo.isDone());
        assertSame(todo, todoSignal.value());
    }

    @Test
    void map_update_preservesOtherFields() {
        MutableTodo todo = new MutableTodo("Task", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);

        doneSignal.update(value -> !value);

        assertEquals("Task", todo.getText());
    }

    @Test
    void map_effectTracking_tracksChanges() {
        MutableTodo todo = new MutableTodo("Buy milk", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);

        AtomicInteger effectCount = new AtomicInteger();
        Signal.effect(() -> {
            doneSignal.value();
            effectCount.incrementAndGet();
        });

        assertEquals(1, effectCount.get());

        doneSignal.value(true);
        assertEquals(2, effectCount.get());
    }

    @Test
    void map_asReadonly_createsReadonlyView() {
        MutableTodo todo = new MutableTodo("Buy milk", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);
        Signal<Boolean> readonlySignal = doneSignal.asReadonly();

        assertFalse(readonlySignal instanceof WritableSignal);

        doneSignal.value(true);
        assertTrue(readonlySignal.value());
    }

    @Test
    void map_multipleFieldMappings_independent() {
        MutableTodo todo = new MutableTodo("Task", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<String> textSignal = todoSignal
                .mapMutable(MutableTodo::getText, MutableTodo::setText);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);

        textSignal.value("Updated task");
        assertEquals("Updated task", todo.getText());
        assertFalse(todo.isDone());

        doneSignal.value(true);
        assertEquals("Updated task", todo.getText());
        assertTrue(todo.isDone());
    }

    @Test
    void map_sameInstanceMaintained_afterModification() {
        MutableTodo todo = new MutableTodo("Task", false);
        ValueSignal<MutableTodo> todoSignal = new ValueSignal<>(todo);
        WritableSignal<Boolean> doneSignal = todoSignal
                .mapMutable(MutableTodo::isDone, MutableTodo::setDone);

        doneSignal.value(true);
        assertSame(todo, todoSignal.value());

        doneSignal.value(false);
        assertSame(todo, todoSignal.value());

        doneSignal.update(d -> !d);
        assertSame(todo, todoSignal.value());
    }
}
