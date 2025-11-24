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
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for binding CSS class presence to a Signal using ClassList.bind.
 */
public class ClassListBindTest {

    private static MockVaadinServletService service;

    private MockedStatic<FeatureFlags> featureFlagStaticMock;

    @BeforeClass
    public static void init() {
        MockedStatic<FeatureFlags> staticMock = Mockito
                .mockStatic(FeatureFlags.class);
        featureFlagEnabled(staticMock);
        service = new MockVaadinServletService();
        close(staticMock);
    }

    @AfterClass
    public static void clean() {
        VaadinService.setCurrent(null);
        service.destroy();
    }

    @Before
    public void before() {
        featureFlagStaticMock = Mockito.mockStatic(FeatureFlags.class);
        featureFlagEnabled(featureFlagStaticMock);
        mockLockedSessionWithErrorHandler();
    }

    @After
    public void after() {
        close(featureFlagStaticMock);
        VaadinService.setCurrent(null);
    }

    @Test
    public void bindingMirrorsSignalWhileAttached_toggleAddsRemovesClass() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        element.getClassList().bind("highlight", signal);

        // Initially false -> not present
        Assert.assertFalse(element.getClassList().contains("highlight"));

        signal.value(true);
        Assert.assertTrue(element.getClassList().contains("highlight"));

        signal.value(false);
        Assert.assertFalse(element.getClassList().contains("highlight"));
    }

    @Test
    public void bindingInactiveWhenDetached_reactivatedOnAttach_appliesCurrentValue() {
        Element element = new Element("span");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        element.getClassList().bind("active", signal);

        // Detach element
        UI.getCurrent().getElement().removeChild(element);

        // Change signal while detached – should NOT apply
        signal.value(true);
        Assert.assertFalse(element.getClassList().contains("active"));

        // Reattach – current value true should be applied
        UI.getCurrent().getElement().appendChild(element);
        Assert.assertTrue(element.getClassList().contains("active"));
    }

    @Test
    public void manualAddRemoveForBoundName_throwsBindingActiveException() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        element.getClassList().bind("locked", signal);

        Assert.assertThrows(BindingActiveException.class,
                () -> element.getClassList().add("locked"));
        Assert.assertThrows(BindingActiveException.class,
                () -> element.getClassList().remove("locked"));
        Assert.assertThrows(BindingActiveException.class,
                () -> element.getClassList().set("locked", true));
        Assert.assertThrows(BindingActiveException.class,
                () -> element.getClassList().set("locked", false));
    }

    @Test
    public void clear_clearsBindingsSilently_andClearsClasses() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> a = new ValueSignal<>(true);
        ValueSignal<Boolean> b = new ValueSignal<>(true);
        element.getClassList().bind("a", a);
        element.getClassList().bind("b", b);

        Assert.assertTrue(element.getClassList().contains("a"));
        Assert.assertTrue(element.getClassList().contains("b"));

        element.getClassList().clear();

        // Classes cleared
        Assert.assertFalse(element.getClassList().contains("a"));
        Assert.assertFalse(element.getClassList().contains("b"));

        // Toggling signals has no effect (bindings were cleared)
        a.value(false);
        b.value(false);
        a.value(true);
        b.value(true);
        Assert.assertFalse(element.getClassList().contains("a"));
        Assert.assertFalse(element.getClassList().contains("b"));
        Assert.assertFalse(element.getClassList().iterator().hasNext());
    }

    @Test
    public void setAttributeClass_bulkReplacement_clearsBindingsSilently() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> bound = new ValueSignal<>(true);
        element.getClassList().bind("flag", bound);
        Assert.assertTrue(element.getClassList().contains("flag"));

        // Bulk replace via attribute handler
        element.setAttribute("class", "foo bar");
        Assert.assertTrue(element.getClassList().contains("foo"));
        Assert.assertTrue(element.getClassList().contains("bar"));
        Assert.assertFalse(element.getClassList().contains("flag"));

        // Binding should be cleared, so toggling has no effect
        bound.value(false);
        bound.value(true);
        Assert.assertFalse(element.getClassList().contains("flag"));
    }

    @Test
    public void bindNull_unbindsAndKeepsLastAppliedPresence() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        element.getClassList().bind("badge", signal);
        Assert.assertTrue(element.getClassList().contains("badge"));

        // Unbind
        element.getClassList().bind("badge", null);

        // Presence remains as-is
        Assert.assertTrue(element.getClassList().contains("badge"));

        // Further signal changes have no effect
        signal.value(false);
        Assert.assertTrue(element.getClassList().contains("badge"));
    }

    @Test(expected = BindingActiveException.class)
    public void rebinding_alreadyBound_throws() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> s1 = new ValueSignal<>(true);
        ValueSignal<Boolean> s2 = new ValueSignal<>(false);

        element.getClassList().bind("tag", s1);
        Assert.assertTrue(element.getClassList().contains("tag"));

        // Rebind to a new signal
        element.getClassList().bind("tag", s2);
    }

    @Test
    public void internalUpdatesDoNotThrowOrRecurse() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        element.getClassList().bind("spin", signal);

        // Flip to true a couple of times; should not throw and should not
        // duplicate class entries.
        signal.value(true);
        signal.value(true); // no-op update
        Assert.assertTrue(element.getClassList().contains("spin"));
        Assert.assertEquals(1,
                element.getClassList().stream().filter("spin"::equals).count());

        signal.value(false);
        signal.value(false); // no-op update
        Assert.assertFalse(element.getClassList().contains("spin"));
    }

    private void mockLockedSessionWithErrorHandler() {
        VaadinService.setCurrent(service);
        var session = new MockVaadinSession(service);
        session.lock();
        new MockUI(session);
        var list = new LinkedList<ErrorEvent>();
        session.setErrorHandler(list::add);
    }

    private static void featureFlagEnabled(
            MockedStatic<FeatureFlags> featureFlagStaticMock) {
        FeatureFlags flags = Mockito.mock(FeatureFlags.class);
        Mockito.when(
                flags.isEnabled(FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId()))
                .thenReturn(true);
        featureFlagStaticMock.when(() -> FeatureFlags.get(any()))
                .thenReturn(flags);
    }

    private static void close(
            MockedStatic<FeatureFlags> featureFlagStaticMock) {
        featureFlagStaticMock.close();
    }
}
