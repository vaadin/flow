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

import static com.vaadin.signals.TestUtil.assertFailure;
import static com.vaadin.signals.TestUtil.assertSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import com.vaadin.signals.ListSignal.ListPosition;
import com.vaadin.signals.NodeSignal.NodeSignalState;
import com.vaadin.signals.impl.UsageTracker;
import com.vaadin.signals.impl.UsageTracker.Usage;
import com.vaadin.signals.operations.InsertOperation;
import com.vaadin.signals.operations.SignalOperation;

public class NodeSignalTest extends SignalTestBase {

    @Test
    void constructor_initialValue_isEmpty() {
        NodeSignal signal = new NodeSignal();

        NodeSignalState value = signal.value();

        assertNull(value.value(String.class));
        assertNull(value.parent());
        assertEquals(0, value.listChildren().size());
        assertEquals(0, value.mapChildren().size());
    }

    @Test
    void insertValue_emptyNode_getsChildNodeWithValue() {
        NodeSignal signal = new NodeSignal();

        InsertOperation<NodeSignal> operation = signal
                .insertChildWithValue("value", ListPosition.last());

        assertSuccess(operation);

        List<NodeSignal> listChildren = signal.value().listChildren();
        assertEquals(1, listChildren.size());
        assertEquals(operation.signal().id(), listChildren.get(0).id());
        assertEquals("value", operation.signal().value().value(String.class));
    }

    @Test
    void asValue_updateValueThroughWrapper_valueUpdated() {
        NodeSignal signal = new NodeSignal();

        ValueSignal<String> asValue = signal.asValue(String.class);
        assertEquals(null, asValue.value());

        asValue.value("update");
        assertEquals("update", signal.value().value(String.class));
    }

    @Test
    void asValue_incompatibleValue_getterThrows() {
        NodeSignal signal = new NodeSignal();

        ValueSignal<String> asString = signal.asValue(String.class);
        asString.value("update");

        assertThrows(RuntimeException.class, () -> {
            signal.value().value(Double.class);
        });

        ValueSignal<Double> asDouble = signal.asValue(Double.class);
        assertThrows(RuntimeException.class, () -> {
            asDouble.value();
        });
    }

    @Test
    void asNumber_nullValue_readsAndIncrementsAsZero() {
        NodeSignal signal = new NodeSignal();

        NumberSignal asNumber = signal.asNumber();
        assertEquals(0, asNumber.value());

        asNumber.incrementBy(1);
        assertEquals(1, asNumber.value());

        assertEquals(1, signal.value().value(Double.class));
    }

    @Test
    void asList_insertThroughWrapper_valueUpdated() {
        NodeSignal signal = new NodeSignal();
        ListSignal<String> asList = signal.asList(String.class);

        asList.insertLast("last");

        List<NodeSignal> listChildren = signal.value().listChildren();
        assertEquals(1, listChildren.size());

        assertEquals("last", listChildren.get(0).value().value(String.class));
    }

    @Test
    void asList_insertThroughNode_wrapperUpdated() {
        NodeSignal signal = new NodeSignal();
        ListSignal<String> asList = signal.asList(String.class);

        signal.insertChildWithValue("last", ListPosition.last());

        List<ValueSignal<String>> value = asList.value();
        assertEquals(1, value.size());

        assertEquals("last", value.get(0).value());
    }

    @Test
    void asMap_putThroughWrapper_valueUpdate() {
        NodeSignal signal = new NodeSignal();
        MapSignal<String> asMap = signal.asMap(String.class);

        asMap.put("key", "value");

        Map<String, NodeSignal> mapChildren = signal.value().mapChildren();
        assertEquals(Set.of("key"), mapChildren.keySet());

        assertEquals("value",
                mapChildren.get("key").value().value(String.class));
    }

