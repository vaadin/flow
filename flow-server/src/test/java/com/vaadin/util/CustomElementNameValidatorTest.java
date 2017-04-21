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
package com.vaadin.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test CustomElementNameValidator that it validates correctly.
 */
public class CustomElementNameValidatorTest {

    @Test
    public void testInvalidElementNames() {
        List<String> invalidNames = Arrays.asList("", "foo", "annotation-xml",
                "0-foo", "-foo", "foo-$", "foo-/", "FOO-BAR", "foo/",
                "øl-unicorn", "foo-💩");

        invalidNames.forEach(name -> Assert.assertFalse(
                String.format("Name %s is valid even though it should not be",
                        name),
                CustomElementNameValidator.isValidCustomElementName(name)));

    }

    @Test
    public void testValidNamesWithoutErrorOrWarning() {
        List<String> validNames = Arrays.asList("foo-bar", "custom-element",
                "date-field", "dos-box");
        validNames.forEach(name -> assertNoErrorOrWarning(name));
    }

    private void assertNoErrorOrWarning(String name) {
        CustomElementNameValidator.Result result = CustomElementNameValidator
                .validate(name);
        Assert.assertTrue(String.format(
                "Got invalid for valid name for %s with error: '%s'", name,
                result.getError()), result.isValid());
        Assert.assertTrue(
                String.format("Recieved error: '%s' for valid name %s",
                        result.getError(), name),
                result.getError().isEmpty());
        Assert.assertTrue(
                String.format("Recieved warning: '%s' for valid name %s",
                        result.getWarning(), name),
                result.getWarning().isEmpty());

    }

    @Test
    public void testValidButWithWarning() {
        List<String> warning = Arrays.asList("polymer-", "x-", "ng-",
                "unicorn-", "unicorn-ø", "uni--corn", "uni-----corn",
                "uni-co___rn", "uni-co.rn", "uni-corné", "xml-unicorn",
                "não-tém", "foo-bår");

        warning.forEach(name -> assertValidButWithWarning(name));

    }

    private void assertValidButWithWarning(String name) {
        CustomElementNameValidator.Result result = CustomElementNameValidator
                .validate(name);
        Assert.assertTrue(String.format(
                "Got invalid for valid name for %s with error: '%s'", name,
                result.getError()), result.isValid());
        Assert.assertTrue(
                String.format("Recieved error: '%s' for valid name %s",
                        result.getError(), name),
                result.getError().isEmpty());
        Assert.assertFalse(
                String.format("Name %s was missing an expected warning", name),
                result.getWarning().isEmpty());
    }
}