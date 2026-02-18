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

public class NativeDetailsBindTextTest extends SignalsUnitTest {

    @Test
    public void bindSummaryText_updatesTextOnSignalChange() {
        NativeDetails details = new NativeDetails();
        UI.getCurrent().add(details);

        ValueSignal<String> signal = new ValueSignal<>("");
        details.bindSummaryText(signal);

        signal.set("summary-1");
        assertEquals("summary-1", details.getSummaryText());

        signal.set("summary-2");
        assertEquals("summary-2", details.getSummaryText());
    }

    @Test
    public void bindSummaryText_setSummaryTextWhileBindingActive_throws() {
        NativeDetails details = new NativeDetails();
        UI.getCurrent().add(details);

        ValueSignal<String> signal = new ValueSignal<>("initial");
        details.bindSummaryText(signal);

        assertThrows(BindingActiveException.class,
                () -> details.setSummaryText("manual"));
    }

    @Test
    public void bindSummaryText_nullSignal_throwsNPE() {
        NativeDetails details = new NativeDetails();
        UI.getCurrent().add(details);

        assertThrows(NullPointerException.class,
                () -> details.bindSummaryText(null));
    }

    @Test
    public void constructorWithSignal_bindsSummaryText() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        NativeDetails details = new NativeDetails(signal);
        UI.getCurrent().add(details);

        assertEquals("initial", details.getSummaryText());

        signal.set("updated");
        assertEquals("updated", details.getSummaryText());
    }

    @Test
    public void constructorWithSignalAndContent_bindsSummaryTextAndSetsContent() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        Span content = new Span("content");
        NativeDetails details = new NativeDetails(signal, content);
        UI.getCurrent().add(details);

        assertEquals("initial", details.getSummaryText());
        assertEquals(content, details.getContent());

        signal.set("updated");
        assertEquals("updated", details.getSummaryText());
    }
}
