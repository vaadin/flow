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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementBindAttributeTest extends SignalsUnitTest {

    @Test
    void bindAttribute_nullAttribute_throwException() {
        Element element = new Element("foo");
        ValueSignal<String> signal = new ValueSignal<>("bar");
        assertThrows(IllegalArgumentException.class,
                () -> element.bindAttribute(null, signal));
    }

    @Test
    void bindAttribute_illegalAttribute_throwException() {
        Element element = new Element("foo");
        ValueSignal<String> signal = new ValueSignal<>("bar");
        assertThrows(IllegalArgumentException.class,
                () -> element.bindAttribute("\"foo\"", signal));
    }

    @Test
    void bindAttribute_notComponent_doNotThrowException() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        element.bindAttribute("foobar", signal);
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttribute_componentNotAttached_bindingIgnored() {
        TestComponent component = new TestComponent();

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);

        // Probe runs immediately at bind time even when not attached
        assertEquals("bar", component.getElement().getAttribute("foo"));

        // Signal changes while detached are ignored (effect is passivated)
        signal.set("changed");
        assertEquals("bar", component.getElement().getAttribute("foo"));
    }

    @Test
    void bindAttribute_componentDetached_bindingIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);

        component.removeFromParent();

        signal.set("baz");

        assertTrue(events.isEmpty());
        assertEquals("bar", component.getElement().getAttribute("foo"));
    }

    @Test
    void bindAttribute_componentAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);

        assertEquals("bar", component.getElement().getAttribute("foo"));
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttribute_componentReAttached_bindingSynced() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);
        assertEquals("bar", component.getElement().getAttribute("foo"));

        component.removeFromParent();
        signal.set("baz");

        assertEquals("baz", signal.peek());
        assertEquals("bar", component.getElement().getAttribute("foo"));

        UI.getCurrent().add(component);
        assertEquals("baz", component.getElement().getAttribute("foo"));

        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttribute_setAttributeWhileBindingIsActive_throwException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);

        assertThrows(BindingActiveException.class,
                () -> component.getElement().setAttribute("foo", "baz"));
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttribute_removeAttributeWhileBindingIsActive_throwException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);

        assertThrows(BindingActiveException.class,
                () -> component.getElement().removeAttribute("foo"));
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttribute_updateSignal_attributeChanged() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo", signal);
        signal.set("baz");

        assertEquals("baz", component.getElement().getAttribute("foo"));
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttribute_nullSignal_throwsNPE() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        assertThrows(NullPointerException.class,
                () -> component.getElement().bindAttribute("foo", null));
    }

    @Test
    void bindAttribute_withTwoAttributesWithSameSignal_attributesChanged() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("foo");

        component.getElement().bindAttribute("attr1", signal);
        component.getElement().bindAttribute("attr2", signal);

        assertEquals("foo", component.getElement().getAttribute("attr1"));
        assertEquals("foo", component.getElement().getAttribute("attr2"));

        signal.set("foobar");

        assertEquals("foobar", component.getElement().getAttribute("attr1"));
        assertEquals("foobar", component.getElement().getAttribute("attr2"));
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttribute_withTwoAttributesAndSignals_attributesChanged() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal1 = new ValueSignal<>("foo");
        ValueSignal<String> signal2 = new ValueSignal<>("bar");

        component.getElement().bindAttribute("attr1", signal1);
        component.getElement().bindAttribute("attr2", signal2);

        assertEquals("foo", component.getElement().getAttribute("attr1"));
        assertEquals("bar", component.getElement().getAttribute("attr2"));

        signal1.set("foobar");
        signal2.set("barfoo");

        assertEquals("foobar", component.getElement().getAttribute("attr1"));
        assertEquals("barfoo", component.getElement().getAttribute("attr2"));
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttribute_simpleComputedSignal_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("bar");

        component.getElement().bindAttribute("foo",
                signal.map(v -> "mapped-" + v));

        assertEquals("mapped-bar", component.getElement().getAttribute("foo"));
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttribute_computedSignal_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Void> dependency = new ValueSignal<>(null);
        Signal<String> signal = Signal.computed(() -> {
            dependency.get();
            return "bar";
        });
        Signal<String> computedSignal = Signal
                .computed(() -> "computed-" + signal.get());

        component.getElement().bindAttribute("foo", computedSignal);

        assertEquals("computed-bar",
                component.getElement().getAttribute("foo"));
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttribute_nullAttributeValue_attributeRemoved() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(true);

        Element element = component.getElement();
        element.bindAttribute("foo", signal.map(value -> value ? "" : null));

        assertEquals("", element.getAttribute("foo"));
        assertTrue(element.getAttributeNames().anyMatch("foo"::equals));

        signal.set(false);

        assertNull(element.getAttribute("foo"));
        // expecting whole attribute to be removed
        assertFalse(element.hasAttribute("foo"));
        assertFalse(element.getAttributeNames().anyMatch("foo"::equals),
                "a removed attribute should not be listed among the attribute names");

        // toggling the signal back adds the attribute again
        signal.set(true);

        assertEquals("", element.getAttribute("foo"));
        assertTrue(element.hasAttribute("foo"));
        assertTrue(element.getAttributeNames().anyMatch("foo"::equals));
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttributeBoolean_toggleSignal_attributePresenceFollows() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(false);

        Element element = component.getElement();
        element.bindAttributeBoolean("noborder", signal);

        assertFalse(element.hasAttribute("noborder"));
        assertNull(element.getAttribute("noborder"));
        assertFalse(element.getAttributeNames().anyMatch("noborder"::equals));

        signal.set(true);

        assertTrue(element.hasAttribute("noborder"));
        // a present boolean attribute renders as <div noborder>
        assertEquals("", element.getAttribute("noborder"));
        assertTrue(element.getAttributeNames().anyMatch("noborder"::equals));

        signal.set(false);

        assertFalse(element.hasAttribute("noborder"));
        assertFalse(element.getAttributeNames().anyMatch("noborder"::equals));
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttributeBoolean_nullSignalValue_attributeAbsent() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        // a null value is treated the same as false
        ValueSignal<Boolean> signal = new ValueSignal<>(null);

        Element element = component.getElement();
        element.bindAttributeBoolean("noborder", signal);

        assertFalse(element.hasAttribute("noborder"));

        signal.set(true);

        assertTrue(element.hasAttribute("noborder"));

        signal.set(null);

        assertFalse(element.hasAttribute("noborder"));
        assertFalse(element.getAttributeNames().anyMatch("noborder"::equals));
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttributeBoolean_onChange_receivesBooleanValues() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(false);

        List<String> changes = new ArrayList<>();
        SignalBinding<Boolean> binding = component.getElement()
                .bindAttributeBoolean("noborder", signal);
        binding.onChange(context -> changes
                .add(context.getOldValue() + "->" + context.getNewValue()));

        assertEquals(List.of("false->false"), changes);

        signal.set(true);

        assertEquals(List.of("false->false", "false->true"), changes);
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttributeBoolean_bindWhileBindingIsActive_throwException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        Element element = component.getElement();
        element.bindAttributeBoolean("noborder", new ValueSignal<>(true));

        assertThrows(BindingActiveException.class, () -> element
                .bindAttributeBoolean("noborder", new ValueSignal<>(false)));
        assertThrows(BindingActiveException.class, () -> element
                .bindAttribute("noborder", new ValueSignal<>("value")));
        assertThrows(BindingActiveException.class,
                () -> element.setAttribute("noborder", false));
        assertTrue(events.isEmpty());
    }

    @Test
    void bindAttributeBoolean_classOrStyleAttribute_throwException() {
        Element element = new Element("foo");

        assertThrows(UnsupportedOperationException.class, () -> element
                .bindAttributeBoolean("class", new ValueSignal<>(true)));
        assertThrows(UnsupportedOperationException.class, () -> element
                .bindAttributeBoolean("style", new ValueSignal<>(true)));
    }

    @Tag("div")
    private static class TestComponent extends Component {

    }
}
