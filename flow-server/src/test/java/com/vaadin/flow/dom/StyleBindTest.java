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
import java.util.Set;
import java.util.stream.Collectors;

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
 * Unit tests for Style.bind(String, Signal<String>).
 */
public class StyleBindTest {

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

    // Lifecycle: applies on attach and signal changes when attached
    @Test
    public void bindingMirrorsSignalWhileAttached_updatesStyleValue() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> color = new ValueSignal<>("red");
        element.getStyle().bind("background-color", color);

        Assert.assertEquals("red", element.getStyle().get("backgroundColor"));

        color.value("blue");
        Assert.assertEquals("blue", element.getStyle().get("backgroundColor"));

        // Null removes the style
        color.value(null);
        Assert.assertNull(element.getStyle().get("backgroundColor"));
    }

    // Lifecycle: no updates while detached; lastApplied preserved across
    // detach/attach
    @Test
    public void detached_noUpdates_lastAppliedPreservedOnReattach() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> color = new ValueSignal<>("red");
        element.getStyle().bind("background-color", color);
        Assert.assertEquals("red", element.getStyle().get("backgroundColor"));

        // Detach
        UI.getCurrent().getElement().removeChild(element);

        // Change while detached -> should not apply
        color.value("green");
        Assert.assertEquals("red", element.getStyle().get("backgroundColor"));

        // Reattach -> current signal value should be applied
        UI.getCurrent().getElement().appendChild(element);
        Assert.assertEquals("green", element.getStyle().get("backgroundColor"));
    }

    // Conflict prevention: set/remove throw while binding is active
    @Test
    public void conflict_setRemoveThrowWhileBoundAndActive() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> color = new ValueSignal<>("red");
        element.getStyle().bind("background-color", color);

        Assert.assertThrows(BindingActiveException.class,
                () -> element.getStyle().set("background-color", "black"));
        Assert.assertThrows(BindingActiveException.class,
                () -> element.getStyle().remove("background-color"));
    }

    // Bulk operations: clear() clears styles and stops updates
    @Test
    public void clear_removesBindingsAndStopsUpdates() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> a = new ValueSignal<>("1");
        ValueSignal<String> b = new ValueSignal<>("2");
        element.getStyle().bind("border-top-width", a);
        element.getStyle().bind("border-bottom-width", b);

        Assert.assertEquals("1", element.getStyle().get("borderTopWidth"));
        Assert.assertEquals("2", element.getStyle().get("borderBottomWidth"));

        element.getStyle().clear();

        // Cleared
        Assert.assertNull(element.getStyle().get("borderTopWidth"));
        Assert.assertNull(element.getStyle().get("borderBottomWidth"));

        // Toggling signals should have no effect
        a.value("3");
        b.value("4");
        Assert.assertNull(element.getStyle().get("borderTopWidth"));
        Assert.assertNull(element.getStyle().get("borderBottomWidth"));
    }

    // Unbinding via null does not force removal
    @Test
    public void bindNull_unbindsWithoutForcingRemoval() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> color = new ValueSignal<>("red");
        element.getStyle().bind("background-color", color);
        Assert.assertEquals("red", element.getStyle().get("backgroundColor"));

        // Unbind
        element.getStyle().bind("background-color", null);

        // Value remains as-is
        Assert.assertEquals("red", element.getStyle().get("backgroundColor"));

        // Further changes do not affect
        color.value("blue");
        Assert.assertEquals("red", element.getStyle().get("backgroundColor"));
    }

    // Getters semantics
    @Test
    public void getters_returnLastAppliedAndNamesWithValues() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> a = new ValueSignal<>("10px");
        ValueSignal<String> b = new ValueSignal<>("initial");
        element.getStyle().bind("margin-top", a);
        element.getStyle().bind("margin-bottom", b);

        // a applied, then set b to null which should remove the style
        Assert.assertEquals("10px", element.getStyle().get("marginTop"));
        b.value(null);
        Assert.assertNull(element.getStyle().get("marginBottom"));

        // getNames should include names that have recorded last-applied values.
        // BasicElementStyle currently exposes attribute names (dash-separated).
        Set<String> names = element.getStyle().getNames()
                .collect(Collectors.toSet());
        Assert.assertTrue(names.contains("margin-top"));
        // b has null last-applied value, but the binding is preserved -> name
        // present
        Assert.assertTrue(names.contains("margin-bottom"));

        // Detach before any apply for c -> bind while detached -> no value
        // applied yet, get returns null
        ValueSignal<String> c = new ValueSignal<>("5px");
        UI.getCurrent().getElement().removeChild(element);
        element.getStyle().bind("padding-top", c);
        Assert.assertNull(element.getStyle().get("paddingTop"));
        names = element.getStyle().getNames().collect(Collectors.toSet());
        // Current implementation records the binding name even before first
        // attach
        Assert.assertTrue(names.contains("padding-top"));
    }

    @Test
    public void nullSignalValue_removesStyleAndHasReturnsFalse() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> color = new ValueSignal<>("rgba(255, 0, 0, 1)");
        element.getStyle().bind("background-color", color);
        Assert.assertEquals("rgba(255, 0, 0, 1)",
                element.getStyle().get("backgroundColor"));

        // Set null -> should remove the style and has() should report false
        color.value(null);
        Assert.assertNull(element.getStyle().get("backgroundColor"));
        Assert.assertFalse(element.getStyle().has("background-color"));

        // but it preserves the signal binding
        Set<String> names = element.getStyle().getNames()
                .collect(Collectors.toSet());
        Assert.assertTrue(names.contains("background-color"));
        color.value("rgba(0, 0, 255, 1)");
        Assert.assertEquals("rgba(0, 0, 255, 1)",
                element.getStyle().get("backgroundColor"));
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
