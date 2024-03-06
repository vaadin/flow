/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.scroll;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ServerRequestScrollIT extends ChromeBrowserTest {

    @Test
    public void scrollPositionIsTheSameAfterServerRequest() {
        open();

        WebElement button = $("server-request").id("template")
                .$(NativeButtonElement.class).first();

        int y = button.getLocation().getY();

        scrollBy(0, y);

        int scrollY = getScrollY();

        Assert.assertTrue(scrollY > 0);

        button.click();

        Assert.assertEquals(
                "Scroll position after the server request is changed", scrollY,
                getScrollY());
    }
}
