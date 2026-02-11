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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Tests for binding CSS class presence to a Signal using ClassList.bind.
 */
public class ClassListBindTest extends SignalsUnitTest {

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
    public void bind_nullSignal_throwsNPE() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);

        Assert.assertThrows(NullPointerException.class,
                () -> element.getClassList().bind("badge", null));
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

    @Test
    public void lazyInitSignalBindingFeature() {
        Element element = new Element("div");
        UI.getCurrent().getElement().appendChild(element);
        element.getClassList().add("spin");
        Assert.assertTrue(element.getClassList().contains("spin"));

        element.getNode().getFeatureIfInitialized(SignalBindingFeature.class)
                .ifPresent(feature -> Assert.fail(
                        "SignalBindingFeature should not be initialized before binding a signal"));

        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        element.getClassList().bind("spin", signal);

        element.getNode().getFeatureIfInitialized(SignalBindingFeature.class)
                .orElseThrow(() -> new AssertionError(
                        "SignalBindingFeature should be initialized after binding a signal"));
    }
}
