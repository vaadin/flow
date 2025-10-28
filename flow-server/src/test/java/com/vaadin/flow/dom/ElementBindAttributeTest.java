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

import java.util.LinkedList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;
import com.vaadin.signals.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class ElementBindAttributeTest {

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

    @Test
    public void bindAttribute_nullAttribute_throwException() {
        Element element = new Element("foo");
        ValueSignal<String> signal = new ValueSignal<>("bar");
        assertThrows(IllegalArgumentException.class,
                () -> element.bindAttribute(null, signal));
    }

    @Test
    public void bindAttribute_illegalAttribute_throwException() {
        Element element = new Element("foo");
        ValueSignal<String> signal = new ValueSignal<>("bar");
        assertThrows(IllegalArgumentException.class,
                () -> element.bindAttribute("\"foo\"", signal));
    }

    @Test
    public void bindAttribute_notComponent_doNotThrowException() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        element.bindAttribute("foobar", signal);
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindAttribute_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);

        assertNull(component.getElement().getAttribute("foo"));
    }

    @Test
    public void bindAttribute_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);

        component.removeFromParent();

        signal.value("baz");

        Assert.assertTrue(events.isEmpty());
        assertEquals("bar", component.getElement().getAttribute("foo"));
    }

    @Test
    public void bindAttribute_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);

        assertEquals("bar", component.getElement().getAttribute("foo"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindAttribute_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);
        assertEquals("bar", component.getElement().getAttribute("foo"));

        component.removeFromParent();
        signal.value("baz");

        assertEquals("baz", signal.peek());
        assertEquals("bar", component.getElement().getAttribute("foo"));

        UI.getCurrent().add(component);
        assertEquals("baz", component.getElement().getAttribute("foo"));

        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindAttribute_setAttributeWhileBindingIsActive_throwException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);

        assertThrows(BindingActiveException.class,
                () -> component.getElement().setAttribute("foo", "baz"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindAttribute_removeAttributeWhileBindingIsActive_throwException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);

        assertThrows(BindingActiveException.class,
                () -> component.getElement().removeAttribute("foo"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindAttribute_updateSignal_attributeChanged() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);
        signal.value("baz");

        assertEquals("baz", component.getElement().getAttribute("foo"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindAttribute_withNullBinding_removesBinding() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);

        assertEquals("bar", component.getElement().getAttribute("foo"));

        component.getElement().bindAttribute("foo", null);

        signal.value("baz");

        assertEquals("bar", component.getElement().getAttribute("foo"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindAttribute_withTwoAttributesWithSameSignal_attributesChanged() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("foo");

        component.getElement().bindAttribute("attr1", signal);
        component.getElement().bindAttribute("attr2", signal);

        assertEquals("foo", component.getElement().getAttribute("attr1"));
        assertEquals("foo", component.getElement().getAttribute("attr2"));

        signal.value("foobar");

        assertEquals("foobar", component.getElement().getAttribute("attr1"));
        assertEquals("foobar", component.getElement().getAttribute("attr2"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindAttribute_withTwoAttributesAndSignals_attributesChanged() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal1 = new ValueSignal<>("foo");
        ValueSignal<String> signal2 = new ValueSignal<>("bar");

        component.getElement().bindAttribute("attr1", signal1);
        component.getElement().bindAttribute("attr2", signal2);

        assertEquals("foo", component.getElement().getAttribute("attr1"));
        assertEquals("bar", component.getElement().getAttribute("attr2"));

        signal1.value("foobar");
        signal2.value("barfoo");

        assertEquals("foobar", component.getElement().getAttribute("attr1"));
        assertEquals("barfoo", component.getElement().getAttribute("attr2"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindAttribute_simpleComputedSignal_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo",
                signal.map(v -> "mapped-" + v));

        assertEquals("mapped-bar", component.getElement().getAttribute("foo"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindAttribute_computedSignal_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        Signal<String> signal = Signal.computed(() -> "bar");
        Signal<String> computedSignal = Signal
                .computed(() -> "computed-" + signal.value());

        component.getElement().bindAttribute("foo", computedSignal);

        assertEquals("computed-bar",
                component.getElement().getAttribute("foo"));
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void bindAttribute_nullAttributeValue_attributeRemoved() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(true);

        component.getElement().bindAttribute("foo",
                signal.map(value -> value ? "" : null));

        assertEquals("", component.getElement().getAttribute("foo"));

        signal.value(false);

        assertNull(component.getElement().getAttribute("foo"));
        Assert.assertTrue(events.isEmpty());
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
