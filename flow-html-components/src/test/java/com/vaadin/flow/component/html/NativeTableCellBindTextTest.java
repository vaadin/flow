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
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class NativeTableCellBindTextTest extends SignalsUnitTest {

    @Test
    public void bindText_updatesTextOnSignalChange() {
        NativeTableCell cell = new NativeTableCell();
        UI.getCurrent().add(cell);

        ValueSignal<String> signal = new ValueSignal<>("");
        cell.bindText(signal);

        signal.value("text-1");
        assertEquals("text-1", cell.getText());

        signal.value("text-2");
        assertEquals("text-2", cell.getText());
    }

    @Test
    public void bindText_setTextWhileBindingActive_throws() {
        NativeTableCell cell = new NativeTableCell();
        UI.getCurrent().add(cell);

        ValueSignal<String> signal = new ValueSignal<>("initial");
        cell.bindText(signal);

        assertThrows(BindingActiveException.class,
                () -> cell.setText("manual"));
    }

    @Test
    public void bindText_unbindWithNull_stopsUpdates() {
        NativeTableCell cell = new NativeTableCell();
        UI.getCurrent().add(cell);

        ValueSignal<String> signal = new ValueSignal<>("a");
        cell.bindText(signal);
        assertEquals("a", cell.getText());

        cell.bindText(null);
        signal.value("b");

        // After unbinding, value should remain as before
        assertEquals("a", cell.getText());
    }

    @Test
    public void constructorWithSignal_bindsText() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        NativeTableCell cell = new NativeTableCell(signal);
        UI.getCurrent().add(cell);

        assertEquals("initial", cell.getText());

        signal.value("updated");
        assertEquals("updated", cell.getText());
    }
}
