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

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for binding theme attribute presence to a Signal using ThemeList.bind.
 */
class ThemeListBindTest extends SignalsUnitTest {

    @Test
    public void bindingMirrorsSignalWhileAttached_toggleAddsRemovesTheme() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        component.bindThemeName("light", signal);

        // Initially false -> not present
        assertFalse(component.hasThemeName("light"));

        signal.set(true);
        assertTrue(component.hasThemeName("light"));

        signal.set(false);
        assertFalse(component.hasThemeName("light"));
    }

    @Test
    public void bindingInactiveWhenDetached_reactivatedOnAttach_appliesCurrentValue() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        component.bindThemeName("active", signal);

        // Detach element
        UI.getCurrent().remove(component);

        // Change signal while detached – should NOT apply
        signal.set(true);
        assertFalse(component.hasThemeName("active"));

        // Reattach – current value true should be applied
        UI.getCurrent().add(component);
        assertTrue(component.hasThemeName("active"));
    }

    @Test
    public void manualAddRemoveForBoundName_throwsBindingActiveException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindThemeName("locked", signal);

        assertThrows(BindingActiveException.class,
                () -> component.addThemeName("locked"));
        assertThrows(BindingActiveException.class,
                () -> component.removeThemeName("locked"));
        assertThrows(BindingActiveException.class,
                () -> component.setThemeName("locked", true));
        assertThrows(BindingActiveException.class,
                () -> component.setThemeName("locked", false));
        assertThrows(BindingActiveException.class,
                () -> component.addThemeNames("locked", "open"));
        assertThrows(BindingActiveException.class,
                () -> component.removeThemeNames("locked", "open"));
        assertThrows(BindingActiveException.class,
                () -> component.getThemeNames().retainAll(Set.of("open")));

        component.addThemeName("open");
        assertTrue(component.getThemeNames().retainAll(Set.of("locked")));
    }

    @Test
    public void clear_throwsWhenBindingsActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindThemeName("a", signal);

        assertThrows(BindingActiveException.class,
                () -> component.getThemeNames().clear());
    }

    @Test
    public void setThemeName_throwsWhenBindingsActive() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> bound = new ValueSignal<>(true);
        component.bindThemeName("flag", bound);
        assertTrue(component.hasThemeName("flag"));

        assertThrows(BindingActiveException.class,
                () -> component.setThemeName("foo"));
    }

    @Test
    public void bind_nullSignal_throwsNPE() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        assertThrows(NullPointerException.class,
                () -> component.bindThemeName("badge", null));
    }

    @Test
    public void rebinding_alreadyBound_throws() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> s1 = new ValueSignal<>(true);
        ValueSignal<Boolean> s2 = new ValueSignal<>(false);

        component.bindThemeName("tag", s1);
        assertTrue(component.hasThemeName("tag"));

        // Rebind to a new signal
        assertThrows(BindingActiveException.class,
                () -> component.bindThemeName("tag", s2));
    }

    @Test
    public void internalUpdatesDoNotThrowOrRecurse() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        component.bindThemeName("spin", signal);

        // Flip to true a couple of times; should not throw and should not
        // duplicate theme entries.
        signal.set(true);
        signal.set(true); // no-op update
        assertTrue(component.hasThemeName("spin"));
        assertEquals(1, component.getThemeNames().stream()
                .filter("spin"::equals).count());

        signal.set(false);
        signal.set(false); // no-op update
        assertFalse(component.hasThemeName("spin"));
    }

    @Test
    public void bindMultipleSignals() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        enum DummyEnum {
            ONE, TWO, THREE
        }
        ValueSignal<DummyEnum> signal = new ValueSignal<>(DummyEnum.ONE);

        Signal<Boolean> a = signal.map(v -> v == DummyEnum.ONE);
        Signal<Boolean> b = signal.map(v -> v == DummyEnum.TWO);
        Signal<Boolean> c = signal.map(v -> v == DummyEnum.THREE);
        component.bindThemeName("a", a);
        component.bindThemeName("b", b);
        component.bindThemeName("c", c);

        assertTrue(component.hasThemeName("a"));
        assertFalse(component.hasThemeName("b"));
        assertFalse(component.hasThemeName("b"));

        signal.set(DummyEnum.TWO);
        assertFalse(component.hasThemeName("a"));
        assertTrue(component.hasThemeName("b"));
        assertFalse(component.hasThemeName("c"));

        signal.set(DummyEnum.THREE);
        assertFalse(component.hasThemeName("a"));
        assertFalse(component.hasThemeName("b"));
        assertTrue(component.hasThemeName("c"));
    }

    @Tag("span")
    private static class TestComponent extends Component implements HasTheme {
    }
}
