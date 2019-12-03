/*
 * Copyright 2000-2019 Vaadin Ltd.
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