    @Test
    void asMap_putThroughNode_wrapperUpdated() {
        NodeSignal signal = new NodeSignal();
        MapSignal<String> asMap = signal.asMap(String.class);

        signal.putChildWithValue("key", "value");

        Map<String, ValueSignal<String>> value = asMap.value();

        assertEquals(Set.of("key"), value.keySet());
        assertEquals("value", value.get("key").value());
    }

    /*
     * Several operations are already covered by the tests above so not going
     * through those separately.
     */

    @Test
    void insertChild_emptyNode_insertsEmptyListChild() {
        NodeSignal signal = new NodeSignal();

        InsertOperation<NodeSignal> operation = signal
                .insertChild(ListPosition.last());
        NodeSignal child = operation.signal();

        assertEquals(List.of(child), signal.value().listChildren());
        assertEquals(null, child.value().value(String.class));
    }

    @Test
    void putChildIfAbsent_emptyNode_insertsEmptyMapChild() {
        NodeSignal signal = new NodeSignal();

        InsertOperation<NodeSignal> operation = signal.putChildIfAbsent("key");
        NodeSignal child = operation.signal();
        assertEquals(Map.of("key", child), signal.value().mapChildren());
        assertEquals(null, child.value().value(String.class));
    }

    @Test
    void adpotAt_existingChild_orderChanged() {
        NodeSignal signal = new NodeSignal();

        NodeSignal a = signal.insertChild(ListPosition.last()).signal();
        NodeSignal b = signal.insertChild(ListPosition.last()).signal();

        SignalOperation<Void> operation = signal.adoptAt(a,
                ListPosition.last());

        assertSuccess(operation);
        assertEquals(List.of(b, a), signal.value().listChildren());
        assertEquals(signal, a.value().parent());
    }

    @Test
    void adoptAt_nestedStructure_hierarchyChagned() {
        NodeSignal signal = new NodeSignal();

        NodeSignal parent = signal.insertChild(ListPosition.last()).signal();
        NodeSignal child = parent.insertChild(ListPosition.last()).signal();

        assertEquals(parent, child.value().parent());

        SignalOperation<Void> operation = signal.adoptAt(child,
                ListPosition.first());

        assertSuccess(operation);
        assertEquals(signal, child.value().parent());
        assertEquals(List.of(child, parent), signal.value().listChildren());
        assertEquals(List.of(), parent.value().listChildren());
    }

    @Test
    void adoptAt_addParentToChild_rejected() {
        NodeSignal signal = new NodeSignal();

        NodeSignal parent = signal.insertChild(ListPosition.last()).signal();
        NodeSignal child = parent.insertChild(ListPosition.last()).signal();

        SignalOperation<Void> operation = child.adoptAt(parent,
                ListPosition.first());

        assertFailure(operation);
    }

    @Test
    void adoptAt_adoptMapChild_noLongerMapChild() {
        NodeSignal signal = new NodeSignal();
        NodeSignal child = signal.putChildIfAbsent("key").signal();

        assertEquals(List.of(), signal.value().listChildren());

        signal.adoptAt(child, ListPosition.last());

        assertEquals(List.of(child), signal.value().listChildren());
        assertEquals(Map.of(), signal.value().mapChildren());
    }

    @Test
    void adoptAs_existingChild_keyChanged() {
        NodeSignal signal = new NodeSignal();
        NodeSignal child = signal.putChildIfAbsent("key").signal();

        SignalOperation<Void> operation = signal.adoptAs(child, "update");

        assertSuccess(operation);
        assertEquals(Map.of("update", child), signal.value().mapChildren());
        assertEquals(signal, child.value().parent());
    }

    @Test
    void adoptAs_nestedStructure_hierarchyChanged() {
        NodeSignal signal = new NodeSignal();
        NodeSignal parent = signal.putChildIfAbsent("parent").signal();
        NodeSignal child = parent.putChildIfAbsent("child").signal();

        assertEquals(parent, child.value().parent());

        SignalOperation<Void> operation = signal.adoptAs(child, "child");

        assertSuccess(operation);
        assertEquals(signal, child.value().parent());
        assertEquals(Map.of("parent", parent, "child", child),
                signal.value().mapChildren());
        assertEquals(Map.of(), parent.value().mapChildren());
    }

