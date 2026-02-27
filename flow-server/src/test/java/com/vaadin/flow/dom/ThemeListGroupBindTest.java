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
    public void basicGroupBinding_addsAndRemovesThemes() {
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
    public void nullList_treatedAsEmpty() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<String>> signal = new ValueSignal<>(null);
        component.bindThemeNames(signal);

        assertNull(component.getThemeName());
    }

    @Test
    public void nullAndEmptyEntriesInList_silentlyIgnored() {
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
    public void secondGroupBind_throwsBindingActiveException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<String>> s1 = new ValueSignal<>(List.of("a"));
        ValueSignal<List<String>> s2 = new ValueSignal<>(List.of("b"));

        component.getThemeNames().bind(s1);
        assertThrows(BindingActiveException.class,
                () -> component.getThemeNames().bind(s2));
    }

    @Test
    public void duplicateInAttribute_staticAndGroupBothContribute() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        component.addThemeName("wrap");

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("wrap"));
        component.bindThemeNames(signal);

        // Both sources contribute "wrap", so the attribute contains it twice
        String attr = component.getElement().getAttribute("theme");
        long count = Arrays.stream(attr.split(" ")).filter("wrap"::equals)
                .count();
        assertEquals(2, count);
    }

    @Test
    public void staticRemovalWhileGroupHasSameName_groupNameRemains() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        component.addThemeName("shared");

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("shared"));
        component.bindThemeNames(signal);

        assertTrue(component.hasThemeName("shared"));

        // Remove from static source
        component.removeThemeName("shared");

        // Group binding still has it
        assertTrue(component.hasThemeName("shared"));
    }

    @Test
    public void groupRemovalWhileStaticHasSameName_staticNameRemains() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        component.addThemeName("shared");

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("shared"));
        component.bindThemeNames(signal);

        // Remove from group signal
        signal.set(List.of());

        // Static source still has it
        assertTrue(component.hasThemeName("shared"));
    }

    @Test
    public void clear_clearsGroupBindingAndAllThemes() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("a", "b"));
        component.bindThemeNames(signal);

        assertTrue(component.hasThemeName("a"));

        component.getThemeNames().clear();

        assertFalse(component.hasThemeName("a"));
        assertFalse(component.hasThemeName("b"));

        // Binding is cleared, signal changes have no effect
        signal.set(List.of("c"));
        assertFalse(component.hasThemeName("c"));
    }

    @Test
    public void detachAndReattach_reappliesCurrentValue() {
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
    public void coexistenceWithToggleBind_bothWorkIndependently() {
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

    @Tag("span")
    private static class TestComponent extends Component implements HasTheme {
    }
}
