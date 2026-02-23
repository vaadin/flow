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

/**
 * Tests for {@link HtmlObject#bindData(com.vaadin.flow.signals.Signal)}.
 */
public class HtmlObjectBindDataTest extends SignalsUnitTest {

    @Test
    public void bindData_updatesAttributeOnSignalChange() {
        HtmlObject htmlObject = new HtmlObject();
        UI.getCurrent().add(htmlObject);

        ValueSignal<String> signal = new ValueSignal<>("");
        htmlObject.bindData(signal);

        signal.set("https://example.com/data.swf");
        assertEquals("https://example.com/data.swf",
                htmlObject.getElement().getAttribute("data"));

        signal.set("https://example.com/other.swf");
        assertEquals("https://example.com/other.swf",
                htmlObject.getElement().getAttribute("data"));
    }

    @Test
    public void bindData_attachedThenDetached_stopsUpdates() {
        HtmlObject htmlObject = new HtmlObject();
        UI.getCurrent().add(htmlObject);

        ValueSignal<String> signal = new ValueSignal<>("initial");
        htmlObject.bindData(signal);
        assertEquals("initial", htmlObject.getElement().getAttribute("data"));

        // Detach the component
        UI.getCurrent().remove(htmlObject);

        // Update value after detach – attribute should remain unchanged
        signal.set("updated");
        assertEquals("initial", htmlObject.getElement().getAttribute("data"));
    }

    @Test
    public void bindData_nullSignal_throwsNPE() {
        HtmlObject htmlObject = new HtmlObject();
        UI.getCurrent().add(htmlObject);

        assertThrows(NullPointerException.class,
                () -> htmlObject.bindData(null));
    }

    @Test
    public void bindData_setDataWhileBound_throwsException() {
        HtmlObject htmlObject = new HtmlObject();
        UI.getCurrent().add(htmlObject);

        ValueSignal<String> signal = new ValueSignal<>("");
        htmlObject.bindData(signal);

        assertThrows(BindingActiveException.class,
                () -> htmlObject.setData("manual"));
    }

    @Test
    public void bindData_bindAgainWhileBound_throwsException() {
        HtmlObject htmlObject = new HtmlObject();
        UI.getCurrent().add(htmlObject);

        ValueSignal<String> signal = new ValueSignal<>("");
        htmlObject.bindData(signal);

        assertThrows(BindingActiveException.class,
                () -> htmlObject.bindData(new ValueSignal<>("other")));
    }
}
