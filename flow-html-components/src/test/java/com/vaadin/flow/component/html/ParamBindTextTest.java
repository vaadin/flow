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

import java.util.Optional;

import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ParamBindTextTest extends SignalsUnitTest {

    @Test
    public void bindName_updatesAttributeOnSignalChange() {
        Param param = new Param();
        UI.getCurrent().add(param);

        ValueSignal<String> signal = new ValueSignal<>("");
        param.bindName(signal);

        signal.value("name-1");
        assertEquals("name-1", param.getName());

        signal.value("name-2");
        assertEquals("name-2", param.getName());
    }

    @Test
    public void bindName_setNameWhileBindingActive_throws() {
        Param param = new Param();
        UI.getCurrent().add(param);

        ValueSignal<String> signal = new ValueSignal<>("initial");
        param.bindName(signal);

        assertThrows(BindingActiveException.class,
                () -> param.setName("manual"));
    }

    @Test
    public void bindName_unbindWithNull_stopsUpdates() {
        Param param = new Param();
        UI.getCurrent().add(param);

        ValueSignal<String> signal = new ValueSignal<>("a");
        param.bindName(signal);
        assertEquals("a", param.getName());

        param.bindName(null);
        signal.value("b");

        // After unbinding, value should remain as before
        assertEquals("a", param.getName());
    }

    @Test
    public void bindValue_updatesAttributeOnSignalChange() {
        Param param = new Param();
        UI.getCurrent().add(param);

        ValueSignal<String> signal = new ValueSignal<>("");
        param.bindValue(signal);

        signal.value("value-1");
        assertEquals(Optional.of("value-1"), param.getValue());

        signal.value("value-2");
        assertEquals(Optional.of("value-2"), param.getValue());
    }

    @Test
    public void bindValue_setValueWhileBindingActive_throws() {
        Param param = new Param();
        UI.getCurrent().add(param);

        ValueSignal<String> signal = new ValueSignal<>("initial");
        param.bindValue(signal);

        assertThrows(BindingActiveException.class,
                () -> param.setValue("manual"));
    }

    @Test
    public void bindValue_unbindWithNull_stopsUpdates() {
        Param param = new Param();
        UI.getCurrent().add(param);

        ValueSignal<String> signal = new ValueSignal<>("a");
        param.bindValue(signal);
        assertEquals(Optional.of("a"), param.getValue());

        param.bindValue(null);
        signal.value("b");

        // After unbinding, value should remain as before
        assertEquals(Optional.of("a"), param.getValue());
    }
}
