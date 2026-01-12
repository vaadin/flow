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

import org.junit.jupiter.api.Test;

import com.vaadin.signals.NodeSignal.NodeSignalState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SignalFactoryTest extends SignalTestBase {

    @Test
    void shared_twoInstancesSameName_instancesShared() {
        NodeSignal a = SignalFactory.IN_MEMORY_SHARED.node("name");
        NodeSignal b = SignalFactory.IN_MEMORY_SHARED.node("name");

        assertEquals(a, b);
    }

    @Test
    void shared_twoInstancesDifferentName_separateInstances() {
        NodeSignal a = SignalFactory.IN_MEMORY_SHARED.node("a");
        NodeSignal b = SignalFactory.IN_MEMORY_SHARED.node("b");

        assertNotEquals(a, b);
    }

    @Test
    void shared_removeName_separateInstances() {
        NodeSignal a = SignalFactory.IN_MEMORY_SHARED.node("name");
        SignalFactory.IN_MEMORY_SHARED.remove("name");
        NodeSignal b = SignalFactory.IN_MEMORY_SHARED.node("name");

        assertNotEquals(a, b);
    }

    @Test
    void shared_clear_separateInstances() {
        NodeSignal a = SignalFactory.IN_MEMORY_SHARED.node("name");
        SignalFactory.IN_MEMORY_SHARED.clear();
        NodeSignal b = SignalFactory.IN_MEMORY_SHARED.node("name");

        assertNotEquals(a, b);
    }

    @Test
    void exclusive_twoInstancesSameName_separateInstances() {
        NodeSignal a = SignalFactory.IN_MEMORY_EXCLUSIVE.node("name");
        NodeSignal b = SignalFactory.IN_MEMORY_EXCLUSIVE.node("name");

        assertNotEquals(a, b);
    }

    @Test
    void value_newSignal_isValueSignal() {
        ValueSignal<String> signal = SignalFactory.IN_MEMORY_SHARED
                .value("name", String.class);

        signal.value("value");
        assertEquals("value", signal.value());
    }

    @Test
    void value_newWithDefaultValue_usesDefaultValue() {
        ValueSignal<String> signal = SignalFactory.IN_MEMORY_SHARED
                .value("name", String.class, "value");

        assertEquals("value", signal.value());
    }

    @Test
    void value_existingWithDefaultValue_usesOldValue() {
        ValueSignal<String> first = SignalFactory.IN_MEMORY_SHARED.value("name",
                String.class);
        first.value("value");

        ValueSignal<String> second = SignalFactory.IN_MEMORY_SHARED
                .value("name", "second");

        assertEquals("value", second.value());
    }

    @Test
    void value_typeBasedOnDefaultValue_usesRightType() {
        ValueSignal<String> signal = SignalFactory.IN_MEMORY_SHARED
                .value("name", "value");

        assertEquals("value", signal.value());
    }

    @Test
    void number_newSignal_isNumberSignal() {
        NumberSignal signal = SignalFactory.IN_MEMORY_SHARED.number("name");
        signal.incrementBy(1);

        assertEquals(1, signal.value());
    }

    @Test
    void number_newWithDefaultValue_usesDefaultValue() {
        NumberSignal signal = SignalFactory.IN_MEMORY_SHARED.number("name",
                3.14);

        assertEquals(3.14, signal.value());
    }

    @Test
    void number_existingWithDefaultValue_usesOldValue() {
        SignalFactory.IN_MEMORY_SHARED.number("name", 3.14);

        NumberSignal signal = SignalFactory.IN_MEMORY_SHARED.number("name", 42);

        assertEquals(3.14, signal.value());

        assertEquals(3.14, signal.value());
    }

    @Test
    void list_newSignal_isListSignalWithRightType() {
        ListSignal<String> list = SignalFactory.IN_MEMORY_SHARED.list("name",
                String.class);
        list.insertLast("value");

        assertEquals("value", list.value().get(0).value());
    }

    @Test
    void map_newSignal_isListSignalWithRightType() {
        MapSignal<String> map = SignalFactory.IN_MEMORY_SHARED.map("name",
                String.class);
        map.put("key", "value");

        assertEquals("value", map.value().get("key").value());
    }

    @Test
    void shared_sameNameDifferentTypes_dataIsShared() {
        NodeSignal node = SignalFactory.IN_MEMORY_SHARED.node("name");
        ValueSignal<Double> value = SignalFactory.IN_MEMORY_SHARED.value("name",
                Double.class);
        NumberSignal number = SignalFactory.IN_MEMORY_SHARED.number("name");
        ListSignal<String> list = SignalFactory.IN_MEMORY_SHARED.list("name",
                String.class);
        MapSignal<String> map = SignalFactory.IN_MEMORY_SHARED.map("name",
                String.class);

        value.value(Double.valueOf(2));
        number.incrementBy(1);
        list.insertLast("list");
        map.put("key", "map");

        NodeSignalState state = node.value();
        assertEquals(3, state.value(Double.class));
        assertEquals("list",
                state.listChildren().get(0).value().value(String.class));
        assertEquals("map",
                state.mapChildren().get("key").value().value(String.class));
    }
}
