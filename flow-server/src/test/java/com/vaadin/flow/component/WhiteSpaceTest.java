/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.HasText.WhiteSpace;

public class WhiteSpaceTest {

    @Test
    public void toString_styleValueIsReturned() {
        Assert.assertEquals("nowrap", WhiteSpace.NOWRAP.toString());
        Assert.assertEquals("pre-line", WhiteSpace.PRE_LINE.toString());
    }

    @Test
    public void forString_enumIsReturned() {
        Assert.assertEquals(WhiteSpace.NORMAL, WhiteSpace.forString("normal"));
        Assert.assertEquals(WhiteSpace.PRE_WRAP,
                WhiteSpace.forString("pre-wrap"));
    }
}
