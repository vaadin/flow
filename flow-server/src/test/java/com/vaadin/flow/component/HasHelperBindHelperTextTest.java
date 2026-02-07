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

import org.junit.Test;

import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class HasHelperBindHelperTextTest extends SignalsUnitTest {

    @Tag("div")
    public static class HasHelperComponent extends Component
            implements HasHelper {
    }

    @Test
    public void bindHelperText_updatesPropertyOnSignalChange() {
        HasHelperComponent c = new HasHelperComponent();
        UI.getCurrent().add(c);

        ValueSignal<String> signal = new ValueSignal<>("");
        c.bindHelperText(signal);

        signal.value("help-1");
        assertEquals("help-1", c.getElement().getProperty("helperText"));

        signal.value("help-2");
        assertEquals("help-2", c.getElement().getProperty("helperText"));
    }

    @Test
    public void bindHelperText_setHelperTextWhileBindingActive_throws() {
        HasHelperComponent c = new HasHelperComponent();
        UI.getCurrent().add(c);

        ValueSignal<String> signal = new ValueSignal<>("initial");
        c.bindHelperText(signal);

        assertThrows(BindingActiveException.class,
                () -> c.setHelperText("manual"));
    }

    @Test
    public void bindHelperText_unbindWithNull_stopsUpdates() {
        HasHelperComponent c = new HasHelperComponent();
        UI.getCurrent().add(c);

        ValueSignal<String> signal = new ValueSignal<>("a");
        c.bindHelperText(signal);
        assertEquals("a", c.getElement().getProperty("helperText"));

        c.bindHelperText(null);
        signal.value("b");

        // After unbinding, value should remain as before
        assertEquals("a", c.getElement().getProperty("helperText"));
    }
}
