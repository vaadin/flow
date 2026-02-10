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
package com.vaadin.flow.dom;

import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

/**
 * Unit tests for Style.bind(String, Signal<String>).
 */
class StyleBindTest {

    private static MockVaadinServletService service;

    @BeforeAll
    public static void init() {
        service = new MockVaadinServletService();
    }

    @AfterAll
    public static void clean() {
        VaadinService.setCurrent(null);
        service.destroy();
    }

    @BeforeEach
    public void before() {
        mockLockedSessionWithErrorHandler();
    }

    @AfterEach
    public void after() {
        VaadinService.setCurrent(null);
    }

    // Lifecycle: applies on attachment and signal changes when attached
    @Test
    public void bindingMirrorsSignalWhileAttached_updatesStyleValue() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> color = new ValueSignal<>("red");
        element.getStyle().bind("background-color", color);

        Assertions.assertEquals("red",
                element.getStyle().get("backgroundColor"));

        color.set("blue");
        Assertions.assertEquals("blue", element.getStyle().get("backgroundColor"));

        // Null removes the style
        color.set(null);
        Assertions.assertNull(element.getStyle().get("backgroundColor"));
    }

    // Lifecycle: no updates while detached; lastApplied preserved across
    // detach/attachment
    @Test
    public void detached_noUpdates_lastAppliedPreservedOnReattach() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> color = new ValueSignal<>("red");
        element.getStyle().bind("background-color", color);
        Assertions.assertEquals("red",
                element.getStyle().get("backgroundColor"));

        // Detach
        UI.getCurrent().getElement().removeChild(element);

        // Change while detached -> should not apply
        color.set("green");
        Assertions.assertEquals("red", element.getStyle().get("backgroundColor"));

        // Reattach -> current signal value should be applied
        UI.getCurrent().getElement().appendChild(element);
        Assertions.assertEquals("green",
                element.getStyle().get("backgroundColor"));
    }

    // Conflict prevention: set/remove throw while binding is active
    @Test
    public void conflict_setRemoveThrowWhileBoundAndActive() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> color = new ValueSignal<>("red");
        element.getStyle().bind("background-color", color);

        Assertions.assertThrows(BindingActiveException.class,
                () -> element.getStyle().set("background-color", "black"));
        Assertions.assertThrows(BindingActiveException.class,
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

        Assertions.assertEquals("1", element.getStyle().get("borderTopWidth"));
        Assertions.assertEquals("2",
                element.getStyle().get("borderBottomWidth"));

        element.getStyle().clear();

        // Cleared
        Assertions.assertNull(element.getStyle().get("borderTopWidth"));
        Assertions.assertNull(element.getStyle().get("borderBottomWidth"));

        // Toggling signals should have no effect
        a.set("3");
        b.set("4");
        Assertions.assertNull(element.getStyle().get("borderTopWidth"));
        Assertions.assertNull(element.getStyle().get("borderBottomWidth"));
    }

    @Test
    public void bind_nullSignal_throwsNPE() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        Assertions.assertThrows(NullPointerException.class,
                () -> element.getStyle().bind("background-color", null));
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
        Assertions.assertEquals("10px", element.getStyle().get("marginTop"));
        b.set(null);
        Assertions.assertNull(element.getStyle().get("marginBottom"));

        // getNames should include names that have recorded last-applied values.
        // BasicElementStyle currently exposes attribute names (dash-separated).
        Set<String> names = element.getStyle().getNames()
                .collect(Collectors.toSet());
        Assertions.assertTrue(names.contains("margin-top"));
        // b has a null last-applied value, but the binding is preserved -> name
        // present
        Assertions.assertTrue(names.contains("margin-bottom"));

        // Detach before any applying for c -> bind while detached -> no value
        // applied yet, get returns null
        ValueSignal<String> c = new ValueSignal<>("5px");
        UI.getCurrent().getElement().removeChild(element);
        element.getStyle().bind("padding-top", c);
        Assertions.assertNull(element.getStyle().get("paddingTop"));
        names = element.getStyle().getNames().collect(Collectors.toSet());
        // The current implementation records the binding name even before first
        // attach
        Assertions.assertTrue(names.contains("padding-top"));
    }

    @Test
    public void nullSignalValue_removesStyleAndHasReturnsFalse() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<String> color = new ValueSignal<>("rgba(255, 0, 0, 1)");
        element.getStyle().bind("background-color", color);
        Assertions.assertEquals("rgba(255, 0, 0, 1)",
                element.getStyle().get("backgroundColor"));

        // Set null -> should remove the style and has() should report false
        color.set(null);
        Assertions.assertNull(element.getStyle().get("backgroundColor"));
        Assertions.assertFalse(element.getStyle().has("background-color"));

        // but it preserves the signal binding
        Set<String> names = element.getStyle().getNames()
                .collect(Collectors.toSet());
        Assertions.assertTrue(names.contains("background-color"));
        color.set("rgba(0, 0, 255, 1)");
        Assertions.assertEquals("rgba(0, 0, 255, 1)",
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
}
