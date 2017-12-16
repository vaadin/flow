/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.data;

import java.util.Locale;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.ui.datepicker.DatePicker;
import com.vaadin.ui.textfield.TextField;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;

public class ValueContextTest extends UI {

    private static final Locale UI_LOCALE = Locale.GERMAN;
    private static final Locale COMPONENT_LOCALE = Locale.FRENCH;
    private TextField textField;

    @Test
    public void locale_from_component() {
        setLocale(COMPONENT_LOCALE);
        ValueContext fromComponent = new ValueContext(textField);
        Locale locale = fromComponent.getLocale().orElse(null);
        Objects.requireNonNull(locale);
        Assert.assertEquals("Unexpected locale from component",
                COMPONENT_LOCALE, locale);
    }

    @Test
    public void locale_from_ui() {
        ValueContext fromComponent = new ValueContext(textField);
        Locale locale = fromComponent.getLocale().orElse(null);
        Objects.requireNonNull(locale);
        Assert.assertEquals("Unexpected locale from component", UI_LOCALE,
                locale);
    }

    @Test
    public void default_locale() {
        setLocale(Locale.getDefault());
        ValueContext fromComponent = new ValueContext(textField);
        Locale locale = fromComponent.getLocale().orElse(null);
        Objects.requireNonNull(locale);
        Assert.assertEquals("Unexpected locale from component",
                Locale.getDefault(), locale);
    }

    @Test
    public void testHasValue1() {
        setLocale(Locale.getDefault());
        ValueContext fromComponent = new ValueContext(textField);
        Assert.assertEquals(textField, fromComponent.getHasValue().get());
    }

    @Test
    public void testHasValue2() {
        setLocale(Locale.getDefault());
        ValueContext fromComponent = new ValueContext(new DatePicker(),
                textField);
        Assert.assertEquals(textField, fromComponent.getHasValue().get());
    }

    @Test
    public void testHasValue3() {
        setLocale(Locale.getDefault());
        ValueContext fromComponent = new ValueContext(new DatePicker(),
                textField, Locale.CANADA);
        Assert.assertEquals(textField, fromComponent.getHasValue().get());
        Assert.assertEquals(Locale.CANADA, fromComponent.getLocale().get());
    }

    @Before
    public void setUp() {
        setLocale(UI_LOCALE);
        UI.setCurrent(this);
        textField = new TextField();
        add(textField);
    }

    @Override
    public void init(VaadinRequest request) {
    }
}
