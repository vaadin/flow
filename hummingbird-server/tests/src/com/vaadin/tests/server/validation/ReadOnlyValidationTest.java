package com.vaadin.tests.server.validation;

import org.junit.Test;

import com.vaadin.data.validator.IntegerValidator;
import com.vaadin.ui.TestField;

public class ReadOnlyValidationTest {

    @Test
    public void testIntegerValidation() {
        TestField field = new TestField();
        field.addValidator(new IntegerValidator("Enter a Valid Number"));
        field.setValue(String.valueOf(10));
        field.validate();
    }
}
