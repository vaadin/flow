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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for binding a group of theme names to a Signal using
 * ThemeList.bind(Signal&lt;List&lt;String&gt;&gt;).
 */
class ThemeListGroupBindTest extends SignalsUnitTest {

    @Test
    void basicGroupBinding_addsAndRemovesThemes() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("a", "b"));
        component.bindThemeNames(signal);

        assertTrue(component.hasThemeName("a"));
        assertTrue(component.hasThemeName("b"));

        signal.set(List.of("c"));
        assertFalse(component.hasThemeName("a"));
        assertFalse(component.hasThemeName("b"));
        assertTrue(component.hasThemeName("c"));

        signal.set(List.of());
        assertFalse(component.hasThemeName("c"));
    }

    @Test
    void nullList_treatedAsEmpty() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<String>> signal = new ValueSignal<>(null);
        component.bindThemeNames(signal);

        assertNull(component.getThemeName());
    }

    @Test
    void nullAndEmptyEntriesInList_silentlyIgnored() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<String>> signal = new ValueSignal<>(
                Arrays.asList("a", null, "", "b"));
        component.bindThemeNames(signal);

        assertTrue(component.hasThemeName("a"));
        assertTrue(component.hasThemeName("b"));
        assertEquals(2, component.getThemeNames().size());
    }

    @Test
    void secondGroupBind_throwsBindingActiveException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<String>> s1 = new ValueSignal<>(List.of("a"));
        ValueSignal<List<String>> s2 = new ValueSignal<>(List.of("b"));

        component.getThemeNames().bind(s1);
        assertThrows(BindingActiveException.class,
                () -> component.getThemeNames().bind(s2));
    }

    @Test
    void duplicateInAttribute_staticAndGroupAreDeduplicated() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        component.addThemeName("wrap");

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("wrap"));
        component.bindThemeNames(signal);

        // Group names go into the same backing set, so duplicates are merged
        String attr = component.getElement().getAttribute("theme");
        long count = Arrays.stream(attr.split(" ")).filter("wrap"::equals)
                .count();
        assertEquals(1, count);
    }

    @Test
    void staticRemovalWhileGroupHasSameName_nameIsRemoved() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        component.addThemeName("shared");

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("shared"));
        component.bindThemeNames(signal);

        assertTrue(component.hasThemeName("shared"));

        // Remove from static source — single backing store, so name is gone
        component.removeThemeName("shared");

        assertFalse(component.hasThemeName("shared"));
    }

    @Test
    void groupRemovalWhileStaticHasSameName_nameIsRemoved() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        component.addThemeName("shared");

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("shared"));
        component.bindThemeNames(signal);

        // Remove from group signal — single backing store, so name is gone
        signal.set(List.of());

        assertFalse(component.hasThemeName("shared"));
    }

    @Test
    void clear_throwsWhenGroupBindingActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("a", "b"));
        component.bindThemeNames(signal);

        assertTrue(component.hasThemeName("a"));

        assertThrows(BindingActiveException.class,
                () -> component.getThemeNames().clear());
    }

    @Test
    void detachAndReattach_reappliesCurrentValue() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<String>> signal = new ValueSignal<>(
                List.of("initial"));
        component.bindThemeNames(signal);
        assertTrue(component.hasThemeName("initial"));

        // Detach
        UI.getCurrent().remove(component);

        // Change while detached
        signal.set(List.of("updated"));
        assertFalse(component.hasThemeName("updated"));

        // Reattach
        UI.getCurrent().add(component);
        assertTrue(component.hasThemeName("updated"));
        assertFalse(component.hasThemeName("initial"));
    }

    @Test
    void coexistenceWithToggleBind_bothWorkIndependently() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> toggle = new ValueSignal<>(true);
        component.bindThemeName("toggled", toggle);

        ValueSignal<List<String>> group = new ValueSignal<>(List.of("grouped"));
        component.bindThemeNames(group);

        assertTrue(component.hasThemeName("toggled"));
        assertTrue(component.hasThemeName("grouped"));

        toggle.set(false);
        assertFalse(component.hasThemeName("toggled"));
        assertTrue(component.hasThemeName("grouped"));

        group.set(List.of());
        assertFalse(component.hasThemeName("toggled"));
        assertFalse(component.hasThemeName("grouped"));
    }

    @Test
    void bind_onChange_receivesBindingContext() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("a", "b"));
        List<BindingContext<List<String>>> contexts = new ArrayList<>();

        component.getThemeNames().bind(signal).onChange(contexts::add);

        // onChange should have been called once initially
        assertEquals(1, contexts.size());

        signal.set(List.of("c"));

        assertEquals(2, contexts.size());
        BindingContext<List<String>> ctx = contexts.get(1);
        assertFalse(ctx.isInitialRun());
        assertEquals(List.of("a", "b"), ctx.getOldValue());
        assertEquals(List.of("c"), ctx.getNewValue());
        assertEquals(component.getElement(), ctx.getElement());
    }

    @Tag("span")
    private static class TestComponent extends Component implements HasTheme {
    }
}
