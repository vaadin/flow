/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.validator;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class StringLengthValidatorTest extends ValidatorTestBase {

    private static final String LONG_STRING = Stream.generate(() -> "x")
            .limit(1000).collect(Collectors.joining());

    @Test
    public void testNullStringFails() {
        assertPasses(null, new StringLengthValidator("", 0, 10));
    }

    @Test
    public void testMaxLengthTooLongStringFails() {
        assertFails(LONG_STRING,
                new StringLengthValidator("Should be at most 10", null, 10));
    }

    @Test
    public void testMaxLengthStringPasses() {
        assertPasses(LONG_STRING, new StringLengthValidator(
                "Should be at most 1000", null, 1000));
    }

    @Test
    public void testMinLengthEmptyStringFails() {
        assertFails("",
                new StringLengthValidator("Should be at least 1", 1, null));
    }

    @Test
    public void testMinLengthStringPasses() {
        assertPasses("å",
                new StringLengthValidator("Should be at least 1", 1, null));
    }
}
