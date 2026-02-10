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
import com.vaadin.flow.signals.shared.SharedNumberSignal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class RangeInputBindTest extends SignalsUnitTest {

    @Test
    public void bindMin_updatesAttributeOnSignalChange() {
        RangeInput rangeInput = new RangeInput();
        UI.getCurrent().add(rangeInput);

        SharedNumberSignal signal = new SharedNumberSignal(0.0);
        rangeInput.bindMin(signal);

        signal.set(5.5);
        assertEquals("5.5", rangeInput.getElement().getAttribute("min"));

        signal.set(10.0);
        assertEquals("10.0", rangeInput.getElement().getAttribute("min"));
    }

    @Test
    public void bindMin_setMinWhileBindingActive_throws() {
        RangeInput rangeInput = new RangeInput();
        UI.getCurrent().add(rangeInput);

        SharedNumberSignal signal = new SharedNumberSignal(0.0);
        rangeInput.bindMin(signal);

        assertThrows(BindingActiveException.class,
                () -> rangeInput.setMin(5.0));
    }

    @Test
    public void bindMin_nullSignal_throwsNPE() {
        RangeInput rangeInput = new RangeInput();
        UI.getCurrent().add(rangeInput);

        assertThrows(NullPointerException.class,
                () -> rangeInput.bindMin(null));
    }

    @Test
    public void bindMax_updatesAttributeOnSignalChange() {
        RangeInput rangeInput = new RangeInput();
        UI.getCurrent().add(rangeInput);

        SharedNumberSignal signal = new SharedNumberSignal(100.0);
        rangeInput.bindMax(signal);

        signal.set(150.5);
        assertEquals("150.5", rangeInput.getElement().getAttribute("max"));

        signal.set(200.0);
        assertEquals("200.0", rangeInput.getElement().getAttribute("max"));
    }

    @Test
    public void bindMax_setMaxWhileBindingActive_throws() {
        RangeInput rangeInput = new RangeInput();
        UI.getCurrent().add(rangeInput);

        SharedNumberSignal signal = new SharedNumberSignal(100.0);
        rangeInput.bindMax(signal);

        assertThrows(BindingActiveException.class,
                () -> rangeInput.setMax(200.0));
    }

    @Test
    public void bindMax_nullSignal_throwsNPE() {
        RangeInput rangeInput = new RangeInput();
        UI.getCurrent().add(rangeInput);

        assertThrows(NullPointerException.class,
                () -> rangeInput.bindMax(null));
    }
}
