/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.custom;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class CustomRouteIT extends ChromeBrowserTest {

    @Test
    public void CustomRegistry_hasExpectedErrorHandlers() {
        getDriver().get(getRootURL());

        final SpanElement notFoundException = $(SpanElement.class)
                .id("NotFoundException");
        Assert.assertEquals("Wrong error handler registered",
                "NotFoundException :: CustomNotFoundView",
                notFoundException.getText());

        try {
            $(SpanElement.class).id("IllegalAccessException");
            Assert.fail(
                    "Found IllegalAccessException error handler even though it should not be registered");
        } catch (NoSuchElementException nsee) {
            // NO-OP as this should throw element not found
        }
    }

    @Test
    public void testCustomErrorView() {
        getDriver().get(getRootURL() + "/none");
        final SpanElement error = $(SpanElement.class).id("error");
        Assert.assertEquals("Requested route was simply not found!",
                error.getText());
    }
}
