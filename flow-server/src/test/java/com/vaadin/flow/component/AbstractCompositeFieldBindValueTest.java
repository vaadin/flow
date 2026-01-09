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
package com.vaadin.flow.component;

import java.util.LinkedList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.AbstractSinglePropertyFieldTest.StringField;
import com.vaadin.flow.component.ComponentTest.TestDiv;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;
import com.vaadin.signals.WritableSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class AbstractCompositeFieldBindValueTest {

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

    private static class MultipleFieldsField extends
            AbstractCompositeField<TestDiv, MultipleFieldsField, String> {
        private final StringField start = new StringField();
        private final StringField rest = new StringField();

        public MultipleFieldsField() {
            super(null);

            getContent().getElement().appendChild(start.getElement(),
                    rest.getElement());

            start.addValueChangeListener(
                    event -> updateValue(event.isFromClient()));
            rest.addValueChangeListener(
                    event -> updateValue(event.isFromClient()));
        }

        private void updateValue(boolean fromClient) {
            String value = start.getValue();

            String restValue = rest.getValue();
            if (!restValue.isEmpty()) {
                value += " " + restValue;
            }

            setModelValue(value, fromClient);
        }

        @Override
        protected void setPresentationValue(String newPresentationValue) {
            String[] parts = newPresentationValue.split(" ", 2);
            start.setValue(parts[0]);

            if (parts.length > 1) {
                rest.setValue(parts[1]);
            } else {
                rest.setValue("");
            }
        }
    }

    @Test
    public void multipleFieldsField_bindValue() {
        MultipleFieldsField field = new MultipleFieldsField();
        UI.getCurrent().add(field);

        WritableSignal<String> signal = new ValueSignal<>("Hello Cool World");
        field.bindValue(signal);
        Assert.assertEquals("Hello", field.start.getValue());
        Assert.assertEquals("Cool World", field.rest.getValue());

        // test that setValue fails when bound
        Assert.assertThrows(BindingActiveException.class,
                () -> field.setValue(""));

        // setValue for CompositeField's components is allowed since their value
        // change listeners update the value by internal setModelValue method
        field.rest.setValue("");
        Assert.assertEquals("Hello", field.getValue());
        Assert.assertEquals("Hello", signal.peek());

        field.rest.setValue("Vaadin");
        Assert.assertEquals("Hello Vaadin", field.getValue());
        Assert.assertEquals("Hello Vaadin", signal.peek());

        // remove binding. Value should stay the same.
        field.bindValue(null);
        Assert.assertEquals("Hello Vaadin", field.getValue());
        Assert.assertEquals("Hello Vaadin", signal.peek());

        // test that setValue works after unbinding
        field.setValue("Hey You");
        Assert.assertEquals("Hey You", field.getValue());
        Assert.assertEquals("Hello Vaadin", signal.peek());
    }
}
