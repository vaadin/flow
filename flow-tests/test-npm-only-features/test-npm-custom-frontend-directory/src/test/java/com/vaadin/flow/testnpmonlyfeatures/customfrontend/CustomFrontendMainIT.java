/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.customfrontend;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class CustomFrontendMainIT extends ChromeBrowserTest {
    @Test
    public void javascriptShouldHaveBeenExecuted() {
        open();
        Assert.assertNotNull($("div").id("executed"));
    }
}
