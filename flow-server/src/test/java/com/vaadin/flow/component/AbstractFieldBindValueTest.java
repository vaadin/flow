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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class AbstractFieldBindValueTest extends SignalsUnitTest {

    @Test
    public void bindValue_elementAttachedBefore_bindingActive() {
        TestInput input = new TestInput();
        // attach before bindValue
        UI.getCurrent().add(input);
        assertEquals("", input.getValue());
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, signal::set);

        assertEquals("foo", input.getValue());
    }

    @Test
    public void bindValue_elementAttachedAfter_bindingActive() {
        TestInput input = new TestInput();
        assertEquals("", input.getValue());
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, signal::set);
        // attach after bindValue
        UI.getCurrent().add(input);

        assertEquals("foo", input.getValue());
    }

    @Test
    public void bindValue_elementAttached_bindingActive() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, signal::set);

        // initially "foo"
        assertEquals("foo", input.getValue());

        // "foo" -> "bar"
        signal.set("bar");
        assertEquals("bar", input.getValue());

        signal.set(null);
        assertNull(input.getValue());
        assertEquals(3, input.setValueCounter);
    }

    @Test
    public void bindValue_elementNotAttached_bindingInactive() {
        TestInput input = new TestInput();
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, signal::set);
        signal.set("bar");

        assertEquals("", input.getValue());
    }

    @Test
    public void bindValue_elementDetached_bindingInactive() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, signal::set);
        input.removeFromParent();
        signal.set("bar"); // ignored

        assertEquals("foo", input.getValue());
    }

    @Test
    public void bindValue_elementReAttached_bindingActivate() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, signal::set);
        input.removeFromParent();
        signal.set("bar");
        UI.getCurrent().add(input);

        assertEquals("bar", input.getValue());
    }

    @Test
    public void bindValue_bindValueWhileBindingIsActive_throwException() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal1 = new ValueSignal<>("foo");
        input.bindValue(signal1, signal1::set);

        ValueSignal<String> signal2 = new ValueSignal<>("bar");
        assertThrows(BindingActiveException.class,
                () -> input.bindValue(signal2, signal2::set));
        assertEquals("foo", input.getValue());
    }

    @Test
    public void bindValue_setValueWhileBindingIsActive_signalUpdated() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, signal::set);

        input.setValue("bar");
        assertEquals("bar", input.getValue());
        assertEquals("bar", signal.peek());
    }

    @Test
    public void bindValue_nullSignal_throwsNPE() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);

        assertThrows(NullPointerException.class,
                () -> input.bindValue(null, null));
    }

    @Test
    public void bindValue_removeBindingViaFeature_stopsUpdatesAndAllowsManualSet() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, signal::set);
        assertEquals("foo", input.getValue());

        // Remove binding via the node's SignalBindingFeature
        SignalBindingFeature feature = input.getElement().getNode()
                .getFeature(SignalBindingFeature.class);
        feature.removeBinding(SignalBindingFeature.VALUE);

        // Signal changes should no longer affect the component
        signal.set("bar");
        assertEquals("foo", input.getValue());

        // Manual set should work and not update the signal
        input.setValue("baz");
        assertEquals("baz", input.getValue());
        assertEquals("bar", signal.peek());
    }

    @Test
    public void bindValue_lazyInitSignalBindingFeature() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        input.setValue("foo");
        input.getValue();
        input.getElement().getNode()
                .getFeatureIfInitialized(SignalBindingFeature.class)
                .ifPresent(feature -> fail(
                        "SignalBindingFeature should not be initialized before binding a signal"));

        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, signal::set);

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
        input.bindValue(signal, signal::set);

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();
        input.addValueChangeListener(
                event -> listenerValue.set(event.getValue()));

        assertNull(listenerValue.get());
        signal.set("bar");
        assertEquals("bar", listenerValue.get());
    }

    @Test
    public void bindValue_addValueChangeListener_bindValueTriggersEvent() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);

        ValueSignal<String> signal = new ValueSignal<>("foo");

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();
        input.addValueChangeListener(
                event -> listenerValue.set(event.getValue()));
        assertNull(listenerValue.get());
        input.bindValue(signal, signal::set);

        assertEquals("foo", listenerValue.get());
    }

    @Test
    public void bindValue_setValue_countEffectExecutions() {
        TestInput input = new TestInput();

        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, signal::set);

        AtomicInteger counter = new AtomicInteger(0);
        ComponentEffect.effect(input, () -> {
            signal.get();
            counter.incrementAndGet();
        });

        assertEquals(0, counter.get());
        UI.getCurrent().add(input);
        // effect run once on attach
        assertEquals(1, counter.get());

        input.setValue("bar");
        assertEquals(2, counter.get());

        input.setValue("bar");
        assertEquals(2, counter.get());

        input.setValue("foo");
        assertEquals(3, counter.get());

        signal.set("baz");
        assertEquals(4, counter.get());

        input.setValue("baz");
        assertEquals(4, counter.get());
    }

    @Test
    public void bindValue_forElementProperty_addValueChangeListener_bindingValueChangeTriggersEvent() {
        TestPropertyInput input = new TestPropertyInput();
        UI.getCurrent().add(input);

        ValueSignal<String> signal = new ValueSignal<>("foo");

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();
        input.addValueChangeListener(
                event -> listenerValue.set(event.getValue()));
        assertEquals("", input.getValue());
        assertNull(listenerValue.get());
        input.bindValue(signal, signal::set);

        // value after bindValue
        assertEquals("foo", input.getValue());
        assertEquals("foo", listenerValue.get());

        // value after signal value change
        signal.set("bar");
        assertEquals("bar", input.getValue());
        assertEquals("bar", listenerValue.get());

        // null is not allowed in TestPropertyInput. Default value is "".
        signal.set(null);
        // value doesn't change
        assertEquals("bar", input.getValue());
        assertEquals("bar", listenerValue.get());
        assertEquals(1, events.size());
        // clear events for next verification in SignalsUnitTest.after
        events.clear();
    }

    @Test
    public void bindValue_readOnlyBinding_setValueThrows() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, null);

        assertEquals("foo", input.getValue());

        assertThrows(IllegalStateException.class, () -> input.setValue("bar"));
        assertEquals("foo", input.getValue());
        assertEquals("foo", signal.peek());
    }

    @Test
    public void bindValue_readOnlyBinding_signalChangesStillWork() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, null);

        assertEquals("foo", input.getValue());

        signal.set("bar");
        assertEquals("bar", input.getValue());
    }

    @Test
    public void bindValue_readOnlyBinding_detachedSetValueDoesNotThrow() {
        TestInput input = new TestInput();
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, null);

        // Not attached, so setValue should succeed without throwing
        input.setValue("bar");
        assertEquals("bar", input.getValue());
    }

    @Test
    public void bindValue_noOpCallback_revertsToSignalValue() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        // No-op callback: ignores the value, signal stays at "foo"
        input.bindValue(signal, v -> {
        });

        input.setValue("bar");
        // Component should revert to signal's value since callback was no-op
        assertEquals("foo", input.getValue());
        assertEquals("foo", signal.peek());
    }

    @Test
    public void bindValue_transformingCallback_componentShowsTransformed() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        // Callback that uppercases the value
        input.bindValue(signal, v -> signal.set(v.toUpperCase()));

        input.setValue("bar");
        // Signal should have "BAR", and component should show "BAR"
        assertEquals("BAR", signal.peek());
        assertEquals("BAR", input.getValue());
    }

    @Test
    public void bindValue_normalCallback_setValueUpdatesBoth() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, signal::set);

        input.setValue("bar");
        assertEquals("bar", input.getValue());
        assertEquals("bar", signal.peek());
    }

    @Test
    public void bindValue_readOnlySignal_signalToComponentDirection() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> writable = new ValueSignal<>("foo");
        Signal<String> readOnly = writable.asReadonly();
        input.bindValue(readOnly, null);

        assertEquals("foo", input.getValue());

        writable.set("bar");
        assertEquals("bar", input.getValue());
    }

    @Test
    public void bindValue_writeCallbackThrows() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, value -> {
            throw new RuntimeException("test");
        });
        assertEquals("foo", input.getValue());

        input.addValueChangeListener(event -> {
            fail("Value change listener should not be triggered when write callback throws");
        });

        assertThrows(RuntimeException.class, () -> input.setValue("bar"));
    }

    @Test
    public void bindValue_normalCallback_valueChangeEventTriggered() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, signal::set);

        AtomicReference<String> eventValue = new AtomicReference<>();
        AtomicInteger counter = new AtomicInteger(0);
        input.addValueChangeListener(event -> {
            eventValue.set(event.getValue());
            counter.incrementAndGet();
        });

        input.setValue("bar");
        assertEquals("bar", eventValue.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void bindValue_transformingCallback_valueChangeEventTriggered() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, v -> signal.set(v.toUpperCase()));

        AtomicReference<String> eventValue = new AtomicReference<>();
        AtomicInteger counter = new AtomicInteger(0);
        input.addValueChangeListener(event -> {
            eventValue.set(event.getValue());
            counter.incrementAndGet();
        });

        input.setValue("bar");
        assertEquals("BAR", eventValue.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void bindValue_noOpCallback_valueChangeEventNotTriggered() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        input.bindValue(signal, value -> {
        });

        input.addValueChangeListener(event -> {
            fail("Value change listener should not be triggered with a no-op callback");
        });

        // With a no-op callback, value is not changed and event should not be
        // triggered
        input.setValue("bar");
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
