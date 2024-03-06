/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BundlesIT extends ChromeBrowserTest {

    @Test
    public void bundlesIsNotUsed() {
        getDriver().get(getRootURL());
        waitForClientRouter();
        Assert.assertFalse((Boolean) $("testscope-button").first()
                .getProperty("isFromBundle"));
    }

}
