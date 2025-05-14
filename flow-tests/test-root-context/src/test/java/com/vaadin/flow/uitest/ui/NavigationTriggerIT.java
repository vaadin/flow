/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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

        findElement(By.id("navigate")).click();
        assertMessageCount(3);
        assertLastMessage("/navigate", NavigationTrigger.UI_NAVIGATE,
                "navigate");

        getDriver().navigate().back();
        assertMessageCount(4);
        assertLastMessage("/routerlink", NavigationTrigger.HISTORY,
                "routerlink");

        getDriver().navigate().forward();
        assertMessageCount(5);
        assertLastMessage("/navigate", NavigationTrigger.HISTORY, "navigate");

        findElement(By.id("forwardButton")).click();
        assertMessageCount(6);
        assertLastMessage("/forwarded", NavigationTrigger.PROGRAMMATIC,
                "forwarded");

        findElement(By.id("rerouteButton")).click();
        assertMessageCount(7);
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