    @Test
    void adoptAs_addParentToChild_rejected() {
        NodeSignal signal = new NodeSignal();
        NodeSignal parent = signal.putChildIfAbsent("parent").signal();
        NodeSignal child = parent.putChildIfAbsent("child").signal();

        SignalOperation<Void> operation = child.adoptAs(parent, "child");

        assertFailure(operation);
    }

    @Test
    void adoptAs_adoptListChild_noLongerListChild() {
        NodeSignal signal = new NodeSignal();
        NodeSignal child = signal.insertChild(ListPosition.last()).signal();

        assertEquals(Map.of(), signal.value().mapChildren());

        SignalOperation<Void> operation = signal.adoptAs(child, "key");

        assertSuccess(operation);
        assertEquals(List.of(), signal.value().listChildren());
        assertEquals(Map.of("key", child), signal.value().mapChildren());
    }

    @Test
    void removeChildByNode_listAndMapNodes_nodesRemoved() {
        NodeSignal signal = new NodeSignal();
        NodeSignal listChild = signal.insertChild(ListPosition.last()).signal();
        NodeSignal mapChild = signal.putChildIfAbsent("key").signal();

        SignalOperation<Void> mapRemoveOp = signal.removeChild(mapChild);
        assertSuccess(mapRemoveOp);

        assertNull(mapChild.value());
        assertEquals(Map.of(), signal.value().mapChildren());

        SignalOperation<Void> listRemoveOp = signal.removeChild(listChild);
        assertSuccess(listRemoveOp);

        assertNull(listChild.value());
        assertEquals(List.of(), signal.value().listChildren());
    }

    @Test
    void removeChildByString_mapNode_nodeRemoved() {
        NodeSignal signal = new NodeSignal();
        NodeSignal child = signal.putChildIfAbsent("key").signal();

        SignalOperation<Void> mapRemoveOp = signal.removeChild("key");
        assertSuccess(mapRemoveOp);

        assertNull(child.value());
        assertEquals(Map.of(), signal.value().mapChildren());
    }

    @Test
    void clear_listAndMapNodes_nodesRemoved() {
        NodeSignal signal = new NodeSignal();
        NodeSignal listChild = signal.insertChild(ListPosition.last()).signal();
        NodeSignal mapChild = signal.putChildIfAbsent("key").signal();

        SignalOperation<Void> operation = signal.clear();
        assertSuccess(operation);

        assertNull(mapChild.value());
        assertNull(listChild.value());
        assertEquals(Map.of(), signal.value().mapChildren());
        assertEquals(List.of(), signal.value().listChildren());
    }

    @Test
    void value_modifyStateInstance_isImmutable() {
        NodeSignal signal = new NodeSignal();
        NodeSignalState value = signal.value();

        assertThrows(UnsupportedOperationException.class, () -> {
            value.listChildren().clear();
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            value.mapChildren().clear();
        });
    }

    @Test
    void value_readStateAfterModifications_seesOldState() {
        NodeSignal signal = new NodeSignal();
        NodeSignalState value = signal.value();

        signal.asValue(String.class).value("value");
        signal.insertChild(ListPosition.last());
        signal.putChildIfAbsent("key");

        assertEquals(null, value.value(String.class));
        assertEquals(List.of(), value.listChildren());
        assertEquals(Map.of(), value.mapChildren());
    }

    @Test
    void withValidator_spyingValidator_seesParentAndChildOperations() {
        NodeSignal signal = new NodeSignal();
        List<SignalCommand> validatedCommands = new ArrayList<>();

        NodeSignal wrapper = signal.withValidator(command -> {
            validatedCommands.add(command);
            return true;
        });

        NodeSignal child = wrapper
                .insertChildWithValue("child", ListPosition.last()).signal();

        assertEquals(1, validatedCommands.size());
        assertInstanceOf(SignalCommand.InsertCommand.class,
                validatedCommands.get(0));

        child.asValue(String.class).value("update");
        assertEquals(2, validatedCommands.size());
        assertInstanceOf(SignalCommand.ValueCommand.class,
                validatedCommands.get(1));
    }

