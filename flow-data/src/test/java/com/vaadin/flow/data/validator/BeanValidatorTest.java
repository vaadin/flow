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
package com.vaadin.flow.data.validator;

import jakarta.validation.Validation;

import java.util.Calendar;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.tests.data.bean.Address;
import com.vaadin.flow.tests.data.bean.BeanToValidate;

class BeanValidatorTest extends ValidatorTestBase {

    @Test
    void testFirstNameNullFails() {
        assertFails(null, "must not be null", validator("firstname"));
    }

    @Test
    void testFirstNameTooShortFails() {
        assertFails("x", "size must be between 3 and 16",
                validator("firstname"));
    }

    @Test
    void testFirstNameLongEnoughPasses() {
        assertPasses("Magi", validator("firstname"));
    }

    @Test
    void testAgeTooYoungFails() {
        assertFails(14, "Must be 18 or above", validator("age"));
    }

    @Test
    void testDateOfBirthNullPasses() {
        assertPasses(null, validator("dateOfBirth"));
    }

    @Test
    void testDateOfBirthInTheFutureFails() {
        Calendar year3k = Calendar.getInstance();
        year3k.set(3000, 0, 1);
        assertFails(year3k, "must be a past date", validator("dateOfBirth"));
    }

    @Test
    void testAddressesEmptyArrayPasses() {
        Address[] noAddresses = {};
        System.out.println(Validation.buildDefaultValidatorFactory());
        assertPasses(noAddresses, validator("addresses"));
    }

    @Test
    void testAddressesNullFails() {
        assertFails(null, "must not be null", validator("addresses"));
    }

    @Test
    void testInvalidDecimalsFailsInFrench() {
        setLocale(Locale.FRENCH);
        BeanValidator v = validator("decimals");
        assertFails("1234.567", "valeur numérique hors limites "
                + "(<3 chiffres>.<2 chiffres> attendu)", v);
    }

    @Test
    void testAddressNestedPropertyInvalidPostalCodeFails() {
        assertFails(100_000, "must be less than or equal to 99999",
                validator("address.postalCode"));
    }

    @Test
    void testNullValuePasses() {
        assertPasses(null, validator("nickname"));
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    private BeanValidator validator(String propertyName) {
        return new BeanValidator(BeanToValidate.class, propertyName);
    }

}
