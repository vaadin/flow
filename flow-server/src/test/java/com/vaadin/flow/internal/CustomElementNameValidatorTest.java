/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.CustomElementNameValidator;

/**
 * Test CustomElementNameValidator that it validates correctly.
 */
public class CustomElementNameValidatorTest {

    @Test
    public void testInvalidElementNames() {
        Stream.of("", "foo", "annotation-xml", "0-foo", "-foo", "foo-$",
                "foo-/", "FOO-BAR", "foo/", "øl-unicorn", "foo-💩")
                .forEach(name -> Assert.assertFalse(String.format(
                        "Name %s is valid even though it should not be", name),
                        CustomElementNameValidator.isCustomElementName(name)));
    }

    @Test
    public void testValidNamesWithoutErrorOrWarning() {
        Stream.of("foo-bar", "custom-element", "date-field", "dos-box")
                .forEach(name -> Assert.assertTrue(String.format(
                        "Name %s is not valid even though it should be", name),
                        CustomElementNameValidator.isCustomElementName(name)));
    }

    @Test
    public void testValidButWithWarning() {
        Stream.of("polymer-", "x-", "ng-", "unicorn-", "unicorn-ø", "uni--corn",
                "uni-----corn", "uni-co___rn", "uni-co.rn", "uni-corné",
                "xml-unicorn", "não-tém", "foo-bår")
                .forEach(name -> Assert.assertTrue(String.format(
                        "Name %s is not valid even though it should be", name),
                        CustomElementNameValidator.isCustomElementName(name)));

    }
}
