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

import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class CustomBrowserTooOldPageIT extends ChromeBrowserTest {

    @Test
    public void customPageUsed() {
        // There needs to be a session for the "too old page" to be shown
        getDriver().get(getRootURL() + "/view/");
        getDriver().get(getRootURL() + "/view/?v-r="
                + RequestType.BROWSER_TOO_OLD.getIdentifier());
        String pageSource = getDriver().getPageSource();
        Assert.assertTrue(
                pageSource.contains("You so old you cannot view this page"));
    }

}
