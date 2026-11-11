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

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.InputMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InputTest extends ComponentTest {

    // Actual test methods in super class

    @BeforeEach
    @Override
    void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException {
        whitelistProperty("valueChangeMode");
        whitelistProperty("valueChangeTimeout");
        whitelistProperty("inputMode");
        whitelistProperty("name");
        whitelistProperty("autocomplete");
        super.setup();
    }

    @Test
    void inputMode_defaultsToNull() {
        Input input = new Input();
        assertNull(input.getInputMode());
    }

    @Test
    void setInputMode_getInputMode() {
        Input input = new Input();

        input.setInputMode(InputMode.NUMERIC);
        assertEquals(InputMode.NUMERIC, input.getInputMode());
        assertEquals("numeric", input.getElement().getAttribute("inputmode"));

        input.setInputMode(null);
        assertNull(input.getInputMode());
        assertNull(input.getElement().getAttribute("inputmode"));
    }

    @Test
    void setName_getName() {
        Input input = new Input();
        assertNull(input.getName());

        input.setName("username");
        assertEquals("username", input.getName());
        assertEquals("username", input.getElement().getAttribute("name"));

        input.setName(null);
        assertNull(input.getName());
        assertNull(input.getElement().getAttribute("name"));
    }

    @Test
    void setAutocomplete_getAutocomplete() {
        Input input = new Input();
        assertNull(input.getAutocomplete());

        input.setAutocomplete("current-password");
        assertEquals("current-password", input.getAutocomplete());
        assertEquals("current-password",
                input.getElement().getAttribute("autocomplete"));

        input.setAutocomplete(null);
        assertNull(input.getAutocomplete());
        assertNull(input.getElement().getAttribute("autocomplete"));
    }

    @Test
    void setRequiredIndicatorVisible_setsNativeRequiredProperty() {
        Input input = new Input();
        input.setRequiredIndicatorVisible(true);
        // the browser blocks a form submit while a required input is empty
        assertEquals(true, input.getElement().getProperty("required", false));
    }

    @Override
    protected void addProperties() {
        addStringProperty("type", "text");
        // Object.class because of generics
        addProperty("value", Object.class, "", "foo", false, false);
        addStringProperty("placeholder", "");
    }

    @Test
    @Override
    protected void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }
}
