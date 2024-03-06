/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import org.junit.Assert;
import org.junit.Test;

public class AnchorTargetValueTest {

    @Test
    public void fromString_notEnum_objectHasValueAndEquals() {
        AnchorTargetValue value = AnchorTargetValue.forString("foo");
        Assert.assertEquals("foo", value.getValue());

        AnchorTargetValue value1 = AnchorTargetValue.forString("foo");
        Assert.assertEquals(value, value1);
        Assert.assertEquals(value.hashCode(), value1.hashCode());
    }

    @Test
    public void fromString_enumValue_resultIsEnum() {
        AnchorTargetValue value = AnchorTargetValue
                .forString(AnchorTarget.TOP.getValue());
        Assert.assertEquals(AnchorTarget.TOP, value);
    }
}
