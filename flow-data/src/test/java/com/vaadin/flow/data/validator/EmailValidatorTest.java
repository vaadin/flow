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

import org.junit.jupiter.api.Test;

class EmailValidatorTest extends ValidatorTestBase {

    @Test
    void testNullStringFails() {
        assertPasses(null, shouldNotFail());
    }

    @Test
    void testEmptyStringFails() {
        assertFails("", validator("empty string not allowed"));
    }

    @Test
    void testStringWithoutAtSignFails() {
        assertFails("johannesd.vaadin", validator("@ is required"));
    }

    @Test
    void testMissingLocalPartFails() {
        RegexpValidator v = validator("local part is required");
        assertFails("@localhost", v);
        assertFails(" @localhost", v);
    }

    @Test
    void testNonAsciiEmailFails() {
        RegexpValidator v = validator("accented letters not allowed");
        assertFails("jöhännes@vaadin.com", v);
        assertFails("johannes@váádìn.com", v);
        assertFails("johannes@vaadin.cõm", v);
    }

    @Test
    void testLocalPartWithPunctuationPasses() {
        RegexpValidator v = shouldNotFail();
        assertPasses("johannesd+test@vaadin.com", v);
        assertPasses("johannes.dahlstrom@vaadin.com", v);
        assertPasses("johannes_d@vaadin.com", v);
    }

    @Test
    void testEmailWithoutDomainPartFails() {
        assertFails("johannesd@", validator("domain part is required"));
    }

    @Test
    void testComplexDomainPasses() {
        assertPasses("johannesd@foo.bar.baz.vaadin.com", shouldNotFail());
    }

    @Test
    void testDomainWithPunctuationPasses() {
        assertPasses("johannesd@vaadin-dev.com", shouldNotFail());
    }

    @Test
    void testMissingTldFails() {
        assertFails("johannesd@localhost", validator("tld is required"));
    }

    @Test
    void testOneLetterTldFails() {
        assertFails("johannesd@vaadin.f",
                validator("one-letter tld not allowed"));
    }

    @Test
    void testLongTldPasses() {
        assertPasses("joonas@vaadin.management", shouldNotFail());
    }

    @Test
    void testIdnTldPasses() {
        assertPasses("leif@vaadin.XN--VERMGENSBERATER-CTB", shouldNotFail());
    }

    @Test
    void testYelledEmailPasses() {
        assertPasses("JOHANNESD@VAADIN.COM", shouldNotFail());
    }

    @Test
    void testEmailWithDigitsPasses() {
        assertPasses("johannes84@v44d1n.com", shouldNotFail());
    }

    @Test
    void emptyString_validatorAcceptsEmptyValue_passesValidation() {
        assertPasses("", new EmailValidator("this should not fail", true));
    }

    @Test
    void emptyString_validatorDoesNotAcceptsEmptyValue_validationFails() {
        assertFails("", new EmailValidator(
                "explcitily disallowed empty value should not be accepted",
                false));
    }

    @Test
    void testDomainWithDotDotFails() {
        EmailValidator v = validator("domains containing dot dot should fail");
        assertFails("hello@sample..com", v);
        assertFails("hello@samp..le..com", v);
    }

    private EmailValidator validator(String errorMessage) {
        return new EmailValidator(errorMessage);
    }

    private EmailValidator shouldNotFail() {
        return validator("this should not fail");
    }
}
