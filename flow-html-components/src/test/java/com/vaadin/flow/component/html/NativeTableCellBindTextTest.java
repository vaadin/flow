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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NativeTableCellBindTextTest extends SignalsUnitTest {

    @Test
    public void bindText_updatesTextOnSignalChange() {
        NativeTableCell cell = new NativeTableCell();
        UI.getCurrent().add(cell);

        ValueSignal<String> signal = new ValueSignal<>("");
        cell.bindText(signal);

        signal.set("text-1");
        assertEquals("text-1", cell.getText());

        signal.set("text-2");
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
    public void bindText_nullSignal_throwsNPE() {
        NativeTableCell cell = new NativeTableCell();
        UI.getCurrent().add(cell);

        assertThrows(NullPointerException.class, () -> cell.bindText(null));
    }

    @Test
    public void constructorWithSignal_bindsText() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        NativeTableCell cell = new NativeTableCell(signal);
        UI.getCurrent().add(cell);

        assertEquals("initial", cell.getText());

        signal.set("updated");
        assertEquals("updated", cell.getText());
    }
}
