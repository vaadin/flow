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
import com.vaadin.flow.signals.local.ListSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class NativeFormBindChildrenTest extends SignalsUnitTest {

    @Test
    void bindChildren_factoryCreatesForm_throws() {
        NativeForm form = new NativeForm();
        UI.getCurrent().add(form);
        ListSignal<String> values = new ListSignal<>();
        form.bindChildren(values,
                value -> "form".equals(value.peek()) ? new NativeForm()
                        : new Span(value.peek()));

        values.insertLast("field");
        assertEquals(1, form.getComponentCount());

        values.insertLast("form");

        // the binding runs in an effect, which reports the failure to the
        // session error handler instead of throwing to the caller
        assertEquals(1, events.size());
        assertInstanceOf(IllegalArgumentException.class,
                events.remove().getThrowable());
        assertEquals(1, form.getComponentCount());
    }
}
