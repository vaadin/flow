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
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises the signal-bound text of {@link TableCaption}.
 */
class TableCaptionBindTextTest extends SignalsUnitTest {

    @Test
    void signalConstructor_bindsTheText() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        TableCaption caption = new TableCaption(signal);
        Table table = new Table();
        table.setCaption(caption);
        UI.getCurrent().add(table);

        assertEquals("initial", table.getCaptionText());
        signal.set("updated");
        assertEquals("updated", table.getCaptionText());
    }

    @Test
    void signalConstructor_nullSignal_throws() {
        assertThrows(NullPointerException.class,
                () -> new TableCaption((Signal<String>) null));
    }
}
