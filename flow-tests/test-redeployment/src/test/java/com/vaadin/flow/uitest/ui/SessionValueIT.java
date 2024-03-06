/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class SessionValueIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return super.getTestPath().replace("/view", "");
    }

    @Test
    public void sessionValuePreservedOnReload() throws InterruptedException {
        open();
        TestBenchElement div = $("div").id("customAttribute");
        String customAttribute = div.getText()
                .replace("The custom value in the session is: ", "");

        // trigger reload
        findElement(By.id(WebpackDevServerPortView.TRIGGER_RELOAD_ID)).click();

        waitForElementPresent(By.id("customAttribute"));
        div = $("div").id("customAttribute");
        String customAttributeAfterReload = div.getText()
                .replace("The custom value in the session is: ", "");
        Assert.assertEquals(customAttribute, customAttributeAfterReload);

        // trigger reload
        findElement(By.id(WebpackDevServerPortView.TRIGGER_RELOAD_ID)).click();

        waitForElementPresent(By.id("customAttribute"));
        div = $("div").id("customAttribute");
        String customAttributeAfterSecondReload = div.getText()
                .replace("The custom value in the session is: ", "");
        Assert.assertEquals(customAttribute, customAttributeAfterSecondReload);

    }
}
