/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test CustomElementNameValidator that it validates correctly.
 */
public class CustomElementNameValidatorTest {

    @Test
    public void testInvalidElementNames() {
        Stream.of("", "foo", "annotation-xml", "0-foo", "-foo", "foo-$",
                "foo-/", "FOO-BAR", "foo/", "øl-unicorn", "foo-💩",
                "5th-element")
                .forEach(name -> Assert.assertFalse(String.format(
                        "Name %s is valid even though it should not be", name),
                        CustomElementNameValidator.isCustomElementName(name)));
    }

    @Test
    public void testValidNamesWithoutErrorOrWarning() {
        Stream.of("foo-bar", "custom-element", "date-field", "dos-box",
                "home-4-good")
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
