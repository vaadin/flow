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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link HasValue#bindRequiredIndicatorVisible(Signal)}.
 */
class HasValueBindRequiredIndicatorVisibleTest extends SignalsUnitTest {

    @Test
    public void bindRequired_elementAttachedBefore_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        assertFalse(component.isRequiredIndicatorVisible());

        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindRequiredIndicatorVisible(signal);

        assertTrue(component.isRequiredIndicatorVisible());
    }

    @Test
    public void bindRequired_elementAttachedAfter_bindingActive() {
        TestComponent component = new TestComponent();
        assertFalse(component.isRequiredIndicatorVisible());

        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindRequiredIndicatorVisible(signal);
        UI.getCurrent().add(component);

        assertTrue(component.isRequiredIndicatorVisible());
    }

    @Test
    public void bindRequired_elementAttached_bindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindRequiredIndicatorVisible(signal);

        // initially true
        assertTrue(component.isRequiredIndicatorVisible());

        // true -> false
        signal.set(false);
        assertFalse(component.isRequiredIndicatorVisible());

        // false -> true
        signal.set(true);
        assertTrue(component.isRequiredIndicatorVisible());
    }

    @Test
    public void bindRequired_elementNotAttached_bindingInactive() {
        TestComponent component = new TestComponent();
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindRequiredIndicatorVisible(signal);
        signal.set(false);

        assertFalse(component.isRequiredIndicatorVisible());
    }

    @Test
    public void bindRequired_elementDetached_bindingInactive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindRequiredIndicatorVisible(signal);
        component.removeFromParent();
        signal.set(false); // ignored

        assertTrue(component.isRequiredIndicatorVisible());
    }

    @Test
    public void bindRequired_elementReAttached_bindingActivate() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindRequiredIndicatorVisible(signal);
        component.removeFromParent();
        signal.set(false);
        UI.getCurrent().add(component);

        assertFalse(component.isRequiredIndicatorVisible());
    }

    @Test
    public void bindRequired_bindOrSetRequiredWhileBindingIsActive_throwException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        component.bindRequiredIndicatorVisible(new ValueSignal<>(true));

        assertThrows(BindingActiveException.class, () -> component
                .bindRequiredIndicatorVisible(new ValueSignal<>(false)));
        assertThrows(BindingActiveException.class,
                () -> component.setRequiredIndicatorVisible(false));
        assertTrue(component.isRequiredIndicatorVisible());
    }

    @Test
    public void bindRequired_nullSignal_throwsNPE() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        assertThrows(NullPointerException.class,
                () -> component.bindRequiredIndicatorVisible(null));
    }

    @Test
    public void bindRequired_nullSignalValue_setsRequiredToFalse() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindRequiredIndicatorVisible(signal);
        assertTrue(component.isRequiredIndicatorVisible());

        // null transforms to false (default value for boolean property)
        signal.set(null);
        assertFalse(component.isRequiredIndicatorVisible());
    }

    @Test
    public void bindRequired_toggleSignalValue_requiredUpdates() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        component.bindRequiredIndicatorVisible(signal);
        assertFalse(component.isRequiredIndicatorVisible());

        signal.set(true);
        assertTrue(component.isRequiredIndicatorVisible());

        signal.set(false);
        assertFalse(component.isRequiredIndicatorVisible());

        signal.set(true);
        assertTrue(component.isRequiredIndicatorVisible());
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
