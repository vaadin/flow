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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.impl.UsageTracker;
import com.vaadin.flow.signals.impl.UsageTracker.Usage;
import com.vaadin.flow.signals.operations.PutIfAbsentResult;
import com.vaadin.flow.signals.operations.SignalOperation;

import static com.vaadin.flow.signals.TestUtil.assertFailure;
import static com.vaadin.flow.signals.TestUtil.assertSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SharedMapSignalTest extends SignalTestBase {

    @Test
    void constructor_initialValue_isEmpty() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);

        int size = signal.get().size();

        assertEquals(0, size);
    }

    /*
     * Note that we're not testing all possible failure cases for the operations
     * since those are already covered by MutableTreeRevision tests
     */

    @Test
    void put_missingKey_newEntryCreated() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);

        SignalOperation<String> operation = signal.put("key", "value");

        String resultValue = assertSuccess(operation);
        assertNull(resultValue);
        assertChildren(signal, "key", "value");
    }

    @Test
    void put_existingKey_oldEntryUpdated() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "initial");
        SharedValueSignal<String> child = signal.get().get("key");

        SignalOperation<String> operation = signal.put("key", "update");

        String resultValue = assertSuccess(operation);
        assertEquals("initial", resultValue);
        assertEquals("update", child.get());
        assertChildren(signal, "key", "update");
    }

    @Test
    void put_multiplePuts_insertionOrderPreserved() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);

        List<String> insertOrder = Stream
                .generate(() -> UUID.randomUUID().toString()).limit(10)
                .toList();

        for (String key : insertOrder) {
            signal.put(key, null);
        }

        ArrayList<String> updateOrder = new ArrayList<>(insertOrder);
        Collections.shuffle(updateOrder);
        for (String key : updateOrder) {
            signal.put(key, "update");
        }

        List<String> keyOrder = List.copyOf(signal.get().keySet());
        assertEquals(insertOrder, keyOrder);
    }

    @Test
    void putIfAbsent_missingKey_newEntryCreated() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);

        SignalOperation<PutIfAbsentResult<SharedValueSignal<String>>> operation = signal
                .putIfAbsent("key", "value");

        PutIfAbsentResult<SharedValueSignal<String>> result = assertSuccess(
                operation);
        assertTrue(result.created());
        SharedValueSignal<String> child = result.entry();
        assertEquals("value", child.get());
        assertChildren(signal, "key", "value");

        child.set("update");
        assertChildren(signal, "key", "update");
    }

    @Test
    void putIfAbsent_existingKey_noUpdateAndEntryUnchanged() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");
        SharedValueSignal<String> child = signal.get().get("key");

        SignalOperation<PutIfAbsentResult<SharedValueSignal<String>>> operation = signal
                .putIfAbsent("key", "update");

        PutIfAbsentResult<SharedValueSignal<String>> result = assertSuccess(
                operation);
        assertFalse(result.created());
        SharedValueSignal<String> resultChild = result.entry();

        assertEquals("value", resultChild.get());
        assertChildren(signal, "key", "value");
        assertEquals(child.id(), resultChild.id());
    }

    @Test
    void remove_exisingKey_removed() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<String> operation = signal.remove("key");

        String resultValue = assertSuccess(operation);
        assertEquals("value", resultValue);
        assertChildren(signal);
    }

    @Test
    void remove_missingKey_operationFailed() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<String> operation = signal.remove("other");

        assertFailure(operation);
        assertChildren(signal, "key", "value");
    }

    @Test
    void clear_mapWithEntries_cleared() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<Void> operation = signal.clear();

        assertSuccess(operation);
        assertChildren(signal);
    }

    @Test
    void verifyKey_matchingKey_success() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<Void> operation = signal.verifyKey("key",
                signal.get().get("key"));

        assertSuccess(operation);
    }

    @Test
    void verifyKey_mismatchingKey_success() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");
        signal.put("key2", "value2");

        SignalOperation<Void> operation = signal.verifyKey("key",
                signal.get().get("key2"));

        assertFailure(operation);
    }

    @Test
    void verifyHasKey_hasKey_success() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<Void> operation = signal.verifyHasKey("key");

        assertSuccess(operation);
    }

    @Test
    void verifyHasKey_hasNoKey_failure() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<Void> operation = signal.verifyHasKey("key2");

        assertFailure(operation);
    }

    @Test
    void verifyKeyAbsent_hasNoKey_success() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<Void> operation = signal.verifyKeyAbsent("key2");

        assertSuccess(operation);
    }

    @Test
    void verifyKeyAbsent_hasKey_failure() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<Void> operation = signal.verifyKeyAbsent("key");

        assertFailure(operation);
    }

    @Test
    void value_modifyMapInstance_immutable() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");

        Map<String, SharedValueSignal<String>> value = signal.get();

        assertThrows(UnsupportedOperationException.class, () -> {
            value.put("key", new SharedValueSignal<>("update"));
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            value.clear();
        });

        assertChildren(signal, "key", "value");
    }

    @Test
    void value_readMapAfterUpdate_readsOldData() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");

        Map<String, SharedValueSignal<String>> value = signal.get();

        signal.put("key2", "value2");

        assertFalse(value.containsKey("key2"));
    }

    @Test
    void withValidator_spyingValidator_seesParentAndChildOperations() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        List<SignalCommand> validatedCommands = new ArrayList<>();

        SharedMapSignal<String> wrapper = signal.withValidator(command -> {
            validatedCommands.add(command);
            return true;
        });

        wrapper.put("key", "value");

        assertEquals(1, validatedCommands.size());
        assertInstanceOf(SignalCommand.PutCommand.class,
                validatedCommands.get(0));

        wrapper.get().get("key").set("update");
        assertEquals(2, validatedCommands.size());
        assertInstanceOf(SignalCommand.ValueCommand.class,
                validatedCommands.get(1));
    }

    @Test
    void readonly_makeChangesToMapAndChild_allChangesRejected() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key", "value");

        SharedMapSignal<String> readonly = signal.asReadonly();
        SharedValueSignal<String> readonlyChild = readonly.get().get("key");

        assertThrows(UnsupportedOperationException.class, () -> {
            readonly.clear();
        });
        assertChildren(signal, "key", "value");

        assertThrows(UnsupportedOperationException.class, () -> {
            readonlyChild.set("update");
        });
        assertChildren(signal, "key", "value");
    }

    @Test
    void usageTracking_changeDifferentValues_onlyMapChangeDetected() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        signal.asNode().asValue(String.class).set("value");
        assertFalse(usage.hasChanges());

        signal.put("key", "value");
        assertTrue(usage.hasChanges());
    }

    @Test
    void equalsHashCode() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        assertEquals(signal, signal);

        SharedMapSignal<String> copy = new SharedMapSignal<>(signal.tree(),
                signal.id(), signal.validator(), String.class);
        assertEquals(signal, copy);
        assertEquals(signal.hashCode(), copy.hashCode());

        SharedMapSignal<String> asValue = signal.asNode().asMap(String.class);
        assertEquals(signal, asValue);
        assertEquals(signal.hashCode(), asValue.hashCode());

        assertNotEquals(signal, new SharedMapSignal<>(String.class));
        assertNotEquals(signal, signal.asReadonly());
        assertNotEquals(signal, signal.asNode());
        assertNotEquals(signal, signal.asNode().asList(Double.class));
    }

    @Test
    void equalsHashCode_children() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);

        signal.putIfAbsent("child", "value");
        signal.putIfAbsent("other", "other");

        SharedValueSignal<String> child = signal.get().get("child");
        SharedValueSignal<String> other = signal.get().get("other");

        SharedValueSignal<String> valueChild = signal.get().get("child");

        assertEquals(child, valueChild);
        assertEquals(child.hashCode(), valueChild.hashCode());

        assertNotEquals(child, other);
    }

    @Test
    void toString_includesValue() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        signal.put("key1", "value1");
        signal.put("key2", "value2");

        assertEquals("SharedMapSignal[key1=value1, key2=value2]",
                signal.toString());
    }

    private void assertChildren(SharedMapSignal<String> signal,
            String... expectedKeyValuePairs) {
        assertEquals(0, expectedKeyValuePairs.length % 2);

        Map<String, SharedValueSignal<String>> value = signal.get();

        assertEquals(expectedKeyValuePairs.length / 2, value.size());

        for (int i = 0; i < expectedKeyValuePairs.length; i += 2) {
            String key = expectedKeyValuePairs[i];
            String expextedValue = expectedKeyValuePairs[i + 1];

            assertTrue(value.containsKey(key));
            assertEquals(expextedValue, value.get(key).get());
        }
    }

}
