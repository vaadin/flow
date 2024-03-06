/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
    private UI ui;

    @Before
    public void setUp() {
        ui = new UI() {
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
