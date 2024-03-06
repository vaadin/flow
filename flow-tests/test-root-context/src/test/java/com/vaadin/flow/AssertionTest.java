/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test that tests are run with assertions activated
 */
public class AssertionTest {

    @Test
    public void testAssertionsAreEnabled() {
        boolean assertOn = false;
        // *assigns* true if assertions are on.
        assert assertOn = true;

        Assert.assertTrue(
                "Assertions are turned off for the root-context package",
                assertOn);
    }
}
