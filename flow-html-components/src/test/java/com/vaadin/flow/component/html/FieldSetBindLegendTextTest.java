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

/**
 * Tests for {@link FieldSet#bindLegendText(com.vaadin.signals.Signal)} using
 * {@link SignalsUnitTest} setup.
 */
public class FieldSetBindLegendTextTest extends SignalsUnitTest {

    @Test
    public void bindLegendText_updatesLegendOnSignalChange() {
        FieldSet fieldSet = new FieldSet();
        UI.getCurrent().add(fieldSet);

        ValueSignal<String> signal = new ValueSignal<>("");
        fieldSet.bindLegendText(signal);

        signal.value("legend-1");
        assertEquals("legend-1", fieldSet.getLegend().getText());

        signal.value("legend-2");
        assertEquals("legend-2", fieldSet.getLegend().getText());
    }

    @Test
    public void bindLegendText_setLegendTextWhileBindingActive_throws() {
        FieldSet fieldSet = new FieldSet();
        UI.getCurrent().add(fieldSet);

        ValueSignal<String> signal = new ValueSignal<>("initial");
        fieldSet.bindLegendText(signal);

        assertThrows(BindingActiveException.class,
                () -> fieldSet.setLegendText("manual"));
    }

    @Test
    public void bindLegendText_unbindWithNull_stopsUpdates() {
        FieldSet fieldSet = new FieldSet();
        UI.getCurrent().add(fieldSet);

        ValueSignal<String> signal = new ValueSignal<>("a");
        fieldSet.bindLegendText(signal);
        assertEquals("a", fieldSet.getLegend().getText());

        fieldSet.bindLegendText(null);
        signal.value("b");

        // After unbinding, value should remain as before
        assertEquals("a", fieldSet.getLegend().getText());
    }
}
