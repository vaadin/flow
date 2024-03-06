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

public class ErrorPageIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/view/abcd";
    };

    @Test
    public void testErrorViewOpened() {
        open();

        Assert.assertTrue(getDriver().getPageSource()
                .contains("Could not navigate to 'abcd'"));

        getDriver().get(getTestURL() + "/foobar");

        Assert.assertTrue(getDriver().getPageSource()
                .contains("Could not navigate to 'abcd/foobar'"));
    }
}
