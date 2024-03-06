/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Locale;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.data.converter.Converter;

/**
 * A base class for {@code Binder} unit tests.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 */
public abstract class BinderTestBase<BINDER extends Binder<ITEM>, ITEM>
        implements Serializable {

    protected static final String NEGATIVE_ERROR_MESSAGE = "Value must be non-negative";

    protected static final String NOT_NUMBER_ERROR_MESSAGE = "Value must be a number";

    protected static final String EMPTY_ERROR_MESSAGE = "Value cannot be empty";

    protected BINDER binder;

    protected ITEM item;

    protected TestTextField nameField;
    protected TestTextField ageField;

    protected Validator<String> notEmpty = Validator.from(val -> !val.isEmpty(),
            EMPTY_ERROR_MESSAGE);
    protected Converter<String, Integer> stringToInteger = Converter.from(
            Integer::valueOf, String::valueOf, e -> NOT_NUMBER_ERROR_MESSAGE);
    protected Validator<Integer> notNegative = Validator.from(x -> x >= 0,
            NEGATIVE_ERROR_MESSAGE);

    public static void testSerialization(Object toSerialize) {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(
                new ByteArrayOutputStream())) {
            objectOutputStream.writeObject(toSerialize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void assertInvalidField(String expectedErrorMessage, HasValidation field) {
        Assert.assertEquals(
                "The field should contain same error message as binder",
                expectedErrorMessage, field.getErrorMessage());
        Assert.assertTrue("The field should be invalid", field.isInvalid());
    }

    void assertValidField(HasValidation field) {
        Assert.assertFalse("The field should be valid", field.isInvalid());
    }

    @Before
    public void setUpBase() {
        UI ui = new UI() {
            @Override
            public Locale getLocale() {
                return Locale.US;
            }

        };
        nameField = new TestTextField();
        ageField = new TestTextField();
        ui.add(nameField, ageField);
    }

    @After
    public void testBinderSerialization() {
        testSerialization(binder);
    }
}
