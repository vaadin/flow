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

public class BeanConversionTest extends BaseTypeConversionTest {

    @Test
    public void should_ConvertToBean_When_ReceiveBeanObject() {
        String inputValue = "{\"name\":\"mybean\",\"address\":\"myaddress\","
                + "\"age\":10,\"isAdmin\":true,\"testEnum\":\"FIRST\","
                + "\"roles\":[\"Admin\"], \"customProperty\": \"customValue\"}";
        String expectedValue = "{\"name\":\"mybean-foo\","
                + "\"address\":\"myaddress-foo\","
                + "\"age\":11,\"isAdmin\":false," + "\"testEnum\":\"SECOND\","
                + "\"roles\":[\"Admin\",\"User\"],\"customProperty\":\"customValue-foo\"}";
        assertEqualExpectedValueWhenCallingMethod("getFooBean", inputValue,
                expectedValue);
    }
}
