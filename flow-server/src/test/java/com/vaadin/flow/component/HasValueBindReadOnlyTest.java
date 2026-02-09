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
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link HasValue#bindReadOnly(Signal)}.
 */
public class HasValueBindReadOnlyTest extends SignalsUnitTest {

    @Test
    public void bindReadOnly_elementAttachedBefore_bindingActive() {
        TestComponent component = new TestComponent();
        // attach before bindReadOnly
        UI.getCurrent().add(component);
        assertFalse(component.isReadOnly());

        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindReadOnly(signal);

        assertTrue(component.isReadOnly());
    }

    @Test
    public void bindReadOnly_elementAttachedAfter_bindingActive() {
        TestComponent component = new TestComponent();
        assertFalse(component.isReadOnly());

        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindReadOnly(signal);
        // attach after bindReadOnly
        UI.getCurrent().add(component);

        assertTrue(component.isReadOnly());
    }

    @Test
    public void bindReadOnly_elementAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindReadOnly(signal);

        // initially true
        assertTrue(component.isReadOnly());

        // true -> false
        signal.value(false);
        assertFalse(component.isReadOnly());

        // false -> true
        signal.value(true);
        assertTrue(component.isReadOnly());
    }

    @Test
    public void bindReadOnly_elementNotAttached_bindingInactive() {
        TestComponent component = new TestComponent();
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindReadOnly(signal);
        signal.value(false);

        assertFalse(component.isReadOnly());
    }

    @Test
    public void bindReadOnly_elementDetached_bindingInactive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindReadOnly(signal);
        component.removeFromParent();
        signal.value(false); // ignored

        assertTrue(component.isReadOnly());
    }

    @Test
    public void bindReadOnly_elementReAttached_bindingActivate() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindReadOnly(signal);
        component.removeFromParent();
        signal.value(false);
        UI.getCurrent().add(component);

        assertFalse(component.isReadOnly());
    }

    @Test
    public void bindReadOnly_bindOrSetReadOnlyWhileBindingIsActive_throwException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        component.bindReadOnly(new ValueSignal<>(true));

        assertThrows(BindingActiveException.class,
                () -> component.bindReadOnly(new ValueSignal<>(false)));
        assertThrows(BindingActiveException.class,
                () -> component.setReadOnly(false));
        assertTrue(component.isReadOnly());
    }

    @Test
    public void bindReadOnly_withNullBinding_removesBinding() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindReadOnly(signal);
        assertTrue(component.isReadOnly());

        component.bindReadOnly(null); // remove binding
        signal.value(false); // no effect
        assertTrue(component.isReadOnly());

        component.setReadOnly(false);
        assertFalse(component.isReadOnly());
    }

    @Test
    public void bindReadOnly_withNullBinding_allowsSetReadOnly() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindReadOnly(signal);
        assertTrue(component.isReadOnly());

        component.bindReadOnly(null); // remove binding

        component.setReadOnly(false);
        assertFalse(component.isReadOnly());
    }

    @Test
    public void bindReadOnly_nullSignalValue_setsReadOnlyToFalse() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindReadOnly(signal);
        assertTrue(component.isReadOnly());

        // null transforms to false (default value for boolean property)
        signal.value(null);
        assertFalse(component.isReadOnly());
    }

    @Test
    public void bindReadOnly_toggleSignalValue_readOnlyUpdates() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        component.bindReadOnly(signal);
        assertFalse(component.isReadOnly());

        signal.value(true);
        assertTrue(component.isReadOnly());

        signal.value(false);
        assertFalse(component.isReadOnly());

        signal.value(true);
        assertTrue(component.isReadOnly());
    }

    /**
     * Test component implementing {@link HasValueAndElement}.
     */
    @Tag(Tag.INPUT)
    private static class TestComponent
            extends AbstractField<TestComponent, String> {

        public TestComponent() {
            super("");
        }

        @Override
        protected void setPresentationValue(String newPresentationValue) {
            // NOP
        }
    }
}
