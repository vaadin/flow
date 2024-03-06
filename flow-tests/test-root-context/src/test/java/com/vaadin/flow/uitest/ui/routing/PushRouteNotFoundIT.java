/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.routing;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Category(IgnoreOSGi.class)
public class PushRouteNotFoundIT extends ChromeBrowserTest {

    @Test
    public void renderRouteNotFoundErrorPage_pushIsSpecifiedViaParentLayout() {
        open();

        TestBenchElement push = $(TestBenchElement.class).id("push-layout")
                .$(TestBenchElement.class).id("push-mode");
        Assert.assertEquals("Push mode: AUTOMATIC", push.getText());
    }

    @Test
    public void renderRouteNotFoundErrorPage_parentLayoutReroute_reroutingIsDone() {
        String url = getTestURL(getRootURL(),
                doGetTestPath(PushLayout.FORWARD_PATH), new String[0]);

        getDriver().get(url);
        waitForDevServer();
        waitUntil(driver -> driver.getCurrentUrl()
                .endsWith(ForwardPage.class.getName()));

        Assert.assertTrue(isElementPresent(By.id("forwarded")));
    }

    @Override
    protected String getTestPath() {
        return doGetTestPath(PushRouteNotFoundView.PUSH_NON_EXISTENT_PATH);
    }

    private String doGetTestPath(String uri) {
        String path = super.getTestPath();
        int index = path.lastIndexOf("/");
        return path.substring(0, index + 1) + uri;
    }
}
