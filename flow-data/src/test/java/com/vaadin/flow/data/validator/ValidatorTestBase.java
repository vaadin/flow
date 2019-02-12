/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.data.validator;

import java.util.Locale;

import com.vaadin.flow.data.binder.testcomponents.TestLabel;
import org.junit.Assert;
import org.junit.Before;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;

public class ValidatorTestBase {

    private TestLabel localeContext;
    private Locale locale = Locale.US;

    @Before
    public void setUp() {
        UI ui = new UI() {
            @Override
            public Locale getLocale() {
                return locale;
            }
        };
        UI.setCurrent(ui);
        localeContext = new TestLabel();
    }

    protected <T> void assertPasses(T value, Validator<? super T> validator) {
        ValidationResult result = validator.apply(value, new ValueContext());
        if (result.isError()) {
            Assert.fail(value + " should pass " + validator + " but got "
                    + result.getErrorMessage());
        }
    }

    protected <T> void assertFails(T value, String errorMessage,
            Validator<? super T> validator) {
        ValidationResult result = validator.apply(value,
                new ValueContext(localeContext));
        Assert.assertTrue(result.isError());
        Assert.assertEquals(errorMessage, result.getErrorMessage());
    }

    protected <T> void assertFails(T value, AbstractValidator<? super T> v) {
        assertFails(value, v.getMessage(value), v);
    }

    protected void setLocale(Locale locale) {
        this.locale = locale;
    }
}