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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractFieldBindValueTest extends SignalsUnitTest {

    @Test
    public void bindValue_elementAttachedBefore_bindingActive() {
        TestInput input = new TestInput();
        // attach before bindValue
        UI.getCurrent().add(input);
        assertEquals("", input.getValue());
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal);

        assertEquals("foo", input.getValue());
    }

    @Test
    public void bindValue_elementAttachedAfter_bindingActive() {
        TestInput input = new TestInput();
        assertEquals("", input.getValue());
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal);
        // attach after bindValue
        UI.getCurrent().add(input);

        assertEquals("foo", input.getValue());
    }

    @Test
    public void bindValue_elementAttached_bindingActive() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal);

        // initially "foo"
        assertEquals("foo", input.getValue());

        // "foo" -> "bar"
        signal.value("bar");
        assertEquals("bar", input.getValue());

        signal.value(null);
        assertNull(input.getValue());
        assertEquals(3, input.setValueCounter);
    }

    @Test
    public void bindValue_elementNotAttached_bindingInactive() {
        TestInput input = new TestInput();
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal);
        signal.value("bar");

        assertEquals("", input.getValue());
    }

    @Test
    public void bindValue_elementDetached_bindingInactive() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal);
        input.removeFromParent();
        signal.value("bar"); // ignored

        assertEquals("foo", input.getValue());
    }

    @Test
    public void bindValue_elementReAttached_bindingActivate() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal);
        input.removeFromParent();
        signal.value("bar");
        UI.getCurrent().add(input);

        assertEquals("bar", input.getValue());
    }

    @Test
    public void bindValue_bindValueWhileBindingIsActive_throwException() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        input.bindValue(new ValueSignal<>("foo"));

        assertThrows(BindingActiveException.class,
                () -> input.bindValue(new ValueSignal<>("bar")));
        assertEquals("foo", input.getValue());
    }

    @Test
    public void bindValue_setValueWhileBindingIsActive_signalUpdated() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal);

        input.setValue("bar");
        assertEquals("bar", input.getValue());
        assertEquals("bar", signal.peek());
    }

    @Test
    public void bindValue_withNullBinding_removesBinding() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal);
        assertEquals("foo", input.getValue());

        input.bindValue(null); // remove binding
        signal.value("bar"); // no effect
        assertEquals("foo", input.getValue());

        input.setValue("bar");
        assertEquals("bar", input.getValue());
    }

    @Test
    public void bindValue_withNullBinding_allowsSetValue() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal);
        assertEquals("foo", input.getValue());

        input.bindValue(null); // remove binding

        input.setValue("bar");
        assertEquals("bar", input.getValue());
    }

    @Test
    public void bindValue_lazyInitSignalBindingFeature() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        input.setValue("foo");
        input.getValue();
        input.getElement().getNode()
                .getFeatureIfInitialized(SignalBindingFeature.class)
                .ifPresent(feature -> Assertions.fail(
                        "SignalBindingFeature should not be initialized before binding a signal"));

        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal);

        input.getElement().getNode()
                .getFeatureIfInitialized(SignalBindingFeature.class)
                .orElseThrow(() -> new AssertionError(
                        "SignalBindingFeature should be initialized after binding a signal"));
    }

    @Test
    public void bindValue_addValueChangeListener_signalValueChangeTriggersEvent() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);

        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal);

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();
        input.addValueChangeListener(
                event -> listenerValue.set(event.getValue()));

        Assertions.assertNull(listenerValue.get());
        signal.value("bar");
        Assertions.assertEquals("bar", listenerValue.get());
    }

    @Test
    public void bindValue_addValueChangeListener_bindValueTriggersEvent() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);

        ValueSignal<String> signal = new ValueSignal<>("foo");

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();
        input.addValueChangeListener(
                event -> listenerValue.set(event.getValue()));
        Assertions.assertNull(listenerValue.get());
        input.bindValue(signal);

        Assertions.assertEquals("foo", listenerValue.get());
    }

    @Test
    public void bindValue_setValue_countEffectExecutions() {
        TestInput input = new TestInput();

        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal);

        AtomicInteger counter = new AtomicInteger(0);
        ComponentEffect.effect(input, () -> {
            signal.value();
            counter.incrementAndGet();
        });

        Assertions.assertEquals(0, counter.get());
        UI.getCurrent().add(input);
        // effect run once on attach
        Assertions.assertEquals(1, counter.get());

        input.setValue("bar");
        Assertions.assertEquals(2, counter.get());

        input.setValue("bar");
        Assertions.assertEquals(2, counter.get());

        input.setValue("foo");
        Assertions.assertEquals(3, counter.get());

        signal.value("baz");
        Assertions.assertEquals(4, counter.get());

        input.setValue("baz");
        Assertions.assertEquals(4, counter.get());
    }

    @Test
    public void bindValue_forElementProperty_addValueChangeListener_bindingValueChangeTriggersEvent() {
        TestPropertyInput input = new TestPropertyInput();
        UI.getCurrent().add(input);

        ValueSignal<String> signal = new ValueSignal<>("foo");

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();
        input.addValueChangeListener(
                event -> listenerValue.set(event.getValue()));
        Assertions.assertEquals("", input.getValue());
        Assertions.assertNull(listenerValue.get());
        input.bindValue(signal);

        // value after bindValue
        Assertions.assertEquals("foo", input.getValue());
        Assertions.assertEquals("foo", listenerValue.get());

        // value after signal value change
        signal.value("bar");
        Assertions.assertEquals("bar", input.getValue());
        Assertions.assertEquals("bar", listenerValue.get());

        // null is not allowed in TestPropertyInput. Default value is "".
        signal.value(null);
        // value doesn't change
        Assertions.assertEquals("bar", input.getValue());
        Assertions.assertEquals("bar", listenerValue.get());
        Assertions.assertEquals(1, events.size());
        // clear events for next verification in SignalsUnitTest.after
        events.clear();
    }

    /**
     * Test input component using {@link AbstractField} directly.
     */
    @Tag(Tag.INPUT)
    private static class TestInput extends AbstractField<TestInput, String> {

        int setValueCounter = 0;

        public TestInput() {
            this("");
        }

        public TestInput(String defaultValue) {
            super(defaultValue);
        }

        @Override
        protected void setPresentationValue(String newPresentationValue) {
            // NOP
        }

        @Override
        public void setValue(String value) {
            super.setValue(value);
            setValueCounter++;
        }
    }

    /**
     * Test input component using {@link AbstractSinglePropertyField} with a
     * value property.
     */
    @Tag(Tag.INPUT)
    private static class TestPropertyInput
            extends AbstractSinglePropertyField<TestInput, String> {

        public TestPropertyInput() {
            super("value", "", false);
        }

    }
}