    @Test
    void readonly_makeChangesToRoottAndChild_allChangesRejected() {
        NodeSignal signal = new NodeSignal();
        signal.insertChildWithValue("child", ListPosition.last()).signal();

        NodeSignal readonly = signal.asReadonly();
        NodeSignal readonlyChild = readonly.value().listChildren().get(0);

        assertThrows(UnsupportedOperationException.class, () -> {
            readonly.clear();
        });
        assertEquals(List.of(readonlyChild), readonly.value().listChildren());

        assertThrows(UnsupportedOperationException.class, () -> {
            readonlyChild.asValue(String.class).value("update");
        });
        assertEquals("child", readonlyChild.value().value(String.class));
    }

    @Test
    void usageTracking_changeDifferentValues_anyChangeDetected() {
        NodeSignal signal = new NodeSignal();

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        signal.asValue(String.class).value("value");
        assertTrue(usage.hasChanges());

        usage = UsageTracker.track(() -> {
            signal.value();
        });
        signal.insertChildWithValue("insert", ListPosition.last());
        assertTrue(usage.hasChanges());

        usage = UsageTracker.track(() -> {
            signal.value();
        });
        signal.putChildWithValue("key", "value");
        assertTrue(usage.hasChanges());
    }

    @Test
    void equalsHashCode() {
        NodeSignal signal = new NodeSignal();
        assertEquals(signal, signal);

        NodeSignal copy = new NodeSignal(signal.tree(), signal.id(),
                signal.validator());
        assertEquals(signal, copy);
        assertEquals(signal.hashCode(), copy.hashCode());

        assertEquals(signal, signal.asValue(String.class).asNode());

        assertNotEquals(signal, new NodeSignal());
        assertNotEquals(signal, signal.asReadonly());
        assertNotEquals(signal, signal.asList(Double.class));
    }

    @Test
    void equalsHashCode_listChildren() {
        NodeSignal signal = new NodeSignal();

        NodeSignal operationChild = signal.insertChild(ListPosition.last())
                .signal();
        NodeSignal other = signal.insertChild(ListPosition.last()).signal();

        NodeSignal valueChild = signal.value().listChildren().get(0);

        assertEquals(operationChild, valueChild);
        assertEquals(operationChild.hashCode(), valueChild.hashCode());

        assertNotEquals(operationChild, other);
    }

    @Test
    void equalsHashCode_mapChildren() {
        NodeSignal signal = new NodeSignal();

        NodeSignal operationChild = signal.putChildIfAbsent("child").signal();
        NodeSignal other = signal.putChildIfAbsent("other").signal();

        NodeSignal valueChild = signal.value().mapChildren().get("child");

        assertEquals(operationChild, valueChild);
        assertEquals(operationChild.hashCode(), valueChild.hashCode());

        assertNotEquals(operationChild, other);
    }

    @Test
    void toString_includesValue() {
        NodeSignal signal = new NodeSignal();

        assertEquals("NodeSignal[]", signal.toString());

        signal.asValue(String.class).value("value");
        assertEquals("NodeSignal[value: \"value\"]", signal.toString());

        signal.insertChildWithValue("listChild", ListPosition.last());
        assertEquals(
                "NodeSignal[value: \"value\","
                        + " listChildren: [NodeSignal[value: \"listChild\"]]]",
                signal.toString());

        signal.putChildWithValue("key", "mapChild");
        assertEquals("NodeSignal[value: \"value\","
                + " listChildren: [NodeSignal[value: \"listChild\"]],"
                + " mapChildren: [key = NodeSignal[value: \"mapChild\"]]]",
                signal.toString());
    }
}
