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
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class AbstractFieldBindValueTest {

    private static MockVaadinServletService service;

    private MockedStatic<FeatureFlags> featureFlagStaticMock;

    private MockUI ui;

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
        assertTrue(events.isEmpty());
        close(featureFlagStaticMock);
        events = null;
        ui = null;
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

    private LinkedList<ErrorEvent> mockLockedSessionWithErrorHandler() {
        VaadinService.setCurrent(service);

        var session = new MockVaadinSession(service);
        session.lock();

        // UI is set to field to avoid too eager GC due to WeakReference in
        // CurrentInstance.
        ui = new MockUI(session);
        var events = new LinkedList<ErrorEvent>();
        session.setErrorHandler(events::add);

        return events;
    }

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

        // null transforms to default value ""
        signal.value(null);
        assertEquals("", input.getValue());
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
    public void bindValue_setValueAndBindValueWhileBindingIsActive_throwException() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        input.bindValue(new ValueSignal<>("foo"));

        assertThrows(BindingActiveException.class, () -> input.setValue("bar"));
        assertThrows(BindingActiveException.class,
                () -> input.bindValue(new ValueSignal<>("bar")));
        assertEquals("foo", input.getValue());
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
    public void bindValue_lazyInitSignalBindingFeature() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);
        input.setValue("foo");
        input.getValue();
        input.getElement().getNode()
                .getFeatureIfInitialized(SignalBindingFeature.class)
                .ifPresent(feature -> Assert.fail(
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

        Assert.assertNull(listenerValue.get());
        signal.value("bar");
        Assert.assertEquals("bar", listenerValue.get());
    }

    @Test
    public void bindValue_addValueChangeListener_bindValueTriggersEvent() {
        TestInput input = new TestInput();
        UI.getCurrent().add(input);

        ValueSignal<String> signal = new ValueSignal<>("foo");

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();
        input.addValueChangeListener(
                event -> listenerValue.set(event.getValue()));
        Assert.assertNull(listenerValue.get());
        input.bindValue(signal);

        Assert.assertEquals("foo", listenerValue.get());
    }

    @Test
    public void bindValue_forElementProperty_addValueChangeListener_bindingValueChangeTriggersEvent() {
        TestPropertyInput input = new TestPropertyInput();
        UI.getCurrent().add(input);

        ValueSignal<String> signal = new ValueSignal<>("foo");

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

        // null defaults to defaultValue
        signal.value(null);
        Assert.assertEquals("", input.getValue());
        Assert.assertEquals("", listenerValue.get());
    }

    /**
     * Test input component using {@link AbstractField} directly.
     */
    @Tag(Tag.INPUT)
    private static class TestInput extends AbstractField<TestInput, String> {

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
