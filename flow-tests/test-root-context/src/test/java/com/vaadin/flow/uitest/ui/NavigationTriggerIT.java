/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NavigationTriggerIT extends ChromeBrowserTest {

    @Test
    public void testNavigationTriggers() {
        String url = getTestURL() + "/abc/";
        getDriver().get(url);

        assertMessageCount(1);

        assertLastMessage("/abc", NavigationTrigger.PAGE_LOAD, "abc");
        Assert.assertEquals("The trailing '/' from the URL should be removed.",
                url.substring(0, url.length() - 1),
                getDriver().getCurrentUrl());

        findElement(By.id("routerlink")).click();
        assertMessageCount(2);
        assertLastMessage("/routerlink", NavigationTrigger.ROUTER_LINK,
                "routerlink");

        findElement(By.id("routerlink-button")).click();
        assertMessageCount(3);
        assertLastMessage("/routerlink-button", NavigationTrigger.ROUTER_LINK,
                "routerlink-button");

        findElement(By.id("navigate")).click();
        assertMessageCount(4);
        assertLastMessage("/navigate", NavigationTrigger.UI_NAVIGATE,
                "navigate");

        getDriver().navigate().back();
        assertMessageCount(5);
        assertLastMessage("/routerlink-button", NavigationTrigger.HISTORY,
                "routerlink-button");

        getDriver().navigate().forward();
        assertMessageCount(6);
        assertLastMessage("/navigate", NavigationTrigger.HISTORY, "navigate");

        findElement(By.id("forwardButton")).click();
        assertMessageCount(7);
        assertLastMessage("/forwarded", NavigationTrigger.PROGRAMMATIC,
                "forwarded");

        findElement(By.id("rerouteButton")).click();
        assertMessageCount(8);
        assertLastMessage("/rerouted", NavigationTrigger.PROGRAMMATIC,
                "rerouted");
    }

    private void assertLastMessage(String path, NavigationTrigger trigger,
            String parameter) {
        List<WebElement> messages = getMessages();
        String lastMessageText = messages.get(messages.size() - 1).getText();

        Assert.assertEquals(
                NavigationTriggerView.buildMessage(path, trigger, parameter),
                lastMessageText);
    }

    private void assertMessageCount(int count) {
        Assert.assertEquals(count, getMessages().size());
    }

    private List<WebElement> getMessages() {
        return findElements(By.cssSelector(".message"));
    }
}
