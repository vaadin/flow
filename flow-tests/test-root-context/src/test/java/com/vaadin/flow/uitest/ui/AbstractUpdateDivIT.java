/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public abstract class AbstractUpdateDivIT extends ChromeBrowserTest {

    @Test
    public void pushUpdatesEmbeddedWebComponent() {
        open();

        int initialUpdateCount = getUpdateCount();
        Assert.assertTrue(
                "The initial update count should be less than maximum 50, but it has value "
                        + initialUpdateCount,
                initialUpdateCount < 50);

        waitUntil(driver -> getUpdateCount() > initialUpdateCount, 5);

        int nextUpdateCount = getUpdateCount();

        Assert.assertTrue(
                "The next interim update count should be less than maximum 50, but it has value "
                        + nextUpdateCount,
                nextUpdateCount < 50);

        waitUntil(driver -> getUpdateCount() == 50, 5);

        int updateCount = getUpdateCount();
        Assert.assertEquals(
                "The update count should have reached the maximin 50, but it has value "
                        + updateCount,
                50, updateCount);
    }

    private int getUpdateCount() {
        TestBenchElement div = $(TestBenchElement.class).id("push-update");
        String count = div.getText();
        return Integer.parseInt(count);
    }
}
