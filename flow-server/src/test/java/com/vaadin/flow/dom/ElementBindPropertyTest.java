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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JacksonUtilsTest;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.ElementListenersTest;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.PropertyChangeDeniedException;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ElementBindPropertyTest {

    private static MockVaadinServletService service;

    private LinkedList<ErrorEvent> events;

    @BeforeAll
    public static void init() {
        service = new MockVaadinServletService();
    }

    @AfterAll
    public static void clean() {
        CurrentInstance.clearAll();
        service.destroy();
    }

    @BeforeEach
    public void before() {
        events = mockLockedSessionWithErrorHandler();
    }

    @AfterEach
    public void after() {
        CurrentInstance.clearAll();
        events = null;
    }

    // common property signal binding tests

    @Test
    public void bindProperty_nullProperty_throwException() {
        Element element = new Element("foo");
        ValueSignal<String> signal = new ValueSignal<>("bar");
        assertThrows(IllegalArgumentException.class,
                () -> element.bindProperty(null, signal, signal::set));
    }

    @Test
    public void bindProperty_illegalProperty_throwException() {
        Element element = new Element("foo");
        ValueSignal<String> signal = new ValueSignal<>("bar");
        assertThrows(IllegalArgumentException.class,
                () -> element.bindProperty("textContent", signal, signal::set));
        assertThrows(IllegalArgumentException.class,
                () -> element.bindProperty("classList", signal, signal::set));
        assertThrows(IllegalArgumentException.class,
                () -> element.bindProperty("className", signal, signal::set));
        assertThrows(IllegalArgumentException.class,
                () -> element.bindProperty("outerHTML", signal, signal::set));
    }

    @Test
    public void bindProperty_notComponent_doNotThrowException() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        element.bindProperty("foobar", signal, signal::set);
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindProperty_setPropertyWhileBindingIsActive_throwException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindProperty("foo", signal, signal::set);

        assertThrows(BindingActiveException.class,
                () -> component.getElement().setProperty("foo", "baz"));
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindProperty_nullSignal_throwsNPE() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        assertThrows(NullPointerException.class,
                () -> component.getElement().bindProperty("foo", null, null));
    }

    @Test
    public void bindProperty_removePropertyWhileBindingIsActive_throwException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindProperty("foo", signal, signal::set);

        assertThrows(BindingActiveException.class,
                () -> component.getElement().removeProperty("foo"));
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindPropertyComputedSignal_getPropertyValue_returnsCorrectValue() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");
        Signal<String> computedSignal = Signal
                .computed(() -> "computed-" + signal.get());
        component.getElement().bindProperty("foo", computedSignal, null);

        assertEquals("computed-bar",
                component.getElement().getProperty("foo", "default"));
    }

    @Test
    public void bindPropertyMappedSignal_getPropertyValue_returnsCorrectValue() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");
        Signal<String> mappedSignal = signal.map(text -> "mapped-" + text);
        component.getElement().bindProperty("foo", mappedSignal, null);

        assertEquals("mapped-bar",
                component.getElement().getProperty("foo", "default"));
    }

    @Test
    public void bindPropertyJacksonNull_getPropertyValue_returnsCorrectValue() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        Signal<?> computedSignal = Signal.computed(() -> {
            // bypass signal usage requirement
            new ValueSignal<>().get();
            return null;
        });
        component.getElement().bindProperty("foo", computedSignal, null);
        assertEquals(JacksonUtils.nullNode(),
                component.getElement().getPropertyRaw("foo"));
        assertEquals(null, component.getElement().getProperty("foo"));
    }

    @Test
    public void bindPropertyJacksonObjectNode_getPropertyValue_returnsCorrectValue() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        Signal<?> computedSignal = Signal.computed(() -> {
            // bypass signal usage requirement
            new ValueSignal<>().get();
            return JacksonUtils.createObjectNode();
        });
        component.getElement().bindProperty("bar", computedSignal, null);
        assertEquals(JacksonUtils.createObjectNode(),
                component.getElement().getPropertyRaw("bar"));
        assertEquals("{}", component.getElement().getProperty("bar"));
    }

    @Test
    public void bindProperty_addPropertyChangeListenerAttached_listenerReceivesValueChange() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");
        component.getElement().bindProperty("foo", signal, signal::set);

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();
        component.getElement().addPropertyChangeListener("foo", "event",
                event -> listenerValue.set(event.getValue()));

        assertEquals(DisabledUpdateMode.ONLY_WHEN_ENABLED,
                component.getElement().getNode()
                        .getFeature(ElementListenerMap.class)
                        .getPropertySynchronizationMode("foo"),
                "The property should be synchronized");

        ElementListenerMap listenerMap = component.getElement().getNode()
                .getFeature(ElementListenerMap.class);

        assertEquals(
                Collections.singleton(
                        JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN + "foo"),
                ElementListenersTest.getExpressions(listenerMap, "event"),
                "A DOM event synchronization should be defined");

        signal.set("changedValue");
        assertEquals("changedValue", listenerValue.get());
    }

    @Test
    public void bindProperty_addPropertyChangeListenerDetached_listenerReceivesValueChange() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");
        component.getElement().bindProperty("foo", signal, signal::set);

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();
        component.getElement().addPropertyChangeListener("foo", "event",
                event -> listenerValue.set(event.getValue()));

        signal.set("changedValue");
        assertEquals("changedValue", listenerValue.get());

        // When detached, signal change should not propagate to the property and
        // the listener should not be triggered
        component.removeFromParent();
        signal.set("secondChangedValue");
        assertEquals("changedValue", listenerValue.get());
        assertEquals("changedValue", component.getElement().getProperty("foo"));
    }

    // boolean property signal binding tests

    @Test
    public void bindBooleanProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<Boolean> signal = new ValueSignal<>(true);

        component.getElement().bindProperty("foo", signal, signal::set);

        assertNull(component.getElement().getProperty("foo"));
    }

    @Test
    public void bindBooleanProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(true);

        component.getElement().bindProperty("foo", signal, signal::set);

        component.removeFromParent();

        signal.set(false);

        assertTrue(events.isEmpty());
        assertTrue(component.getElement().getProperty("foo", false));
    }

    @Test
    public void bindBooleanProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(true);

        component.getElement().bindProperty("foo", signal, signal::set);

        assertTrue(component.getElement().getProperty("foo", false));
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindBooleanProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(true);

        component.getElement().bindProperty("foo", signal, signal::set);
        assertTrue(component.getElement().getProperty("foo", false));

        component.removeFromParent();
        signal.set(false);

        assertEquals(false, signal.peek());
        assertTrue(component.getElement().getProperty("foo", false));

        UI.getCurrent().add(component);
        assertFalse(component.getElement().getProperty("foo", true));

        assertTrue(events.isEmpty());
    }

    // double property signal binding tests

    @Test
    public void bindDoubleProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<Double> signal = new ValueSignal<>(1.0d);

        component.getElement().bindProperty("foo", signal, signal::set);

        assertNull(component.getElement().getProperty("foo"));
    }

    @Test
    public void bindDoubleProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Double> signal = new ValueSignal<>(1.0d);

        component.getElement().bindProperty("foo", signal, signal::set);

        component.removeFromParent();

        signal.set(2.0d);

        assertTrue(events.isEmpty());
        assertEquals(1.0d, component.getElement().getProperty("foo", -1.0d),
                0.0d);
    }

    @Test
    public void bindDoubleProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Double> signal = new ValueSignal<>(1.0d);

        component.getElement().bindProperty("foo", signal, signal::set);

        assertEquals(1.0d, component.getElement().getProperty("foo", -1.0d),
                0.0d);
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindDoubleProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Double> signal = new ValueSignal<>(1.0d);

        component.getElement().bindProperty("foo", signal, signal::set);
        assertEquals(1.0d, component.getElement().getProperty("foo", -1.0d),
                0.0d);

        component.removeFromParent();
        signal.set(2.0d);

        assertEquals(2.0d, signal.peek(), 0.0d);
        assertEquals(1.0d, component.getElement().getProperty("foo", -1.0d),
                0.0d);

        UI.getCurrent().add(component);
        assertEquals(2.0d, component.getElement().getProperty("foo", -1.0d),
                0.0d);

        assertTrue(events.isEmpty());
    }

    // integer property signal binding tests

    @Test
    public void bindIntegerProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<Integer> signal = new ValueSignal<>(1);

        component.getElement().bindProperty("foo", signal, signal::set);

        assertNull(component.getElement().getProperty("foo"));
    }

    @Test
    public void bindIntegerProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Integer> signal = new ValueSignal<>(1);

        component.getElement().bindProperty("foo", signal, signal::set);

        component.removeFromParent();

        signal.set(2);

        assertTrue(events.isEmpty());
        assertEquals(1, component.getElement().getProperty("foo", -1));
    }

    @Test
    public void bindIntegerProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Integer> signal = new ValueSignal<>(1);

        component.getElement().bindProperty("foo", signal, signal::set);

        assertEquals(1, component.getElement().getProperty("foo", -1));
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindIntegerProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Integer> signal = new ValueSignal<>(1);

        component.getElement().bindProperty("foo", signal, signal::set);
        assertEquals(1, component.getElement().getProperty("foo", -1));

        component.removeFromParent();
        signal.set(2);

        assertEquals(2, (long) signal.peek());
        assertEquals(1, component.getElement().getProperty("foo", -1));

        UI.getCurrent().add(component);
        assertEquals(2, component.getElement().getProperty("foo", -1));

        assertTrue(events.isEmpty());
    }

    // string property signal binding tests

    @Test
    public void bindStringProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindProperty("foo", signal, signal::set);

        assertNull(component.getElement().getProperty("foo", null));
    }

    @Test
    public void bindStringProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindProperty("foo", signal, signal::set);

        component.removeFromParent();

        signal.set("baz");

        assertTrue(events.isEmpty());
        assertEquals("bar",
                component.getElement().getProperty("foo", "default"));
    }

    @Test
    public void bindStringProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindProperty("foo", signal, signal::set);

        assertEquals("bar",
                component.getElement().getProperty("foo", "default"));
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindStringProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindProperty("foo", signal, signal::set);
        assertEquals("bar",
                component.getElement().getProperty("foo", "default"));

        component.removeFromParent();
        signal.set("baz");

        assertEquals("baz", signal.peek());
        assertEquals("bar",
                component.getElement().getProperty("foo", "default"));

        UI.getCurrent().add(component);
        assertEquals("baz",
                component.getElement().getProperty("foo", "default"));

        assertTrue(events.isEmpty());
    }

    // bean property signal binding tests

    @Test
    public void bindBeanProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<JacksonUtilsTest.Person> signal = new ValueSignal<>(
                createJohn());
        component.getElement().bindProperty("foo", signal, signal::set);

        assertNull(component.getElement().getProperty("foo", null));
    }

    @Test
    public void bindBeanProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        JacksonUtilsTest.Person john = createJohn();
        ValueSignal<JacksonUtilsTest.Person> signal = new ValueSignal<>(john);
        component.getElement().bindProperty("foo", signal, signal::set);

        component.removeFromParent();

        signal.set(createPerson("Jack", 52, false));

        assertPersonEquals(john,
                (JsonNode) component.getElement().getPropertyRaw("foo"));
        assertTrue(events.isEmpty());

    }

    @Test
    public void bindBeanProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        JacksonUtilsTest.Person john = createJohn();
        ValueSignal<JacksonUtilsTest.Person> signal = new ValueSignal<>(john);
        component.getElement().bindProperty("foo", signal, signal::set);

        assertPersonEquals(john,
                (JsonNode) component.getElement().getPropertyRaw("foo"));
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindBeanProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        JacksonUtilsTest.Person john = createJohn();
        ValueSignal<JacksonUtilsTest.Person> signal = new ValueSignal<>(john);
        component.getElement().bindProperty("foo", signal, signal::set);

        assertPersonEquals(john,
                (JsonNode) component.getElement().getPropertyRaw("foo"));

        component.removeFromParent();
        JacksonUtilsTest.Person jack = createJack();
        signal.set(jack);

        assertEquals(jack, signal.peek());
        assertPersonEquals(john,
                (JsonNode) component.getElement().getPropertyRaw("foo"));

        UI.getCurrent().add(component);

        assertPersonEquals(jack,
                (JsonNode) component.getElement().getPropertyRaw("foo"));
        assertTrue(events.isEmpty());
    }

    // list property signal binding tests

    @Test
    public void bindListProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<List<JacksonUtilsTest.Person>> signal = new ValueSignal<>(
                Arrays.asList(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal, null);

        assertNull(component.getElement().getProperty("foo", null));
    }

    @Test
    public void bindListProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<JacksonUtilsTest.Person>> signal = new ValueSignal<>(
                Arrays.asList(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal, null);
        assertEquals("John",
                getFromList(component, "foo", 0).get("name").asString());
        assertEquals("Jack",
                getFromList(component, "foo", 1).get("name").asString());

        component.removeFromParent();

        signal.set(Arrays.asList(createJack(), createJohn()));
        assertEquals("John",
                getFromList(component, "foo", 0).get("name").asString());
        assertEquals("Jack",
                getFromList(component, "foo", 1).get("name").asString());

        assertTrue(events.isEmpty());
    }

    @Test
    public void bindListProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<JacksonUtilsTest.Person>> signal = new ValueSignal<>(
                Arrays.asList(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal, null);

        assertEquals("John",
                getFromList(component, "foo", 0).get("name").asString());
        assertEquals("Jack",
                getFromList(component, "foo", 1).get("name").asString());
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindListProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<JacksonUtilsTest.Person>> signal = new ValueSignal<>(
                Arrays.asList(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal, null);

        // assert initial value
        assertEquals("John",
                getFromList(component, "foo", 0).get("name").asString());
        assertEquals("Jack",
                getFromList(component, "foo", 1).get("name").asString());

        component.removeFromParent();
        signal.set(Arrays.asList(createJack(), createJohn()));

        // assert signal value updated
        assertEquals("Jack", (signal.peek().getFirst()).name());
        assertEquals("John", (signal.peek().getLast()).name());
        // assert property value not updated
        assertEquals("John",
                getFromList(component, "foo", 0).get("name").asString());
        assertEquals("Jack",
                getFromList(component, "foo", 1).get("name").asString());

        UI.getCurrent().add(component);

        // assert property value updated
        assertEquals("Jack",
                getFromList(component, "foo", 0).get("name").asString());
        assertEquals("John",
                getFromList(component, "foo", 1).get("name").asString());
        assertTrue(events.isEmpty());
    }

    // map property signal binding tests

    @Test
    public void bindMapProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<Map<String, JacksonUtilsTest.Person>> signal = new ValueSignal<>(
                createPersonMap(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal, null);

        assertNull(component.getElement().getProperty("foo", null));
    }

    @Test
    public void bindMapProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Map<String, JacksonUtilsTest.Person>> signal = new ValueSignal<>(
                createPersonMap(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal, null);
        assertEquals("John",
                getFromMap(component, "foo", "0").get("name").asString());
        assertEquals("Jack",
                getFromMap(component, "foo", "1").get("name").asString());

        component.removeFromParent();

        signal.set(createPersonMap(createJack(), createJohn()));
        assertEquals("John",
                getFromMap(component, "foo", "0").get("name").asString());
        assertEquals("Jack",
                getFromMap(component, "foo", "1").get("name").asString());

        assertTrue(events.isEmpty());
    }

    @Test
    public void bindMapProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Map<?, ?>> signal = new ValueSignal<>(
                createPersonMap(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal, null);

        assertEquals("John",
                getFromMap(component, "foo", "0").get("name").asString());
        assertEquals("Jack",
                getFromMap(component, "foo", "1").get("name").asString());
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindMapProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Map<String, JacksonUtilsTest.Person>> signal = new ValueSignal<>(
                createPersonMap(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal, null);

        // assert initial value
        assertEquals("John",
                getFromMap(component, "foo", "0").get("name").asString());
        assertEquals("Jack",
                getFromMap(component, "foo", "1").get("name").asString());

        component.removeFromParent();
        signal.set(createPersonMap(createJack(), createJohn()));

        // assert signal value updated
        assertEquals("Jack", (signal.peek().get("0")).name());
        assertEquals("John", (signal.peek().get("1")).name());
        // assert property value not updated
        assertEquals("John",
                getFromMap(component, "foo", "0").get("name").asString());
        assertEquals("Jack",
                getFromMap(component, "foo", "1").get("name").asString());

        UI.getCurrent().add(component);

        // assert property value updated
        assertEquals("Jack",
                getFromMap(component, "foo", "0").get("name").asString());
        assertEquals("John",
                getFromMap(component, "foo", "1").get("name").asString());
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindProperty_writeCallbackThrows() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        component.getElement().bindProperty("prop", signal, value -> {
            throw new RuntimeException("test");
        });
        assertEquals("foo", component.getElement().getProperty("prop"));

        component.getElement().addPropertyChangeListener("prop", "change",
                event -> {
                    fail("Property change listener should not be triggered when write callback throws");
                });

        assertThrows(RuntimeException.class,
                () -> emulateClientUpdate(component.getElement(), "prop",
                        "bar"));
    }

    @Test
    public void bindProperty_normalCallback_valueChangeEventTriggered() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        component.getElement().bindProperty("prop", signal, signal::set);

        AtomicReference<Serializable> eventValue = new AtomicReference<>();
        AtomicInteger counter = new AtomicInteger(0);
        component.getElement().addPropertyChangeListener("prop", "change",
                event -> {
                    eventValue.set(event.getValue());
                    counter.incrementAndGet();
                });

        emulateClientUpdate(component.getElement(), "prop", "bar");
        assertEquals("bar", eventValue.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void bindProperty_transformingCallback_valueChangeEventTriggered() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        component.getElement().bindProperty("prop", signal,
                v -> signal.set(v.toUpperCase()));

        AtomicReference<Serializable> eventValue = new AtomicReference<>();
        AtomicInteger counter = new AtomicInteger(0);
        component.getElement().addPropertyChangeListener("prop", "change",
                event -> {
                    eventValue.set(event.getValue());
                    counter.incrementAndGet();
                });

        emulateClientUpdate(component.getElement(), "prop", "bar");
        assertEquals("BAR", eventValue.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void bindProperty_noOpCallback_valueChangeEventNotTriggered() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        component.getElement().bindProperty("prop", signal, value -> {
        });

        component.getElement().addPropertyChangeListener("prop", "change",
                event -> {
                    fail("Property change listener should not be triggered with a no-op callback");
                });

        // With a no-op callback, value is not changed and event should not be
        // triggered
        emulateClientUpdate(component.getElement(), "prop", "bar");
    }

    private void emulateClientUpdate(Element element, String property,
            String value) {
        ElementPropertyMap childModel = ElementPropertyMap
                .getModel(element.getNode());
        try {
            childModel.deferredUpdateFromClient(property, value);
        } catch (PropertyChangeDeniedException e) {
            fail("Failed to update property from client: " + e.getMessage());
        }
    }

    private void assertPersonEquals(JacksonUtilsTest.Person person,
            JsonNode jsonNode) {
        assertEquals(person.name(), jsonNode.get("name").asString());
        assertEquals(person.age(), jsonNode.get("age").asDouble(), 0);
        assertEquals(person.canSwim(), jsonNode.get("canSwim").asBoolean());
    }

    private JacksonUtilsTest.Person createPerson(String name, double age,
            boolean canSwim) {
        return new JacksonUtilsTest.Person(name, age, canSwim);
    }

    private JacksonUtilsTest.Person createJohn() {
        return createPerson("John", 42, true);
    }

    private JacksonUtilsTest.Person createJack() {
        return createPerson("Jack", 52, false);
    }

    private Map<String, JacksonUtilsTest.Person> createPersonMap(
            JacksonUtilsTest.Person... persons) {
        Map<String, JacksonUtilsTest.Person> map = new HashMap<>();
        for (int i = 0; i < persons.length; i++) {
            map.put(String.valueOf(i), persons[i]);
        }
        return map;
    }

    private ObjectNode getFromList(Component component, String propertyName,
            int index) {
        ArrayNode arrayNode = (ArrayNode) component.getElement()
                .getPropertyRaw(propertyName);
        return (ObjectNode) arrayNode.get(index);
    }

    private ObjectNode getFromMap(Component component, String propertyName,
            String key) {
        ObjectNode objectNode = (ObjectNode) component.getElement()
                .getPropertyRaw(propertyName);
        return (ObjectNode) objectNode.get(key);
    }

    private LinkedList<ErrorEvent> mockLockedSessionWithErrorHandler() {
        VaadinService.setCurrent(service);

        var session = new MockVaadinSession(service);
        session.lock();

        var ui = new MockUI(session);
        var events = new LinkedList<ErrorEvent>();
        session.setErrorHandler(events::add);

        return events;
    }

    @Tag("div")
    private static class TestComponent extends Component {

    }
}
