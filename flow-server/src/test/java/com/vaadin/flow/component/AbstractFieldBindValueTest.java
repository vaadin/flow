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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.signals.SharedValueSignal;
import com.vaadin.signals.core.BindingActiveException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class AbstractFieldBindValueTest extends SignalsUnitTest {

    @Test
    public void bindValue_elementAttachedBefore_bindingActive() {
        TestInput input = new TestInput();
        // attach before bindValue
        UI.getCurrent().add(input);
        assertEquals("", input.getValue());
        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");
        input.bindValue(signal);

        assertEquals("foo", input.getValue());
    }

    @Test
    public void bindValue_elementAttachedAfter_bindingActive() {
        TestInput input = new TestInput();
        assertEquals("", input.getValue());
        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");
        input.bindValue(signal);
        // attach after bindValue
        UI.getCurrent().add(input);

        assertEquals("foo", input.getValue());
    }

    @Test
    public void bindValue_elementAttached_bindingActive() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");
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
        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");
        input.bindValue(signal);
        signal.value("bar");

        assertEquals("", input.getValue());
    }

    @Test
    public void bindValue_elementDetached_bindingInactive() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");
        input.bindValue(signal);
        input.removeFromParent();
        signal.value("bar"); // ignored

        assertEquals("foo", input.getValue());
    }

    @Test
    public void bindValue_elementReAttached_bindingActivate() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");
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
        input.bindValue(new SharedValueSignal<>("foo"));

        assertThrows(BindingActiveException.class,
                () -> input.bindValue(new SharedValueSignal<>("bar")));
        assertEquals("foo", input.getValue());
    }

    @Test
    public void bindValue_setValueWhileBindingIsActive_signalUpdated() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");
        input.bindValue(signal);

        input.setValue("bar");
        assertEquals("bar", input.getValue());
        assertEquals("bar", signal.peek());
    }

    @Test
    public void bindValue_withNullBinding_removesBinding() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");
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
        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");
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
                .ifPresent(feature -> Assert.fail(
                        "SignalBindingFeature should not be initialized before binding a signal"));

        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");
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

        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");
        input.bindValue(signal);

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();
        input.addValueChangeListener(
                event -> listenerValue.set(event.getValue()));

        Assert.assertNull(listenerValue.get());
        signal.value("bar");
        Assert.assertEquals("bar", listenerValue.get());
    }

    @Test
    public void bindValue_addValueChangeListener_bindValueTriggersEvent() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);

        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();
        input.addValueChangeListener(
                event -> listenerValue.set(event.getValue()));
        Assert.assertNull(listenerValue.get());
        input.bindValue(signal);

        Assert.assertEquals("foo", listenerValue.get());
    }

    @Test
    public void bindValue_setValue_countEffectExecutions() {
        TestInput input = new TestInput();

        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");
        input.bindValue(signal);

        AtomicInteger counter = new AtomicInteger(0);
        ComponentEffect.effect(input, () -> {
            signal.value();
            counter.incrementAndGet();
        });

        Assert.assertEquals(0, counter.get());
        UI.getCurrent().add(input);
        // effect run once on attach
        Assert.assertEquals(1, counter.get());

        input.setValue("bar");
        Assert.assertEquals(2, counter.get());

        input.setValue("bar");
        Assert.assertEquals(2, counter.get());

        input.setValue("foo");
        Assert.assertEquals(3, counter.get());

        signal.value("baz");
        Assert.assertEquals(4, counter.get());

        input.setValue("baz");
        Assert.assertEquals(4, counter.get());
    }

    @Test
    public void bindValue_forElementProperty_addValueChangeListener_bindingValueChangeTriggersEvent() {
        TestPropertyInput input = new TestPropertyInput();
        UI.getCurrent().add(input);

        SharedValueSignal<String> signal = new SharedValueSignal<>("foo");

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();
        input.addValueChangeListener(
                event -> listenerValue.set(event.getValue()));
        Assert.assertEquals("", input.getValue());
        Assert.assertNull(listenerValue.get());
        input.bindValue(signal);

        // value after bindValue
        Assert.assertEquals("foo", input.getValue());
        Assert.assertEquals("foo", listenerValue.get());

        // value after signal value change
        signal.value("bar");
        Assert.assertEquals("bar", input.getValue());
        Assert.assertEquals("bar", listenerValue.get());

        // null is not allowed in TestPropertyInput. Default value is "".
        signal.value(null);
        // value doesn't change
        Assert.assertEquals("bar", input.getValue());
        Assert.assertEquals("bar", listenerValue.get());
        Assert.assertEquals(1, events.size());
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
