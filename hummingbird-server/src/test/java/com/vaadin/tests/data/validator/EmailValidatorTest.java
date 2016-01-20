package com.vaadin.tests.data.validator;

import com.vaadin.data.validator.EmailValidator;

import junit.framework.TestCase;

public class EmailValidatorTest extends TestCase {

    private EmailValidator validator = new EmailValidator("Error");

    public void testEmailValidatorWithNull() {
        assertTrue(validator.isValid(null));
    }

    public void testEmailValidatorWithEmptyString() {
        assertTrue(validator.isValid(""));
    }

    public void testEmailValidatorWithFaultyString() {
        assertFalse(validator.isValid("not.an.email"));
    }

    public void testEmailValidatorWithOkEmail() {
        assertTrue(validator.isValid("my.name@email.com"));
    }
}
