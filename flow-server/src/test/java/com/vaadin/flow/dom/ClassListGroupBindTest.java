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
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for binding a group of CSS class names to a Signal using
 * ClassList.bind(Signal&lt;List&lt;String&gt;&gt;).
 */
class ClassListGroupBindTest extends SignalsUnitTest {

    @Test
    public void basicGroupBinding_addsAndRemovesClasses() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("a", "b"));
        element.getClassList().bind(signal);

        assertTrue(element.getClassList().contains("a"));
        assertTrue(element.getClassList().contains("b"));

        signal.set(List.of("c"));
        assertFalse(element.getClassList().contains("a"));
        assertFalse(element.getClassList().contains("b"));
        assertTrue(element.getClassList().contains("c"));

        signal.set(List.of());
        assertFalse(element.getClassList().contains("c"));
    }

    @Test
    public void nullList_treatedAsEmpty() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<List<String>> signal = new ValueSignal<>(null);
        element.getClassList().bind(signal);

        assertEquals(0, element.getClassList().size());
    }

    @Test
    public void nullAndEmptyEntriesInList_silentlyIgnored() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<List<String>> signal = new ValueSignal<>(
                Arrays.asList("a", null, "", "b"));
        element.getClassList().bind(signal);

        assertTrue(element.getClassList().contains("a"));
        assertTrue(element.getClassList().contains("b"));
        assertEquals(2, element.getClassList().size());
    }

    @Test
    public void secondGroupBind_throwsBindingActiveException() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<List<String>> s1 = new ValueSignal<>(List.of("a"));
        ValueSignal<List<String>> s2 = new ValueSignal<>(List.of("b"));

        element.getClassList().bind(s1);
        assertThrows(BindingActiveException.class,
                () -> element.getClassList().bind(s2));
    }

    @Test
    public void coexistenceWithStaticAdd_bothPresent() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        element.getClassList().add("static");

        ValueSignal<List<String>> signal = new ValueSignal<>(
                List.of("dynamic"));
        element.getClassList().bind(signal);

        assertTrue(element.getClassList().contains("static"));
        assertTrue(element.getClassList().contains("dynamic"));
    }

    @Test
    public void coexistenceWithStaticAdd_groupRemovalRemovesFromFlatList() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        element.getClassList().add("shared");

        ValueSignal<List<String>> signal = new ValueSignal<>(
                List.of("shared", "extra"));
        element.getClassList().bind(signal);

        assertTrue(element.getClassList().contains("shared"));
        assertTrue(element.getClassList().contains("extra"));

        // Remove "shared" from group signal - since classList uses a flat
        // NodeList with deduplication, removing from the group binding also
        // removes the only entry, even if it was statically added.
        signal.set(List.of("extra"));
        assertFalse(element.getClassList().contains("shared"));
        assertTrue(element.getClassList().contains("extra"));
    }

    @Test
    public void coexistenceWithToggleBind_bothWorkIndependently() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<Boolean> toggle = new ValueSignal<>(true);
        element.getClassList().bind("toggled", toggle);

        ValueSignal<List<String>> group = new ValueSignal<>(List.of("grouped"));
        element.getClassList().bind(group);

        assertTrue(element.getClassList().contains("toggled"));
        assertTrue(element.getClassList().contains("grouped"));

        toggle.set(false);
        assertFalse(element.getClassList().contains("toggled"));
        assertTrue(element.getClassList().contains("grouped"));

        group.set(List.of());
        assertFalse(element.getClassList().contains("toggled"));
        assertFalse(element.getClassList().contains("grouped"));
    }

    @Test
    public void clear_clearsGroupBindingAndAllClasses() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("a", "b"));
        element.getClassList().bind(signal);

        assertTrue(element.getClassList().contains("a"));

        element.getClassList().clear();

        assertFalse(element.getClassList().contains("a"));
        assertFalse(element.getClassList().contains("b"));

        // Binding is cleared, signal changes have no effect
        signal.set(List.of("c"));
        assertFalse(element.getClassList().contains("c"));
    }

    @Test
    public void detachAndReattach_reappliesCurrentValue() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<List<String>> signal = new ValueSignal<>(
                List.of("initial"));
        element.getClassList().bind(signal);
        assertTrue(element.getClassList().contains("initial"));

        // Detach
        UI.getCurrent().getElement().removeChild(element);

        // Change while detached
        signal.set(List.of("updated"));
        assertFalse(element.getClassList().contains("updated"));

        // Reattach
        UI.getCurrent().getElement().appendChild(element);
        assertTrue(element.getClassList().contains("updated"));
        assertFalse(element.getClassList().contains("initial"));
    }

    @Test
    public void incrementalUpdates_diffAppliedCorrectly() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("a", "b"));
        element.getClassList().bind(signal);

        assertTrue(element.getClassList().contains("a"));
        assertTrue(element.getClassList().contains("b"));

        // Change to ["b", "c"] — "a" removed, "c" added, "b" stays
        signal.set(List.of("b", "c"));
        assertFalse(element.getClassList().contains("a"));
        assertTrue(element.getClassList().contains("b"));
        assertTrue(element.getClassList().contains("c"));
    }

    @Test
    public void bindClassNames_shorthandOnHasStyle() {
        HasStyleComponent component = new HasStyleComponent();
        UI.getCurrent().add(component);

        ValueSignal<List<String>> signal = new ValueSignal<>(List.of("x", "y"));
        component.bindClassNames(signal);

        assertTrue(component.hasClassName("x"));
        assertTrue(component.hasClassName("y"));

        signal.set(List.of("z"));
        assertFalse(component.hasClassName("x"));
        assertTrue(component.hasClassName("z"));
    }

    @Tag("div")
    private static class HasStyleComponent extends Component
            implements HasStyle {
    }
}
