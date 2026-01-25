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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.signals.SharedValueSignal;
import com.vaadin.signals.core.BindingActiveException;
import com.vaadin.signals.core.Signal;

/**
 * Tests for binding theme attribute presence to a Signal using ThemeList.bind.
 */
public class ThemeListBindTest extends SignalsUnitTest {

    @Test
    public void bindingMirrorsSignalWhileAttached_toggleAddsRemovesTheme() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        SharedValueSignal<Boolean> signal = new SharedValueSignal<>(false);
        component.bindThemeName("light", signal);

        // Initially false -> not present
        Assert.assertFalse(component.hasThemeName("light"));

        signal.value(true);
        Assert.assertTrue(component.hasThemeName("light"));

        signal.value(false);
        Assert.assertFalse(component.hasThemeName("light"));
    }

    @Test
    public void bindingInactiveWhenDetached_reactivatedOnAttach_appliesCurrentValue() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        SharedValueSignal<Boolean> signal = new SharedValueSignal<>(false);
        component.bindThemeName("active", signal);

        // Detach element
        UI.getCurrent().remove(component);

        // Change signal while detached – should NOT apply
        signal.value(true);
        Assert.assertFalse(component.hasThemeName("active"));

        // Reattach – current value true should be applied
        UI.getCurrent().add(component);
        Assert.assertTrue(component.hasThemeName("active"));
    }

    @Test
    public void manualAddRemoveForBoundName_throwsBindingActiveException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        SharedValueSignal<Boolean> signal = new SharedValueSignal<>(true);
        component.bindThemeName("locked", signal);

        Assert.assertThrows(BindingActiveException.class,
                () -> component.addThemeName("locked"));
        Assert.assertThrows(BindingActiveException.class,
                () -> component.removeThemeName("locked"));
        Assert.assertThrows(BindingActiveException.class,
                () -> component.setThemeName("locked", true));
        Assert.assertThrows(BindingActiveException.class,
                () -> component.setThemeName("locked", false));
        Assert.assertThrows(BindingActiveException.class,
                () -> component.addThemeNames("locked", "open"));
        Assert.assertThrows(BindingActiveException.class,
                () -> component.removeThemeNames("locked", "open"));
        Assert.assertThrows(BindingActiveException.class,
                () -> component.getThemeNames().retainAll(Set.of("open")));

        component.addThemeName("open");
        Assert.assertTrue(
                component.getThemeNames().retainAll(Set.of("locked")));
    }

    @Test
    public void clear_clearsBindingsSilently_andClearsThemes() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        SharedValueSignal<Boolean> a = new SharedValueSignal<>(true);
        SharedValueSignal<Boolean> b = new SharedValueSignal<>(true);
        component.bindThemeName("a", a);
        component.bindThemeName("b", b);

        Assert.assertTrue(component.hasThemeName("a"));
        Assert.assertTrue(component.hasThemeName("b"));

        component.getThemeNames().clear();

        // Themes cleared
        Assert.assertFalse(component.hasThemeName("a"));
        Assert.assertFalse(component.hasThemeName("b"));

        // Toggling signals has no effect (bindings were cleared)
        a.value(false);
        b.value(false);
        a.value(true);
        b.value(true);
        Assert.assertFalse(component.hasThemeName("a"));
        Assert.assertFalse(component.hasThemeName("b"));
        Assert.assertFalse(component.getThemeNames().iterator().hasNext());
    }

    @Test
    public void setThemeName_bulkReplacement_clearsBindingsSilently() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        SharedValueSignal<Boolean> bound = new SharedValueSignal<>(true);
        component.bindThemeName("flag", bound);
        Assert.assertTrue(component.hasThemeName("flag"));

        // Bulk replace via setThemeName.
        // Note that setting theme attribute directly can't clear bindings due
        // to Element API's setAttribute don't know anything about special
        // meaning of 'theme' attribute. This could be improved in the future,
        // but for now, setThemeName supports clearing bindings.
        component.setThemeName("foo");
        Assert.assertTrue(component.hasThemeName("foo"));
        Assert.assertFalse(component.hasThemeName("flag"));

        // Binding should be cleared, so toggling has no effect
        bound.value(false);
        bound.value(true);
        Assert.assertFalse(component.hasThemeName("flag"));
    }

    @Test
    public void bindNull_unbindsAndKeepsLastAppliedPresence() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        SharedValueSignal<Boolean> signal = new SharedValueSignal<>(true);
        component.bindThemeName("badge", signal);
        Assert.assertTrue(component.hasThemeName("badge"));

        // Unbind
        component.bindThemeName("badge", null);

        // Presence remains as-is
        Assert.assertTrue(component.hasThemeName("badge"));

        // Further signal changes have no effect
        signal.value(false);
        Assert.assertTrue(component.hasThemeName("badge"));
    }

    @Test(expected = BindingActiveException.class)
    public void rebinding_alreadyBound_throws() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        SharedValueSignal<Boolean> s1 = new SharedValueSignal<>(true);
        SharedValueSignal<Boolean> s2 = new SharedValueSignal<>(false);

        component.bindThemeName("tag", s1);
        Assert.assertTrue(component.hasThemeName("tag"));

        // Rebind to a new signal
        component.bindThemeName("tag", s2);
    }

    @Test
    public void internalUpdatesDoNotThrowOrRecurse() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);
        SharedValueSignal<Boolean> signal = new SharedValueSignal<>(false);
        component.bindThemeName("spin", signal);

        // Flip to true a couple of times; should not throw and should not
        // duplicate theme entries.
        signal.value(true);
        signal.value(true); // no-op update
        Assert.assertTrue(component.hasThemeName("spin"));
        Assert.assertEquals(1, component.getThemeNames().stream()
                .filter("spin"::equals).count());

        signal.value(false);
        signal.value(false); // no-op update
        Assert.assertFalse(component.hasThemeName("spin"));
    }

    @Test
    public void bindMultipleSignals() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        enum DummyEnum {
            ONE, TWO, THREE
        }
        SharedValueSignal<DummyEnum> signal = new SharedValueSignal<>(
                DummyEnum.ONE);

        Signal<Boolean> a = signal.map(v -> v == DummyEnum.ONE);
        Signal<Boolean> b = signal.map(v -> v == DummyEnum.TWO);
        Signal<Boolean> c = signal.map(v -> v == DummyEnum.THREE);
        component.bindThemeName("a", a);
        component.bindThemeName("b", b);
        component.bindThemeName("c", c);

        Assert.assertTrue(component.hasThemeName("a"));
        Assert.assertFalse(component.hasThemeName("b"));
        Assert.assertFalse(component.hasThemeName("b"));

        signal.value(DummyEnum.TWO);
        Assert.assertFalse(component.hasThemeName("a"));
        Assert.assertTrue(component.hasThemeName("b"));
        Assert.assertFalse(component.hasThemeName("c"));

        signal.value(DummyEnum.THREE);
        Assert.assertFalse(component.hasThemeName("a"));
        Assert.assertFalse(component.hasThemeName("b"));
        Assert.assertTrue(component.hasThemeName("c"));
    }

    @Tag("span")
    private static class TestComponent extends Component implements HasTheme {
    }
}
