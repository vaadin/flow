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
package com.vaadin.flow.component;

import org.junit.Test;

import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.local.ValueSignal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for
 * {@link HasPlaceholder#bindPlaceholder(com.vaadin.signals.Signal)}.
 */
public class HasPlaceholderBindTest extends SignalsUnitTest {

    @Tag(Tag.DIV)
    private static class TestComponent extends Component
            implements HasPlaceholder {
    }

    @Test
    public void bindPlaceholder_elementAttached_updatesWithSignal() {
        TestComponent component = new TestComponent();
        // Attach component so that Element.bindProperty becomes active
        UI.getCurrent().add(component);

        // Bind a signal and verify initial propagation
        ValueSignal<String> signal = new ValueSignal<>("first");
        component.bindPlaceholder(signal);
        assertEquals("first", component.getPlaceholder());

        // Update signal and verify placeholder follows
        signal.value("second");
        assertEquals("second", component.getPlaceholder());

        // Another update to ensure continued propagation
        signal.value("third");
        assertEquals("third", component.getPlaceholder());
    }

    @Test
    public void bindPlaceholder_elementNotAttached_bindingInactive() {
        TestComponent component = new TestComponent();
        // Not attached yet
        ValueSignal<String> signal = new ValueSignal<>("foo");
        component.bindPlaceholder(signal);
        // No propagation while detached
        assertNull(component.getPlaceholder());
        signal.value("bar");
        assertNull(component.getPlaceholder());
    }

    @Test
    public void bindPlaceholder_attachAfterBinding_activatesAndAppliesLatest() {
        TestComponent component = new TestComponent();
        ValueSignal<String> signal = new ValueSignal<>("foo");
        component.bindPlaceholder(signal);
        // Update before attach
        signal.value("bar");
        assertNull(component.getPlaceholder());
        // Attach -> latest value is applied
        UI.getCurrent().add(component);
        assertEquals("bar", component.getPlaceholder());
    }

    @Test
    public void bindPlaceholder_elementDetached_bindingInactive_andReactivatesOnAttach() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        component.bindPlaceholder(signal);
        // Initially propagated
        assertEquals("foo", component.getPlaceholder());
        // Detach and update signal -> ignored while detached
        component.removeFromParent();
        signal.value("bar");
        assertEquals("foo", component.getPlaceholder());
        // Re-attach -> latest value applied
        UI.getCurrent().add(component);
        assertEquals("bar", component.getPlaceholder());
    }

    @Test
    public void bindPlaceholder_unbindWithNullSignal_keepsCurrentAndStopsUpdates() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("one");
        component.bindPlaceholder(signal);
        assertEquals("one", component.getPlaceholder());

        // Unbind by passing null; current value should remain
        component.bindPlaceholder(null);
        assertEquals("one", component.getPlaceholder());

        // Further updates to the old signal should not propagate
        signal.value("two");
        assertEquals("one", component.getPlaceholder());

        // Manual updates should work after unbind
        component.setPlaceholder("manual");
        assertEquals("manual", component.getPlaceholder());
    }

    @Test
    public void setPlaceholder_whileBindingActive_throwsBindingActiveException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        component.bindPlaceholder(signal);
        assertEquals("foo", component.getPlaceholder());
        assertThrows(
                "Expected BindingActiveException when setting placeholder while binding is active",
                BindingActiveException.class,
                () -> component.setPlaceholder("bar"));
    }

    @Test
    public void bindPlaceholder_againWhileActive_throwsBindingActiveException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        component.bindPlaceholder(signal);
        assertEquals("foo", component.getPlaceholder());
        assertThrows(
                "Expected BindingActiveException when binding a new signal while a binding is active",
                BindingActiveException.class,
                () -> component.bindPlaceholder(new ValueSignal<>("bar")));
    }
}
