/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class InvalidLocationIT extends ChromeBrowserTest {

    // #9443
    @Test
    public void invalidCharactersOnPath_UiNotServed() {
        open();

        Assert.assertTrue("Faulty URL didn't return 400 error page.",
                getDriver().getPageSource().contains("400"));
    }

    @Override
    protected String getTestPath() {
        return "/view/..**";
    }
}
