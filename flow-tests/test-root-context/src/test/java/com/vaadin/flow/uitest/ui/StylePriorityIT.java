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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class StylePriorityIT extends ChromeBrowserTest {

    @Test
    public void noParameters() {
        open();
        WebElement div = findElement(By.id("priority-style"));

        Assert.assertEquals("display: block !important;",
                div.getAttribute("style"));
    }
}
