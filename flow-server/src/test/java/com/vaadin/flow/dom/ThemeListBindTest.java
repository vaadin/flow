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

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
        Assertions.assertFalse(component.hasThemeName("light"));

        signal.set(true);
        Assertions.assertTrue(component.hasThemeName("light"));

        signal.set(false);
        Assertions.assertFalse(component.hasThemeName("light"));
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
        Assertions.assertFalse(component.hasThemeName("active"));

        // Reattach – current value true should be applied
        UI.getCurrent().add(component);
        Assertions.assertTrue(component.hasThemeName("active"));
    }

    @Test
    public void manualAddRemoveForBoundName_throwsBindingActiveException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindThemeName("locked", signal);

        Assertions.assertThrows(BindingActiveException.class,
                () -> component.addThemeName("locked"));
        Assertions.assertThrows(BindingActiveException.class,
                () -> component.removeThemeName("locked"));
        Assertions.assertThrows(BindingActiveException.class,
                () -> component.setThemeName("locked", true));
        Assertions.assertThrows(BindingActiveException.class,
                () -> component.setThemeName("locked", false));
        Assertions.assertThrows(BindingActiveException.class,
                () -> component.addThemeNames("locked", "open"));
        Assertions.assertThrows(BindingActiveException.class,
                () -> component.removeThemeNames("locked", "open"));
        Assertions.assertThrows(BindingActiveException.class,
                () -> component.getThemeNames().retainAll(Set.of("open")));

        component.addThemeName("open");
        Assertions.assertTrue(
                component.getThemeNames().retainAll(Set.of("locked")));
    }

    @Test
    public void clear_clearsBindingsSilently_andClearsThemes() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> a = new ValueSignal<>(true);
        ValueSignal<Boolean> b = new ValueSignal<>(true);
        component.bindThemeName("a", a);
        component.bindThemeName("b", b);

        Assertions.assertTrue(component.hasThemeName("a"));
        Assertions.assertTrue(component.hasThemeName("b"));

        component.getThemeNames().clear();

        // Themes cleared
        Assertions.assertFalse(component.hasThemeName("a"));
        Assertions.assertFalse(component.hasThemeName("b"));

        // Toggling signals has no effect (bindings were cleared)
        a.set(false);
        b.set(false);
        a.set(true);
        b.set(true);
        Assertions.assertFalse(component.hasThemeName("a"));
        Assertions.assertFalse(component.hasThemeName("b"));
        Assertions.assertFalse(component.getThemeNames().iterator().hasNext());
    }

    @Test
    public void setThemeName_bulkReplacement_clearsBindingsSilently() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> bound = new ValueSignal<>(true);
        component.bindThemeName("flag", bound);
        Assertions.assertTrue(component.hasThemeName("flag"));

        // Bulk replace via setThemeName.
        // Note that setting theme attribute directly can't clear bindings due
        // to Element API's setAttribute don't know anything about special
        // meaning of 'theme' attribute. This could be improved in the future,
        // but for now, setThemeName supports clearing bindings.
        component.setThemeName("foo");
        Assertions.assertTrue(component.hasThemeName("foo"));
        Assertions.assertFalse(component.hasThemeName("flag"));

        // Binding should be cleared, so toggling has no effect
        bound.set(false);
        bound.set(true);
        Assertions.assertFalse(component.hasThemeName("flag"));
    }

    @Test
    public void bind_removeBindingViaFeature_stopsUpdatesAndAllowsManualSet() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindThemeName("badge", signal);
        Assertions.assertTrue(component.hasThemeName("badge"));

        // Remove binding via the node's SignalBindingFeature
        SignalBindingFeature feature = component.getElement().getNode()
                .getFeature(SignalBindingFeature.class);
        feature.removeBinding(SignalBindingFeature.THEMES + "badge");

        // Signal changes should no longer affect the theme list
        signal.set(false);
        Assertions.assertTrue(component.hasThemeName("badge"));

        // Manual set should work without throwing
        component.removeThemeName("badge");
        Assertions.assertFalse(component.hasThemeName("badge"));
    }

    @Test
    public void bind_nullSignal_throwsNPE() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        Assertions.assertThrows(NullPointerException.class,
                () -> component.bindThemeName("badge", null));
    }

    @Test
    public void rebinding_alreadyBound_throws() {
        assertThrows(BindingActiveException.class, () -> {
            TestComponent component = new TestComponent();
            UI.getCurrent().add(component);
            ValueSignal<Boolean> s1 = new ValueSignal<>(true);
            ValueSignal<Boolean> s2 = new ValueSignal<>(false);

            component.bindThemeName("tag", s1);
            Assertions.assertTrue(component.hasThemeName("tag"));

            // Rebind to a new signal
            component.bindThemeName("tag", s2);
        });
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
        Assertions.assertTrue(component.hasThemeName("spin"));
        Assertions.assertEquals(1, component.getThemeNames().stream()
                .filter("spin"::equals).count());

        signal.set(false);
        signal.set(false); // no-op update
        Assertions.assertFalse(component.hasThemeName("spin"));
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

        Assertions.assertTrue(component.hasThemeName("a"));
        Assertions.assertFalse(component.hasThemeName("b"));
        Assertions.assertFalse(component.hasThemeName("b"));

        signal.set(DummyEnum.TWO);
        Assertions.assertFalse(component.hasThemeName("a"));
        Assertions.assertTrue(component.hasThemeName("b"));
        Assertions.assertFalse(component.hasThemeName("c"));

        signal.set(DummyEnum.THREE);
        Assertions.assertFalse(component.hasThemeName("a"));
        Assertions.assertFalse(component.hasThemeName("b"));
        Assertions.assertTrue(component.hasThemeName("c"));
    }

    @Tag("span")
    private static class TestComponent extends Component implements HasTheme {
    }
}
