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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.testutil.AbstractParallelTestBenchTest;

public class RouterParallelIT extends AbstractParallelTestBenchTest {

    @Before
    public void openAndFindElement() {
        getDriver().get(getRootURL() + "/new-router-session/ElementQueryView");
        waitForDevServer();
    }

    @Test
    public void a() {
        Assert.assertTrue("No button was found on page",
                isElementPresent(By.tagName(Tag.BUTTON)));
    }

    @Test
    public void b() {
        Assert.assertTrue("No button was found on page",
                isElementPresent(By.tagName(Tag.BUTTON)));
    }

    @Test
    public void c() {
        Assert.assertTrue("No button was found on page",
                isElementPresent(By.tagName(Tag.BUTTON)));
    }

    @Test
    public void d() {
        Assert.assertTrue("No button was found on page",
                isElementPresent(By.tagName(Tag.BUTTON)));
    }

    @Test
    public void e() {
        Assert.assertTrue("No button was found on page",
                isElementPresent(By.tagName(Tag.BUTTON)));
    }

}
