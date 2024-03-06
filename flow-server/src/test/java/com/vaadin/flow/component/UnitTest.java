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

public class UnitTest {

    @Test
    public void getUnit() {
        Assert.assertFalse(Unit.getUnit(null).isPresent());
        Assert.assertFalse(Unit.getUnit("").isPresent());
        Assert.assertFalse(Unit.getUnit("10unknown").isPresent());

        Assert.assertEquals(Unit.PERCENTAGE, Unit.getUnit("100%").get());
        Assert.assertEquals(Unit.PIXELS, Unit.getUnit("100px").get());
    }
}
