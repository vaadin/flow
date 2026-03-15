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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.impl.UsageTracker;
import com.vaadin.flow.signals.impl.UsageTracker.Usage;
import com.vaadin.flow.signals.operations.InsertOperation;
import com.vaadin.flow.signals.operations.PutIfAbsentResult;
import com.vaadin.flow.signals.operations.SignalOperation;
import com.vaadin.flow.signals.shared.SharedListSignal.ListPosition;
import com.vaadin.flow.signals.shared.SharedNodeSignal.SharedNodeSignalState;

import static com.vaadin.flow.signals.TestUtil.assertFailure;
import static com.vaadin.flow.signals.TestUtil.assertSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SharedNodeSignalTest extends SignalTestBase {

    @Test
    void constructor_initialValue_isEmpty() {
        SharedNodeSignal signal = new SharedNodeSignal();

        SharedNodeSignalState value = signal.peek();

        assertNull(value.value(String.class));
        assertNull(value.parent());
        assertEquals(0, value.listChildren().size());
        assertEquals(0, value.mapChildren().size());
    }

    @Test
    void insertValue_emptyNode_getsChildNodeWithValue() {
        SharedNodeSignal signal = new SharedNodeSignal();

        InsertOperation<SharedNodeSignal> operation = signal
                .insertChildWithValue("value", ListPosition.last());

        assertSuccess(operation);

        List<SharedNodeSignal> listChildren = signal.peek().listChildren();
        assertEquals(1, listChildren.size());
        assertEquals(operation.signal().id(), listChildren.get(0).id());
        assertEquals("value", operation.signal().peek().value(String.class));
    }

    @Test
    void asValue_updateValueThroughWrapper_valueUpdated() {
        SharedNodeSignal signal = new SharedNodeSignal();

        SharedValueSignal<String> asValue = signal.asValue(String.class);
        assertEquals(null, asValue.peek());

        asValue.set("update");
        assertEquals("update", signal.peek().value(String.class));
    }

    @Test
    void asValue_incompatibleValue_getterThrows() {
        SharedNodeSignal signal = new SharedNodeSignal();

        SharedValueSignal<String> asString = signal.asValue(String.class);
        asString.set("update");

        assertThrows(RuntimeException.class, () -> {
            signal.peek().value(Double.class);
        });

        SharedValueSignal<Double> asDouble = signal.asValue(Double.class);
        assertThrows(RuntimeException.class, () -> {
            asDouble.peek();
        });
    }

    @Test
    void asNumber_nullValue_readsAndIncrementsAsZero() {
        SharedNodeSignal signal = new SharedNodeSignal();

        SharedNumberSignal asNumber = signal.asNumber();
        assertEquals(0, asNumber.peek());

        asNumber.incrementBy(1);
        assertEquals(1, asNumber.peek());

        assertEquals(1, signal.peek().value(Double.class));
    }

    @Test
    void asList_insertThroughWrapper_valueUpdated() {
        SharedNodeSignal signal = new SharedNodeSignal();
        SharedListSignal<String> asList = signal.asList(String.class);

        asList.insertLast("last");

        List<SharedNodeSignal> listChildren = signal.peek().listChildren();
        assertEquals(1, listChildren.size());

        assertEquals("last", listChildren.get(0).peek().value(String.class));
    }

    @Test
    void asList_insertThroughNode_wrapperUpdated() {
        SharedNodeSignal signal = new SharedNodeSignal();
        SharedListSignal<String> asList = signal.asList(String.class);

        signal.insertChildWithValue("last", ListPosition.last());

        List<SharedValueSignal<String>> value = asList.peek();
        assertEquals(1, value.size());

        assertEquals("last", value.get(0).peek());
    }

    @Test
    void asMap_putThroughWrapper_valueUpdate() {
        SharedNodeSignal signal = new SharedNodeSignal();
        SharedMapSignal<String> asMap = signal.asMap(String.class);

        asMap.put("key", "value");

        Map<String, SharedNodeSignal> mapChildren = signal.peek().mapChildren();
        assertEquals(Set.of("key"), mapChildren.keySet());

        SharedNodeSignal child = mapChildren.get("key");
        assertNotNull(child);
        assertEquals("value", child.peek().value(String.class));
    }

    @Test
    void asMap_putThroughNode_wrapperUpdated() {
        SharedNodeSignal signal = new SharedNodeSignal();
        SharedMapSignal<String> asMap = signal.asMap(String.class);

        signal.putChildWithValue("key", "value");

        Map<String, SharedValueSignal<String>> value = asMap.peek();

        assertEquals(Set.of("key"), value.keySet());
        SharedValueSignal<String> keySignal = value.get("key");
        assertNotNull(keySignal);
        assertEquals("value", keySignal.peek());
    }

    /*
     * Several operations are already covered by the tests above so not going
     * through those separately.
     */

    @Test
    void insertChild_emptyNode_insertsEmptyListChild() {
        SharedNodeSignal signal = new SharedNodeSignal();

        InsertOperation<SharedNodeSignal> operation = signal
                .insertChild(ListPosition.last());
        SharedNodeSignal child = operation.signal();

        assertEquals(List.of(child), signal.peek().listChildren());
        assertEquals(null, child.peek().value(String.class));
    }

    @Test
    void putChildIfAbsent_emptyNode_insertsEmptyMapChild() {
        SharedNodeSignal signal = new SharedNodeSignal();

        SignalOperation<PutIfAbsentResult<SharedNodeSignal>> operation = signal
                .putChildIfAbsent("key");
        PutIfAbsentResult<SharedNodeSignal> result = assertSuccess(operation);
        assertTrue(result.created());
        SharedNodeSignal child = signal.peek().mapChildren().get("key");
        assertNotNull(child);
        assertEquals(Map.of("key", child), signal.peek().mapChildren());
        assertEquals(null, child.peek().value(String.class));
    }

    @Test
    void adpotAt_existingChild_orderChanged() {
        SharedNodeSignal signal = new SharedNodeSignal();

        SharedNodeSignal a = signal.insertChild(ListPosition.last()).signal();
        SharedNodeSignal b = signal.insertChild(ListPosition.last()).signal();

        SignalOperation<Void> operation = signal.adoptAt(a,
                ListPosition.last());

        assertSuccess(operation);
        assertEquals(List.of(b, a), signal.peek().listChildren());
        assertEquals(signal, a.peek().parent());
    }

    @Test
    void adoptAt_nestedStructure_hierarchyChagned() {
        SharedNodeSignal signal = new SharedNodeSignal();

        SharedNodeSignal parent = signal.insertChild(ListPosition.last())
                .signal();
        SharedNodeSignal child = parent.insertChild(ListPosition.last())
                .signal();

        assertEquals(parent, child.peek().parent());

        SignalOperation<Void> operation = signal.adoptAt(child,
                ListPosition.first());

        assertSuccess(operation);
        assertEquals(signal, child.peek().parent());
        assertEquals(List.of(child, parent), signal.peek().listChildren());
        assertEquals(List.of(), parent.peek().listChildren());
    }

    @Test
    void adoptAt_addParentToChild_rejected() {
        SharedNodeSignal signal = new SharedNodeSignal();

        SharedNodeSignal parent = signal.insertChild(ListPosition.last())
                .signal();
        SharedNodeSignal child = parent.insertChild(ListPosition.last())
                .signal();

        SignalOperation<Void> operation = child.adoptAt(parent,
                ListPosition.first());

        assertFailure(operation);
    }

    @Test
    void adoptAt_adoptMapChild_noLongerMapChild() {
        SharedNodeSignal signal = new SharedNodeSignal();
        signal.putChildIfAbsent("key");
        SharedNodeSignal child = signal.peek().mapChildren().get("key");
        assertNotNull(child);

        assertEquals(List.of(), signal.peek().listChildren());

        signal.adoptAt(child, ListPosition.last());

        assertEquals(List.of(child), signal.peek().listChildren());
        assertEquals(Map.of(), signal.peek().mapChildren());
    }

    @Test
    void adoptAs_existingChild_keyChanged() {
        SharedNodeSignal signal = new SharedNodeSignal();
        signal.putChildIfAbsent("key");
        SharedNodeSignal child = signal.peek().mapChildren().get("key");
        assertNotNull(child);

        SignalOperation<Void> operation = signal.adoptAs(child, "update");

        assertSuccess(operation);
        assertEquals(Map.of("update", child), signal.peek().mapChildren());
        assertEquals(signal, child.peek().parent());
    }

    @Test
    void adoptAs_nestedStructure_hierarchyChanged() {
        SharedNodeSignal signal = new SharedNodeSignal();
        signal.putChildIfAbsent("parent");
        SharedNodeSignal parent = signal.peek().mapChildren().get("parent");
        assertNotNull(parent);
        parent.putChildIfAbsent("child");
        SharedNodeSignal child = parent.peek().mapChildren().get("child");
        assertNotNull(child);

        assertEquals(parent, child.peek().parent());

        SignalOperation<Void> operation = signal.adoptAs(child, "child");

        assertSuccess(operation);
        assertEquals(signal, child.peek().parent());
        assertEquals(Map.of("parent", parent, "child", child),
                signal.peek().mapChildren());
        assertEquals(Map.of(), parent.peek().mapChildren());
    }

    @Test
    void adoptAs_addParentToChild_rejected() {
        SharedNodeSignal signal = new SharedNodeSignal();
        signal.putChildIfAbsent("parent");
        SharedNodeSignal parent = signal.peek().mapChildren().get("parent");
        assertNotNull(parent);
        parent.putChildIfAbsent("child");
        SharedNodeSignal child = parent.peek().mapChildren().get("child");
        assertNotNull(child);

        SignalOperation<Void> operation = child.adoptAs(parent, "child");

        assertFailure(operation);
    }

    @Test
    void adoptAs_adoptListChild_noLongerListChild() {
        SharedNodeSignal signal = new SharedNodeSignal();
        SharedNodeSignal child = signal.insertChild(ListPosition.last())
                .signal();

        assertEquals(Map.of(), signal.peek().mapChildren());

        SignalOperation<Void> operation = signal.adoptAs(child, "key");

        assertSuccess(operation);
        assertEquals(List.of(), signal.peek().listChildren());
        assertEquals(Map.of("key", child), signal.peek().mapChildren());
    }

    @Test
    void removeChildByNode_listAndMapNodes_nodesRemoved() {
        SharedNodeSignal signal = new SharedNodeSignal();
        SharedNodeSignal listChild = signal.insertChild(ListPosition.last())
                .signal();
        signal.putChildIfAbsent("key");
        SharedNodeSignal mapChild = signal.peek().mapChildren().get("key");
        assertNotNull(mapChild);

        SignalOperation<Void> mapRemoveOp = signal.removeChild(mapChild);
        assertSuccess(mapRemoveOp);

        assertNull(mapChild.peek());
        assertEquals(Map.of(), signal.peek().mapChildren());

        SignalOperation<Void> listRemoveOp = signal.removeChild(listChild);
        assertSuccess(listRemoveOp);

        assertNull(listChild.peek());
        assertEquals(List.of(), signal.peek().listChildren());
    }

    @Test
    void removeChildByString_mapNode_nodeRemoved() {
        SharedNodeSignal signal = new SharedNodeSignal();
        signal.putChildIfAbsent("key");
        SharedNodeSignal child = signal.peek().mapChildren().get("key");
        assertNotNull(child);

        SignalOperation<Void> mapRemoveOp = signal.removeChild("key");
        assertSuccess(mapRemoveOp);

        assertNull(child.peek());
        assertEquals(Map.of(), signal.peek().mapChildren());
    }

    @Test
    void clear_listAndMapNodes_nodesRemoved() {
        SharedNodeSignal signal = new SharedNodeSignal();
        SharedNodeSignal listChild = signal.insertChild(ListPosition.last())
                .signal();
        signal.putChildIfAbsent("key");
        SharedNodeSignal mapChild = signal.peek().mapChildren().get("key");
        assertNotNull(mapChild);

        SignalOperation<Void> operation = signal.clear();
        assertSuccess(operation);

        assertNull(mapChild.peek());
        assertNull(listChild.peek());
        assertEquals(Map.of(), signal.peek().mapChildren());
        assertEquals(List.of(), signal.peek().listChildren());
    }

    @Test
    void value_modifyStateInstance_isImmutable() {
        SharedNodeSignal signal = new SharedNodeSignal();
        SharedNodeSignalState value = signal.peek();

        assertThrows(UnsupportedOperationException.class, () -> {
            value.listChildren().clear();
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            value.mapChildren().clear();
        });
    }

    @Test
    void value_readStateAfterModifications_seesOldState() {
        SharedNodeSignal signal = new SharedNodeSignal();
        SharedNodeSignalState value = signal.peek();

        signal.asValue(String.class).set("value");
        signal.insertChild(ListPosition.last());
        signal.putChildIfAbsent("key");

        assertEquals(null, value.value(String.class));
        assertEquals(List.of(), value.listChildren());
        assertEquals(Map.of(), value.mapChildren());
    }

    @Test
    void withValidator_spyingValidator_seesParentAndChildOperations() {
        SharedNodeSignal signal = new SharedNodeSignal();
        List<SignalCommand> validatedCommands = new ArrayList<>();

        SharedNodeSignal wrapper = signal.withValidator(command -> {
            validatedCommands.add(command);
            return true;
        });

        SharedNodeSignal child = wrapper
                .insertChildWithValue("child", ListPosition.last()).signal();

        assertEquals(1, validatedCommands.size());
        assertInstanceOf(SignalCommand.InsertCommand.class,
                validatedCommands.get(0));

        child.asValue(String.class).set("update");
        assertEquals(2, validatedCommands.size());
        assertInstanceOf(SignalCommand.ValueCommand.class,
                validatedCommands.get(1));
    }

    @Test
    void readonly_makeChangesToRoottAndChild_allChangesRejected() {
        SharedNodeSignal signal = new SharedNodeSignal();
        signal.insertChildWithValue("child", ListPosition.last()).signal();

        SharedNodeSignal readonly = signal.asReadonly();
        SharedNodeSignal readonlyChild = readonly.peek().listChildren().get(0);

        assertThrows(UnsupportedOperationException.class, () -> {
            readonly.clear();
        });
        assertEquals(List.of(readonlyChild), readonly.peek().listChildren());

        assertThrows(UnsupportedOperationException.class, () -> {
            readonlyChild.asValue(String.class).set("update");
        });
        assertEquals("child", readonlyChild.peek().value(String.class));
    }

    @Test
    void usageTracking_changeDifferentValues_anyChangeDetected() {
        SharedNodeSignal signal = new SharedNodeSignal();

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        signal.asValue(String.class).set("value");
        assertTrue(usage.hasChanges());

        usage = UsageTracker.track(() -> {
            signal.get();
        });
        signal.insertChildWithValue("insert", ListPosition.last());
        assertTrue(usage.hasChanges());

        usage = UsageTracker.track(() -> {
            signal.get();
        });
        signal.putChildWithValue("key", "value");
        assertTrue(usage.hasChanges());
    }

    @Test
    void equalsHashCode() {
        SharedNodeSignal signal = new SharedNodeSignal();
        assertEquals(signal, signal);

        SharedNodeSignal copy = new SharedNodeSignal(signal.tree(), signal.id(),
                signal.validator());
        assertEquals(signal, copy);
        assertEquals(signal.hashCode(), copy.hashCode());

        assertEquals(signal, signal.asValue(String.class).asNode());

        assertNotEquals(signal, new SharedNodeSignal());
        assertNotEquals(signal, signal.asReadonly());
        assertNotEquals(signal, signal.asList(Double.class));
    }

    @Test
    void equalsHashCode_listChildren() {
        SharedNodeSignal signal = new SharedNodeSignal();

        SharedNodeSignal operationChild = signal
                .insertChild(ListPosition.last()).signal();
        SharedNodeSignal other = signal.insertChild(ListPosition.last())
                .signal();

        SharedNodeSignal valueChild = signal.peek().listChildren().get(0);

        assertEquals(operationChild, valueChild);
        assertEquals(operationChild.hashCode(), valueChild.hashCode());

        assertNotEquals(operationChild, other);
    }

    @Test
    void equalsHashCode_mapChildren() {
        SharedNodeSignal signal = new SharedNodeSignal();

        signal.putChildIfAbsent("child");
        signal.putChildIfAbsent("other");

        SharedNodeSignal child = signal.peek().mapChildren().get("child");
        assertNotNull(child);
        SharedNodeSignal other = signal.peek().mapChildren().get("other");
        assertNotNull(other);

        SharedNodeSignal valueChild = signal.peek().mapChildren().get("child");
        assertNotNull(valueChild);

        assertEquals(child, valueChild);
        assertEquals(child.hashCode(), valueChild.hashCode());

        assertNotEquals(child, other);
    }

    @Test
    void toString_includesValue() {
        SharedNodeSignal signal = new SharedNodeSignal();

        assertEquals("SharedNodeSignal[]", signal.toString());

        signal.asValue(String.class).set("value");
        assertEquals("SharedNodeSignal[value: \"value\"]", signal.toString());

        signal.insertChildWithValue("listChild", ListPosition.last());
        assertEquals("SharedNodeSignal[value: \"value\","
                + " listChildren: [SharedNodeSignal[value: \"listChild\"]]]",
                signal.toString());

        signal.putChildWithValue("key", "mapChild");
        assertEquals("SharedNodeSignal[value: \"value\","
                + " listChildren: [SharedNodeSignal[value: \"listChild\"]],"
                + " mapChildren: [key = SharedNodeSignal[value: \"mapChild\"]]]",
                signal.toString());
    }
}
