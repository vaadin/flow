/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.typeconversion;

import org.junit.Test;

public class StringConversionTest extends BaseTypeConversionTest {

    @Test
    public void should_ConvertToString_When_ReceiveAString() {
        assertEqualExpectedValueWhenCallingMethod("addFooString",
                "\"some-string\"", "\"some-stringfoo\"");
        assertEqualExpectedValueWhenCallingMethod("addFooString", "\"null\"",
                "\"nullfoo\"");
        assertEqualExpectedValueWhenCallingMethod("addFooString", "null",
                "\"nullfoo\"");
    }

}
