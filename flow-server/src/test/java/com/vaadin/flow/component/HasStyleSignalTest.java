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
package com.vaadin.flow.component;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for
 * {@link HasStyle#bindClassName(String, com.vaadin.flow.signals.Signal)}.
 */
public class HasStyleSignalTest extends SignalsUnitTest {

    @Tag("div")
    public static class HasStyleComponent extends Component
            implements HasStyle {
    }

    @Test
    public void bindClassName_signalBound_classNameToggledWhenAttached() {
        HasStyleComponent component = new HasStyleComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(false);
        component.bindClassName("active", signal);

        assertFalse(component.hasClassName("active"));

        signal.set(true);
        assertTrue(component.hasClassName("active"));

        signal.set(false);
        assertFalse(component.hasClassName("active"));
    }

    @Test
    public void bindClassName_signalTrue_classNameAdded() {
        HasStyleComponent component = new HasStyleComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindClassName("highlight", signal);

        assertTrue(component.hasClassName("highlight"));
    }

    @Test
    public void bindClassName_attachedThenDetached_stopsUpdates() {
        HasStyleComponent component = new HasStyleComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindClassName("active", signal);
        assertTrue(component.hasClassName("active"));

        // Detach
        UI.getCurrent().remove(component);

        // Update after detach
        signal.set(false);
        assertTrue(component.hasClassName("active"));
    }

    @Test
    public void bindClassName_multipleClassNames_independentBindings() {
        HasStyleComponent component = new HasStyleComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal1 = new ValueSignal<>(true);
        ValueSignal<Boolean> signal2 = new ValueSignal<>(false);
        component.bindClassName("class1", signal1);
        component.bindClassName("class2", signal2);

        assertTrue(component.hasClassName("class1"));
        assertFalse(component.hasClassName("class2"));

        signal2.set(true);
        assertTrue(component.hasClassName("class1"));
        assertTrue(component.hasClassName("class2"));

        signal1.set(false);
        assertFalse(component.hasClassName("class1"));
        assertTrue(component.hasClassName("class2"));
    }

    @Test
    public void bindClassName_addClassNameWhileBound_throwsException() {
        HasStyleComponent component = new HasStyleComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindClassName("active", signal);

        assertThrows(BindingActiveException.class,
                () -> component.addClassName("active"));
    }

    @Test
    public void bindClassName_bindAgainWhileBound_throwsException() {
        HasStyleComponent component = new HasStyleComponent();
        UI.getCurrent().add(component);

        ValueSignal<Boolean> signal = new ValueSignal<>(true);
        component.bindClassName("active", signal);

        assertThrows(BindingActiveException.class,
                () -> component.bindClassName("active",
                        new ValueSignal<>(false)));
    }
}
