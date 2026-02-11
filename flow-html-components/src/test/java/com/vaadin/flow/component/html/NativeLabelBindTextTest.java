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
package com.vaadin.flow.component.html;

import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class NativeLabelBindTextTest extends SignalsUnitTest {

    @Test
    public void bindText_updatesTextOnSignalChange() {
        NativeLabel label = new NativeLabel();
        UI.getCurrent().add(label);

        ValueSignal<String> signal = new ValueSignal<>("");
        label.bindText(signal);

        signal.value("text-1");
        assertEquals("text-1", label.getText());

        signal.value("text-2");
        assertEquals("text-2", label.getText());
    }

    @Test
    public void bindText_setTextWhileBindingActive_throws() {
        NativeLabel label = new NativeLabel();
        UI.getCurrent().add(label);

        ValueSignal<String> signal = new ValueSignal<>("initial");
        label.bindText(signal);

        assertThrows(BindingActiveException.class,
                () -> label.setText("manual"));
    }

    @Test
    public void bindText_nullSignal_throwsNPE() {
        NativeLabel label = new NativeLabel();
        UI.getCurrent().add(label);

        assertThrows(NullPointerException.class,
                () -> label.bindText(null));
    }

    @Test
    public void constructorWithSignal_bindsText() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        NativeLabel label = new NativeLabel(signal);
        UI.getCurrent().add(label);

        assertEquals("initial", label.getText());

        signal.value("updated");
        assertEquals("updated", label.getText());
    }
}
