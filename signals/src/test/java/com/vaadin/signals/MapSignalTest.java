/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MapSignalTest extends SignalTestBase {

    @Test
    void constructor_initialValue_isEmpty() {
        MapSignal<String> signal = new MapSignal<>(String.class);

        var value = signal.value();
        assertNotNull(value);
        int size = value.size();

        assertEquals(0, size);
    }

    /*
     * Note that we're not testing all possible failure cases for the operations
     * since those are already covered by MutableTreeRevision tests
     */

    @Test
    void put_missingKey_newEntryCreated() {
        MapSignal<String> signal = new MapSignal<>(String.class);

        SignalOperation<String> operation = signal.put("key", "value");

        String resultValue = assertSuccess(operation);
        assertNull(resultValue);
        assertChildren(signal, "key", "value");
    }

    @Test
    void put_existingKey_oldEntryUpdated() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "initial");
        var value = signal.value();
        assertNotNull(value);
        ValueSignal<String> child = value.get("key");

        SignalOperation<String> operation = signal.put("key", "update");

        String resultValue = assertSuccess(operation);
        assertEquals("initial", resultValue);
        assertNotNull(child);
        assertEquals("update", child.value());
        assertChildren(signal, "key", "update");
    }

    @Test
    void put_multiplePuts_insertionOrderPreserved() {
        MapSignal<String> signal = new MapSignal<>(String.class);

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

        var signalValue = signal.value();
        assertNotNull(signalValue);
        List<String> keyOrder = List.copyOf(signalValue.keySet());
        assertEquals(insertOrder, keyOrder);
    }

    @Test
    void putIfAbsent_missingKey_newEntryCreated() {
        MapSignal<String> signal = new MapSignal<>(String.class);

        InsertOperation<ValueSignal<String>> operation = signal
                .putIfAbsent("key", "value");
        ValueSignal<String> child = operation.signal();

        assertSuccess(operation);
        assertEquals("value", child.value());
        assertChildren(signal, "key", "value");

        child.value("update");
        assertChildren(signal, "key", "update");
    }

    @Test
    void putIfAbsent_existingKey_noUpdateAndEntiresLinked() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");
        var value = signal.value();
        assertNotNull(value);
        ValueSignal<String> child = value.get("key");

        InsertOperation<ValueSignal<String>> operation = signal
                .putIfAbsent("key", "update");
        ValueSignal<String> insertChild = operation.signal();

        assertNotNull(child);
        assertNotEquals(child.id(), insertChild.id());

        assertSuccess(operation);
        assertEquals("value", insertChild.value());
        assertChildren(signal, "key", "value");

        insertChild.value("update");
        assertEquals("update", child.value());
        assertChildren(signal, "key", "update");
    }

    @Test
    void remove_exisingKey_removed() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<String> operation = signal.remove("key");

        String resultValue = assertSuccess(operation);
        assertEquals("value", resultValue);
        assertChildren(signal);
    }

    @Test
    void remove_missingKey_operationFailed() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<String> operation = signal.remove("other");

        assertFailure(operation);
        assertChildren(signal, "key", "value");
    }

    @Test
    void clear_mapWithEntries_cleared() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<Void> operation = signal.clear();

        assertSuccess(operation);
        assertChildren(signal);
    }

    @Test
    void verifyKey_matchingKey_success() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");

        var value = signal.value();
        assertNotNull(value);
        var child = value.get("key");
        assertNotNull(child);
        SignalOperation<Void> operation = signal.verifyKey("key", child);

        assertSuccess(operation);
    }

    @Test
    void verifyKey_mismatchingKey_success() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");
        signal.put("key2", "value2");

        var value = signal.value();
        assertNotNull(value);
        var child2 = value.get("key2");
        assertNotNull(child2);
        SignalOperation<Void> operation = signal.verifyKey("key", child2);

        assertFailure(operation);
    }

    @Test
    void verifyHasKey_hasKey_success() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<Void> operation = signal.verifyHasKey("key");

        assertSuccess(operation);
    }

    @Test
    void verifyHasKey_hasNoKey_failure() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<Void> operation = signal.verifyHasKey("key2");

        assertFailure(operation);
    }

    @Test
    void verifyKeyAbsent_hasNoKey_success() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<Void> operation = signal.verifyKeyAbsent("key2");

        assertSuccess(operation);
    }

    @Test
    void verifyKeyAbsent_hasKey_failure() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");

        SignalOperation<Void> operation = signal.verifyKeyAbsent("key");

        assertFailure(operation);
    }

    @Test
    void value_modifyMapInstance_immutable() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");

        Map<String, ValueSignal<String>> value = signal.value();
        assertNotNull(value);

        assertThrows(UnsupportedOperationException.class, () -> {
            value.put("key", new ValueSignal<>("update"));
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            value.clear();
        });

        assertChildren(signal, "key", "value");
    }

    @Test
    void value_readMapAfterUpdate_readsOldData() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");

        Map<String, ValueSignal<String>> value = signal.value();
        assertNotNull(value);

        signal.put("key2", "value2");

        assertFalse(value.containsKey("key2"));
    }

    @Test
    void withValidator_spyingValidator_seesParentAndChildOperations() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        List<SignalCommand> validatedCommands = new ArrayList<>();

        MapSignal<String> wrapper = signal.withValidator(command -> {
            validatedCommands.add(command);
            return true;
        });

        wrapper.put("key", "value");

        assertEquals(1, validatedCommands.size());
        assertInstanceOf(SignalCommand.PutCommand.class,
                validatedCommands.get(0));

        var wrapperValue = wrapper.value();
        assertNotNull(wrapperValue);
        var child = wrapperValue.get("key");
        assertNotNull(child);
        child.value("update");
        assertEquals(2, validatedCommands.size());
        assertInstanceOf(SignalCommand.ValueCommand.class,
                validatedCommands.get(1));
    }

    @Test
    void readonly_makeChangesToMapAndChild_allChangesRejected() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key", "value");

        MapSignal<String> readonly = signal.asReadonly();
        var readonlyValue = readonly.value();
        assertNotNull(readonlyValue);
        ValueSignal<String> readonlyChild = readonlyValue.get("key");

        assertThrows(UnsupportedOperationException.class, () -> {
            readonly.clear();
        });
        assertChildren(signal, "key", "value");

        assertNotNull(readonlyChild);
        assertThrows(UnsupportedOperationException.class, () -> {
            readonlyChild.value("update");
        });
        assertChildren(signal, "key", "value");
    }

    @Test
    void usageTracking_changeDifferentValues_onlyMapChangeDetected() {
        MapSignal<String> signal = new MapSignal<>(String.class);

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        signal.asNode().asValue(String.class).value("value");
        assertFalse(usage.hasChanges());

        signal.put("key", "value");
        assertTrue(usage.hasChanges());
    }

    @Test
    void equalsHashCode() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        assertEquals(signal, signal);

        MapSignal<String> copy = new MapSignal<>(signal.tree(), signal.id(),
                signal.validator(), String.class);
        assertEquals(signal, copy);
        assertEquals(signal.hashCode(), copy.hashCode());

        MapSignal<String> asValue = signal.asNode().asMap(String.class);
        assertEquals(signal, asValue);
        assertEquals(signal.hashCode(), asValue.hashCode());

        assertNotEquals(signal, new MapSignal<>(String.class));
        assertNotEquals(signal, signal.asReadonly());
        assertNotEquals(signal, signal.asNode());
        assertNotEquals(signal, signal.asNode().asList(Double.class));
    }

    @Test
    void equalsHashCode_children() {
        MapSignal<String> signal = new MapSignal<>(String.class);

        ValueSignal<String> operationChild = signal
                .putIfAbsent("child", "value").signal();
        ValueSignal<String> other = signal.putIfAbsent("other", "other")
                .signal();

        var value = signal.value();
        assertNotNull(value);
        ValueSignal<String> valueChild = value.get("child");

        assertNotNull(valueChild);
        assertEquals(operationChild, valueChild);
        assertEquals(operationChild.hashCode(), valueChild.hashCode());

        assertNotEquals(operationChild, other);
    }

    @Test
    void toString_includesValue() {
        MapSignal<String> signal = new MapSignal<>(String.class);
        signal.put("key1", "value1");
        signal.put("key2", "value2");

        assertEquals("MapSignal[key1=value1, key2=value2]", signal.toString());
    }

    private void assertChildren(MapSignal<String> signal,
            String... expectedKeyValuePairs) {
        assertEquals(0, expectedKeyValuePairs.length % 2);

        Map<String, ValueSignal<String>> value = signal.value();
        assertNotNull(value);

        assertEquals(expectedKeyValuePairs.length / 2, value.size());

        for (int i = 0; i < expectedKeyValuePairs.length; i += 2) {
            String key = expectedKeyValuePairs[i];
            String expextedValue = expectedKeyValuePairs[i + 1];

            assertTrue(value.containsKey(key));
            var valueSignal = value.get(key);
            assertNotNull(valueSignal);
            assertEquals(expextedValue, valueSignal.value());
        }
    }

}
