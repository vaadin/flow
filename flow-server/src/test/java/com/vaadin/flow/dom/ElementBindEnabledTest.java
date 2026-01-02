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
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class ElementBindEnabledTest {

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
    public void bindEnabled_elementAttachedBefore_bindingActive() {
        Element element = new Element("foo");
        // attach before bindEnabled
        UI.getCurrent().getElement().appendChild(element);
        assertTrue(element.isEnabled());
        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        element.bindEnabled(signal);

        assertFalse(element.isEnabled());
    }

    @Test
    public void bindEnabled_elementAttachedAfter_bindingActive() {
        Element element = new Element("foo");
        assertTrue(element.isEnabled());
        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        element.bindEnabled(signal);
        // attach after bindEnabled
        UI.getCurrent().getElement().appendChild(element);

        assertFalse(element.isEnabled());
    }

    @Test
    public void bindEnabled_elementAttached_bindingActive() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        element.bindEnabled(signal);

        // initially false
        assertFalse(element.isEnabled());

        // false -> true
        signal.value(true);
        assertTrue(element.isEnabled());

        // null transforms to false
        signal.value(null);
        assertFalse(element.isEnabled());
    }

    @Test
    public void bindEnabled_elementNotAttached_bindingInactive() {
        Element element = new Element("foo");
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        element.bindEnabled(signal);
        signal.value(false);

        assertTrue(element.isEnabled());
    }

    @Test
    public void bindEnabled_elementDetached_bindingInactive() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        element.bindEnabled(signal);
        element.removeFromParent();
        signal.value(false); // ignored

        assertTrue(element.isEnabled());
    }

    @Test
    public void bindEnabled_elementReAttached_bindingActivate() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        element.bindEnabled(signal);
        element.removeFromParent();
        signal.value(false);
        UI.getCurrent().getElement().appendChild(element);

        assertFalse(element.isEnabled());
    }

    @Test
    public void bindEnabled_setEnabledAndBindEnabledWhileBindingIsActive_throwException() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);
        element.bindEnabled(new ValueSignal<>(true));

        assertThrows(BindingActiveException.class,
                () -> element.setEnabled(false));
        assertThrows(BindingActiveException.class,
                () -> element.bindEnabled(new ValueSignal<>(true)));
        assertTrue(element.isEnabled());
    }

    @Test
    public void bindEnabled_withNullBinding_removesBinding() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        element.bindEnabled(signal);
        assertTrue(element.isEnabled());

        element.bindEnabled(null); // remove binding
        signal.value(false); // no effect
        assertTrue(element.isEnabled());
    }

    @Test
    public void bindEnabled_lazyInitSignalBindingFeature() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);
        element.setEnabled(false);
        element.isEnabled();
        element.getNode().getFeatureIfInitialized(SignalBindingFeature.class)
                .ifPresent(feature -> Assert.fail(
                        "SignalBindingFeature should not be initialized before binding a signal"));

        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        element.bindEnabled(signal);

        element.getNode().getFeatureIfInitialized(SignalBindingFeature.class)
                .orElseThrow(() -> new AssertionError(
                        "SignalBindingFeature should be initialized after binding a signal"));
    }

    @Test
    public void bindEnabled_implicitlyDisabledComponent_isEnabledReturnsFalse() {
        TestComponent component = new TestComponent();
        component.bindEnabled(new ValueSignal<>(true));

        TestComponent parent = new TestComponent();
        parent.setEnabled(false);

        parent.add(component);
        UI.getCurrent().add(parent);

        assertFalse(component.isEnabled());
    }

    @Test
    public void bindEnabled_implicitlyDisabledComponent_detach_componentBecomesEnabled() {
        TestComponent component = new TestComponent();
        component.bindEnabled(new ValueSignal<>(true));

        TestComponent parent = new TestComponent();
        parent.add(component);
        UI.getCurrent().add(parent);

        parent.setEnabled(false);

        parent.remove(component);

        assertTrue(component.isEnabled());
    }

    @Test
    public void bindEnabled_explicitlyDisabledComponent_enableParent_componentRemainsDisabled() {
        TestComponent component = new TestComponent();
        component.bindEnabled(new ValueSignal<>(false));

        TestComponent parent = new TestComponent();
        parent.add(component);
        UI.getCurrent().add(parent);

        parent.setEnabled(false);

        assertFalse(component.isEnabled());

        parent.setEnabled(true);

        assertFalse(component.isEnabled());
    }

    @Tag(Tag.DIV)
    private static class TestComponent extends Component
            implements HasComponents {

    }
}
