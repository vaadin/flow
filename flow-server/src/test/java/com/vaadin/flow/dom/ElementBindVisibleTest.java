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

import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ElementBindVisibleTest extends SignalsUnitTest {

    @Test
    public void bindVisible_elementAttachedBefore_bindingActive() {
        Element element = new Element("foo");
        // attach before bindVisible
        UI.getCurrent().getElement().appendChild(element);
        assertTrue(element.isVisible());
        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        element.bindVisible(signal);

        assertFalse(element.isVisible());
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindVisible_elementAttachedAfter_bindingActive() {
        Element element = new Element("foo");
        assertTrue(element.isVisible());
        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        element.bindVisible(signal);
        // attach after bindVisible
        UI.getCurrent().getElement().appendChild(element);

        assertFalse(element.isVisible());
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindVisible_elementAttached_bindingActive() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        element.bindVisible(signal);

        // initially false
        assertFalse(element.isVisible());

        // false -> true
        signal.value(true);
        assertTrue(element.isVisible());

        // null transforms to false
        signal.value(null);
        assertFalse(element.isVisible());
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindVisible_elementNotAttached_bindingInactive() {
        Element element = new Element("foo");
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        element.bindVisible(signal);

        assertTrue(element.isVisible());
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindVisible_elementDetached_bindingInactive() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        element.bindVisible(signal);
        element.removeFromParent();
        signal.value(false); // ignored

        assertTrue(element.isVisible());
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindVisible_elementReAttached_bindingActivate() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        element.bindVisible(signal);
        element.removeFromParent();
        signal.value(false);
        UI.getCurrent().getElement().appendChild(element);

        assertFalse(element.isVisible());
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindVisible_setVisibleAndBindVisibleWhileBindingIsActive_throwException() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);
        element.bindVisible(new ValueSignal<>(true));

        assertThrows(BindingActiveException.class,
                () -> element.setVisible(false));
        assertThrows(BindingActiveException.class,
                () -> element.bindVisible(new ValueSignal<>(true)));
        assertTrue(element.isVisible());
        assertTrue(events.isEmpty());
    }

    @Test
    public void bindVisible_withNullBinding_removesBinding() {
        Element element = new Element("foo");
        UI.getCurrent().getElement().appendChild(element);
        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        element.bindVisible(signal);
        assertTrue(element.isVisible());

        element.bindVisible(null); // remove binding
        signal.value(false); // no effect
        assertTrue(element.isVisible());
        assertTrue(events.isEmpty());
    }
}
