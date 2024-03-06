/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.validator;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class NotEmptyValidatorTest {

    @Test
    public void nullValueIsDisallowed() {
        NotEmptyValidator<String> validator = new NotEmptyValidator<>("foo");
        ValidationResult result = validator.apply(null, new ValueContext());
        Assert.assertTrue(result.isError());
        Assert.assertEquals("foo", result.getErrorMessage());
    }

    @Test
    public void emptyValueIsDisallowed() {
        NotEmptyValidator<String> validator = new NotEmptyValidator<>("foo");
        ValidationResult result = validator.apply("", new ValueContext());
        Assert.assertTrue(result.isError());
        Assert.assertEquals("foo", result.getErrorMessage());
    }

    @Test
    public void nonNullValueIsAllowed() {
        NotEmptyValidator<Object> validator = new NotEmptyValidator<>("foo");
        Object value = new Object();
        ValidationResult result = validator.apply(value, new ValueContext());
        Assert.assertFalse(result.isError());
        Assert.assertFalse(result.isError());
    }
}
