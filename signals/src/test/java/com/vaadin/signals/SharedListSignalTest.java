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
package com.vaadin.signals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.SharedListSignal.ListPosition;
import com.vaadin.signals.impl.Transaction;
import com.vaadin.signals.impl.UsageTracker;
import com.vaadin.signals.impl.UsageTracker.Usage;
import com.vaadin.signals.operations.InsertOperation;
import com.vaadin.signals.operations.SignalOperation;

import static com.vaadin.signals.TestUtil.assertFailure;
import static com.vaadin.signals.TestUtil.assertSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SharedListSignalTest extends SignalTestBase {
    @Test
    void constructor_initialValue_isEmpty() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);

        int size = signal.value().size();

        assertEquals(0, size);
    }

    @Test
    void insertFirst_twoInserts_insertedInOrder() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);

        signal.insertFirst("a");
        signal.insertFirst("b");

        assertChildren(signal, "b", "a");
    }

    @Test
    void insertFirst_concurrentInserts_insertedInConfirmOrder() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);

        signal.insertFirst("initial");
        Signal.runInTransaction(() -> {
            signal.insertFirst("transaction");
            assertChildren(signal, "transaction", "initial");

            Signal.runWithoutTransaction(() -> {
                signal.insertFirst("direct");
                assertChildren(signal, "direct", "initial");
            });
        });

        assertChildren(signal, "transaction", "direct", "initial");
    }

    @Test
    void insertLast_twoInserts_insertedInOrder() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);

        signal.insertLast("a");
        signal.insertLast("b");

        assertChildren(signal, "a", "b");
    }

    @Test
    void insertLast_concurrentInserts_insertedInConfirmOrder() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);

        signal.insertLast("initial");
        Signal.runInTransaction(() -> {
            signal.insertLast("transaction");
            assertChildren(signal, "initial", "transaction");

            Signal.runWithoutTransaction(() -> {
                signal.insertLast("direct");
                assertChildren(signal, "initial", "direct");
            });
        });

        assertChildren(signal, "initial", "direct", "transaction");
    }

    /*
     * Note that we're not testing all possible failure cases for the operations
     * since those are already covered by MutableTreeRevision tests
     */

    @Test
    void insertAt_validLocation_insertSuccessful() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        signal.insertFirst("first").signal();
        signal.insertLast("last").signal();

        InsertOperation<SharedValueSignal<String>> operation = signal
                .insertAt("afterLast", ListPosition.before(null));

        assertSuccess(operation);
        assertChildren(signal, "first", "last", "afterLast");
    }

    @Test
    void insertAt_invalidLocation_insertFailed() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        SharedValueSignal<String> first = signal.insertFirst("first").signal();
        SharedValueSignal<String> last = signal.insertLast("last").signal();

        InsertOperation<SharedValueSignal<String>> operation = signal
                .insertAt("invalid", ListPosition.between(last, first));

        assertFailure(operation);
        assertChildren(signal, "first", "last");
    }

    @Test
    void insert_updateUnconfirmedInsertedSignal_valueUpdated() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);

        SharedValueSignal<String> child = Transaction.runInTransaction(() -> {
            SharedValueSignal<String> childInner = signal.insertLast("insert")
                    .signal();

            assertEquals(0, signal.peekConfirmed().size());
            assertNull(childInner.peekConfirmed());

            childInner.value("update");

            return childInner;
        }).returnValue();

        assertEquals(1, signal.peekConfirmed().size());
        assertEquals("update", child.peekConfirmed());
    }

    @Test
    void moveTo_validLocation_moveSuccessful() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        signal.insertFirst("first").signal();
        SharedValueSignal<String> middle = signal.insertLast("middle").signal();
        SharedValueSignal<String> last = signal.insertLast("last").signal();

        SignalOperation<Void> operation = signal.moveTo(middle,
                ListPosition.after(last));

        assertSuccess(operation);
        assertChildren(signal, "first", "last", "middle");
    }

    @Test
    void moveTo_invalidLocation_moveFailed() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        SharedValueSignal<String> first = signal.insertFirst("first").signal();
        SharedValueSignal<String> middle = signal.insertLast("middle").signal();
        SharedValueSignal<String> last = signal.insertLast("last").signal();

        SignalOperation<Void> operation = signal.moveTo(middle,
                ListPosition.between(last, first));

        assertFailure(operation);
        assertChildren(signal, "first", "middle", "last");
    }

    @Test
    void remove_existingChild_removeSuccessful() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        SharedValueSignal<String> first = signal.insertFirst("first").signal();
        signal.insertLast("last").signal();

        SignalOperation<Void> operation = signal.remove(first);

        assertSuccess(operation);
        assertChildren(signal, "last");
    }

    @Test
    void remove_nonExtistentChild_removeFails() {
        SharedListSignal<String> other = new SharedListSignal<>(String.class);
        SharedValueSignal<String> child = other.insertLast("child").signal();

        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        signal.insertLast("child");

        SignalOperation<Void> operation = signal.remove(child);
        assertFailure(operation);
        assertChildren(signal, "child");
    }

    @Test
    void clear_listWithChildren_listCleared() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        signal.insertFirst("first").signal();
        signal.insertLast("last").signal();

        SignalOperation<Void> operation = signal.clear();

        assertSuccess(operation);
        assertChildren(signal);
    }

    @Test
    void verifyPosition_correctPosition_successful() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        SharedValueSignal<String> first = signal.insertFirst("first").signal();
        SharedValueSignal<String> last = signal.insertLast("last").signal();

        SignalOperation<Void> operation = signal.verifyPosition(last,
                ListPosition.after(first));

        assertSuccess(operation);
    }

    @Test
    void verifyPosition_invalidPosition_failure() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        SharedValueSignal<String> first = signal.insertFirst("first").signal();
        SharedValueSignal<String> last = signal.insertLast("last").signal();

        SignalOperation<Void> operation = signal.verifyPosition(last,
                ListPosition.before(first));

        assertFailure(operation);
    }

    @Test
    void verifyChild_isChild_successful() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        SharedValueSignal<String> child = signal.insertFirst("child").signal();

        SignalOperation<Void> operation = signal.verifyChild(child);

        assertSuccess(operation);
    }

    @Test
    void verifyChild_isNotChild_failure() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        signal.insertFirst("child").signal();

        SignalOperation<Void> operation = signal
                .verifyChild(new SharedValueSignal<>("child"));

        assertFailure(operation);
    }

    @Test
    void value_modifyListInstance_isImmutable() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        signal.insertFirst("first");

        List<SharedValueSignal<String>> value = signal.value();

        assertThrows(UnsupportedOperationException.class, () -> {
            value.add(new SharedValueSignal<>("new"));
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            value.set(0, new SharedValueSignal<>("new"));
        });
    }

    @Test
    void value_changeSignalBeforeIterating_iteratesOldState() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        signal.insertFirst("first");

        List<SharedValueSignal<String>> value = signal.value();

        signal.insertLast("last");

        List<String> list = value.stream().map(SharedValueSignal::value)
                .toList();
        assertEquals(List.of("first"), list);
    }

    @Test
    void withValidator_spyingValidator_seesParentAndChildOperations() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        List<SignalCommand> validatedCommands = new ArrayList<>();

        SharedListSignal<String> wrapper = signal.withValidator(command -> {
            validatedCommands.add(command);
            return true;
        });

        SharedValueSignal<String> child = wrapper.insertFirst("child").signal();

        assertEquals(1, validatedCommands.size());
        assertInstanceOf(SignalCommand.InsertCommand.class,
                validatedCommands.get(0));

        child.value("update");
        assertEquals(2, validatedCommands.size());
        assertInstanceOf(SignalCommand.ValueCommand.class,
                validatedCommands.get(1));
    }

    @Test
    void readonly_makeChangesToListAndChild_allChangesRejected() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        signal.insertLast("child");

        SharedListSignal<String> readonly = signal.asReadonly();
        SharedValueSignal<String> readonlyChild = readonly.value().get(0);

        assertThrows(UnsupportedOperationException.class, () -> {
            readonly.clear();
        });
        assertChildren(signal, "child");

        assertThrows(UnsupportedOperationException.class, () -> {
            readonlyChild.value("update");
        });
        assertChildren(signal, "child");
    }

    @Test
    void usageTracking_changeDifferentValues_onlyListChangeDetected() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        signal.asNode().asValue(String.class).value("value");
        assertFalse(usage.hasChanges());

        signal.insertLast("insert");
        assertTrue(usage.hasChanges());
    }

    @Test
    void equalsHashCode() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        assertEquals(signal, signal);

        SharedListSignal<String> copy = new SharedListSignal<>(signal.tree(),
                signal.id(), signal.validator(), String.class);
        assertEquals(signal, copy);
        assertEquals(signal.hashCode(), copy.hashCode());

        SharedListSignal<String> asList = signal.asNode().asList(String.class);
        assertEquals(signal, asList);
        assertEquals(signal.hashCode(), asList.hashCode());

        assertNotEquals(signal, new SharedListSignal<>(String.class));
        assertNotEquals(signal, signal.asReadonly());
        assertNotEquals(signal, signal.asNode());
        assertNotEquals(signal, signal.asNode().asList(Double.class));
    }

    @Test
    void equalsHashCode_children() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);

        SharedValueSignal<String> operationChild = signal.insertLast("value")
                .signal();
        SharedValueSignal<String> other = signal.insertLast("other").signal();

        SharedValueSignal<String> valueChild = signal.value().get(0);

        assertEquals(operationChild, valueChild);
        assertEquals(operationChild.hashCode(), valueChild.hashCode());

        assertNotEquals(operationChild, other);
    }

    @Test
    void toString_includesValue() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        signal.insertLast("one");
        signal.insertLast("two");

        assertEquals("SharedListSignal[one, two]", signal.toString());
    }

    static void assertChildren(SharedListSignal<String> signal,
            String... expectedValue) {
        List<String> value = signal.value().stream()
                .map(SharedValueSignal::value).toList();

        assertEquals(List.of(expectedValue), value);
    }
}
