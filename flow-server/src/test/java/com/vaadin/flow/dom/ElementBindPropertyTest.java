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
package com.vaadin.flow.dom;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.JacksonUtilsTest;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class ElementBindPropertyTest {

    private static MockVaadinServletService service;

    private MockedStatic<FeatureFlags> featureFlagStaticMock;

    private LinkedList<ErrorEvent> events;

    @BeforeClass
    public static void init() {
        var featureFlagStaticMock = mockStatic(FeatureFlags.class);
        featureFlagEnabled(featureFlagStaticMock);
        service = new MockVaadinServletService();
        close(featureFlagStaticMock);
    }

    @AfterClass
    public static void clean() {
        CurrentInstance.clearAll();
        service.destroy();
    }

    @Before
    public void before() {
        featureFlagStaticMock = mockStatic(FeatureFlags.class);
        featureFlagEnabled(featureFlagStaticMock);
        events = mockLockedSessionWithErrorHandler();
    }

    @After
    public void after() {
        close(featureFlagStaticMock);
        events = null;
    }

    private static void featureFlagEnabled(
            MockedStatic<FeatureFlags> featureFlagStaticMock) {
        FeatureFlags flags = mock(FeatureFlags.class);
        when(flags.isEnabled(FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId()))
                .thenReturn(true);
        featureFlagStaticMock.when(() -> FeatureFlags.get(any()))
                .thenReturn(flags);
    }

    private static void close(
            MockedStatic<FeatureFlags> featureFlagStaticMock) {
        CurrentInstance.clearAll();
        featureFlagStaticMock.close();
    }

    // common property signal binding tests

    @Test
    public void bindProperty_nullProperty_throwException() {
        Element element = new Element("foo");
        ValueSignal<String> signal = new ValueSignal<>("bar");
        assertThrows(IllegalArgumentException.class,
                () -> element.bindProperty(null, signal));
    }

    @Test
    public void bindProperty_illegalProperty_throwException() {
        Element element = new Element("foo");
        ValueSignal<String> signal = new ValueSignal<>("bar");
        assertThrows(IllegalArgumentException.class,
                () -> element.bindProperty("textContent", signal));
        assertThrows(IllegalArgumentException.class,
                () -> element.bindProperty("classList", signal));
        assertThrows(IllegalArgumentException.class,
                () -> element.bindProperty("className", signal));
        assertThrows(IllegalArgumentException.class,
                () -> element.bindProperty("outerHTML", signal));
    }

    @Test
    public void bindProperty_notComponent_doNotThrowException() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        element.bindProperty("foobar", signal);
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindProperty_setPropertyWhileBindingIsActive_throwException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindProperty("foo", signal);

        assertThrows(BindingActiveException.class,
                () -> component.getElement().setProperty("foo", "baz"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindProperty_removePropertyWhileBindingIsActive_throwException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindProperty("foo", signal);

        assertThrows(BindingActiveException.class,
                () -> component.getElement().removeProperty("foo"));
        Assert.assertTrue(events.isEmpty());
    }

    // boolean property signal binding tests

    @Test
    public void bindBooleanProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<Boolean> signal = new ValueSignal<>(true);

        component.getElement().bindProperty("foo", signal);

        assertNull(component.getElement().getProperty("foo"));
    }

    @Test
    public void bindBooleanProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(true);

        component.getElement().bindProperty("foo", signal);

        component.removeFromParent();

        signal.value(false);

        Assert.assertTrue(events.isEmpty());
        assertTrue(component.getElement().getProperty("foo", false));
    }

    @Test
    public void bindBooleanProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(true);

        component.getElement().bindProperty("foo", signal);

        assertTrue(component.getElement().getProperty("foo", false));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindBooleanProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(true);

        component.getElement().bindProperty("foo", signal);
        assertTrue(component.getElement().getProperty("foo", false));

        component.removeFromParent();
        signal.value(false);

        assertEquals(false, signal.peek());
        assertTrue(component.getElement().getProperty("foo", false));

        UI.getCurrent().add(component);
        assertFalse(component.getElement().getProperty("foo", true));

        Assert.assertTrue(events.isEmpty());
    }

    // double property signal binding tests

    @Test
    public void bindDoubleProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<Double> signal = new ValueSignal<>(1.0d);

        component.getElement().bindProperty("foo", signal);

        assertNull(component.getElement().getProperty("foo"));
    }

    @Test
    public void bindDoubleProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Double> signal = new ValueSignal<>(1.0d);

        component.getElement().bindProperty("foo", signal);

        component.removeFromParent();

        signal.value(2.0d);

        assertTrue(events.isEmpty());
        assertEquals(1.0d, component.getElement().getProperty("foo", -1.0d),
                0.0d);
    }

    @Test
    public void bindDoubleProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Double> signal = new ValueSignal<>(1.0d);

        component.getElement().bindProperty("foo", signal);

        assertEquals(1.0d, component.getElement().getProperty("foo", -1.0d),
                0.0d);
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindDoubleProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Double> signal = new ValueSignal<>(1.0d);

        component.getElement().bindProperty("foo", signal);
        assertEquals(1.0d, component.getElement().getProperty("foo", -1.0d),
                0.0d);

        component.removeFromParent();
        signal.value(2.0d);

        assertEquals(2.0d, signal.peek(), 0.0d);
        assertEquals(1.0d, component.getElement().getProperty("foo", -1.0d),
                0.0d);

        UI.getCurrent().add(component);
        assertEquals(2.0d, component.getElement().getProperty("foo", -1.0d),
                0.0d);

        Assert.assertTrue(events.isEmpty());
    }

    // integer property signal binding tests

    @Test
    public void bindIntegerProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<Integer> signal = new ValueSignal<>(1);

        component.getElement().bindProperty("foo", signal);

        assertNull(component.getElement().getProperty("foo"));
    }

    @Test
    public void bindIntegerProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Integer> signal = new ValueSignal<>(1);

        component.getElement().bindProperty("foo", signal);

        component.removeFromParent();

        signal.value(2);

        assertTrue(events.isEmpty());
        assertEquals(1, component.getElement().getProperty("foo", -1));
    }

    @Test
    public void bindIntegerProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Integer> signal = new ValueSignal<>(1);

        component.getElement().bindProperty("foo", signal);

        assertEquals(1, component.getElement().getProperty("foo", -1));
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindIntegerProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Integer> signal = new ValueSignal<>(1);

        component.getElement().bindProperty("foo", signal);
        assertEquals(1, component.getElement().getProperty("foo", -1));

        component.removeFromParent();
        signal.value(2);

        assertEquals(2, (long) signal.peek());
        assertEquals(1, component.getElement().getProperty("foo", -1));

        UI.getCurrent().add(component);
        assertEquals(2, component.getElement().getProperty("foo", -1));

        Assert.assertTrue(events.isEmpty());
    }

    // string property signal binding tests

    @Test
    public void bindStringProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindProperty("foo", signal);

        assertNull(component.getElement().getProperty("foo", null));
    }

    @Test
    public void bindStringProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindProperty("foo", signal);

        component.removeFromParent();

        signal.value("baz");

        Assert.assertTrue(events.isEmpty());
        assertEquals("bar",
                component.getElement().getProperty("foo", "default"));
    }

    @Test
    public void bindStringProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindProperty("foo", signal);

        assertEquals("bar",
                component.getElement().getProperty("foo", "default"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindStringProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindProperty("foo", signal);
        assertEquals("bar",
                component.getElement().getProperty("foo", "default"));

        component.removeFromParent();
        signal.value("baz");

        assertEquals("baz", signal.peek());
        assertEquals("bar",
                component.getElement().getProperty("foo", "default"));

        UI.getCurrent().add(component);
        assertEquals("baz",
                component.getElement().getProperty("foo", "default"));

        Assert.assertTrue(events.isEmpty());
    }

    // bean property signal binding tests

    @Test
    public void bindBeanProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<JacksonUtilsTest.Person> signal = new ValueSignal<>(
                createJohn());
        component.getElement().bindProperty("foo", signal);

        assertNull(component.getElement().getProperty("foo", null));
    }

    @Test
    public void bindBeanProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        JacksonUtilsTest.Person john = createJohn();
        ValueSignal<JacksonUtilsTest.Person> signal = new ValueSignal<>(john);
        component.getElement().bindProperty("foo", signal);

        component.removeFromParent();

        signal.value(createPerson("Jack", 52, false));

        assertPersonEquals(john,
                (JsonNode) component.getElement().getPropertyRaw("foo"));
        Assert.assertTrue(events.isEmpty());

    }

    @Test
    public void bindBeanProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        JacksonUtilsTest.Person john = createJohn();
        ValueSignal<JacksonUtilsTest.Person> signal = new ValueSignal<>(john);
        component.getElement().bindProperty("foo", signal);

        assertPersonEquals(john,
                (JsonNode) component.getElement().getPropertyRaw("foo"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindBeanProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        JacksonUtilsTest.Person john = createJohn();
        ValueSignal<JacksonUtilsTest.Person> signal = new ValueSignal<>(john);
        component.getElement().bindProperty("foo", signal);

        assertPersonEquals(john,
                (JsonNode) component.getElement().getPropertyRaw("foo"));

        component.removeFromParent();
        JacksonUtilsTest.Person jack = createJack();
        signal.value(jack);

        assertEquals(jack, signal.peek());
        assertPersonEquals(john,
                (JsonNode) component.getElement().getPropertyRaw("foo"));

        UI.getCurrent().add(component);

        assertPersonEquals(jack,
                (JsonNode) component.getElement().getPropertyRaw("foo"));
        Assert.assertTrue(events.isEmpty());
    }

    // list property signal binding tests

    @Test
    public void bindListProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<List<JacksonUtilsTest.Person>> signal = new ValueSignal<>(
                Arrays.asList(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal);

        assertNull(component.getElement().getProperty("foo", null));
    }

    @Test
    public void bindListProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<JacksonUtilsTest.Person>> signal = new ValueSignal<>(
                Arrays.asList(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal);
        assertEquals("John",
                getFromList(component, "foo", 0).get("name").asString());
        assertEquals("Jack",
                getFromList(component, "foo", 1).get("name").asString());

        component.removeFromParent();

        signal.value(Arrays.asList(createJack(), createJohn()));
        assertEquals("John",
                getFromList(component, "foo", 0).get("name").asString());
        assertEquals("Jack",
                getFromList(component, "foo", 1).get("name").asString());

        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindListProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<JacksonUtilsTest.Person>> signal = new ValueSignal<>(
                Arrays.asList(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal);

        assertEquals("John",
                getFromList(component, "foo", 0).get("name").asString());
        assertEquals("Jack",
                getFromList(component, "foo", 1).get("name").asString());
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindListProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<?>> signal = new ValueSignal<>(
                Arrays.asList(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal);

        // assert initial value
        assertEquals("John",
                getFromList(component, "foo", 0).get("name").asString());
        assertEquals("Jack",
                getFromList(component, "foo", 1).get("name").asString());

        component.removeFromParent();
        signal.value(Arrays.asList(createJack(), createJohn()));

        // assert signal value updated
        assertEquals("Jack",
                ((Map<?, ?>) signal.peek().getFirst()).get("name"));
        assertEquals("John", ((Map<?, ?>) signal.peek().getLast()).get("name"));
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
        Assert.assertTrue(events.isEmpty());
    }

    // map property signal binding tests

    @Test
    public void bindMapProperty_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<Map<?, ?>> signal = new ValueSignal<>(
                createPersonMap(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal);

        assertNull(component.getElement().getProperty("foo", null));
    }

    @Test
    public void bindMapProperty_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Map<?, ?>> signal = new ValueSignal<>(
                createPersonMap(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal);
        assertEquals("John",
                getFromMap(component, "foo", "0").get("name").asString());
        assertEquals("Jack",
                getFromMap(component, "foo", "1").get("name").asString());

        component.removeFromParent();

        signal.value(createPersonMap(createJack(), createJohn()));
        assertEquals("John",
                getFromMap(component, "foo", "0").get("name").asString());
        assertEquals("Jack",
                getFromMap(component, "foo", "1").get("name").asString());

        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindMapProperty_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Map<?, ?>> signal = new ValueSignal<>(
                createPersonMap(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal);

        assertEquals("John",
                getFromMap(component, "foo", "0").get("name").asString());
        assertEquals("Jack",
                getFromMap(component, "foo", "1").get("name").asString());
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindMapProperty_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Map<?, ?>> signal = new ValueSignal<>(
                createPersonMap(createJohn(), createJack()));
        component.getElement().bindProperty("foo", signal);

        // assert initial value
        assertEquals("John",
                getFromMap(component, "foo", "0").get("name").asString());
        assertEquals("Jack",
                getFromMap(component, "foo", "1").get("name").asString());

        component.removeFromParent();
        signal.value(createPersonMap(createJack(), createJohn()));

        // assert signal value updated
        assertEquals("Jack", ((Map<?, ?>) signal.peek().get("0")).get("name"));
        assertEquals("John", ((Map<?, ?>) signal.peek().get("1")).get("name"));
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
        Assert.assertTrue(events.isEmpty());
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

    private Map createPersonMap(JacksonUtilsTest.Person... persons) {
        Map map = new HashMap<>();
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
