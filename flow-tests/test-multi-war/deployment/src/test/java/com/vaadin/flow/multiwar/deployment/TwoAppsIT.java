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
package com.vaadin.flow.multiwar.deployment;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TwoAppsIT extends ChromeBrowserTest {
    @Override
    protected String getTestPath() {
        return "/both.html";
    }

    @Test
    public void testWar1Works() {
        testWarWorks("war1");
    }

    @Test
    public void testWar2Works() {
        testWarWorks("war2");
    }

    private void testWarWorks(String warId) {
        getDriver().get(
                getTestURL(getRootURL(), "/test-" + warId, new String[] {}));
        waitForDevServer();
        WebElement helloText = findElement(By.id("hello"));
        Assert.assertEquals(
                "Hello from com.vaadin.flow.multiwar." + warId + ".MainView",
                helloText.getText());
        helloText.click();
        Assert.assertEquals("Hello Hello from com.vaadin.flow.multiwar." + warId
                + ".MainView", helloText.getText());

    }

    @Test
    public void bothWebComponentsEmbedded() {
        open();
        waitForWebComponentsBootstrap();
        WebElement hello1 = findElement(By.id("hello1"));
        WebElement hello2 = findElement(By.id("hello2"));

        Assert.assertEquals(
                "Hello from com.vaadin.flow.multiwar.war1.HelloComponent",
                hello1.getText());
        Assert.assertEquals(
                "Hello from com.vaadin.flow.multiwar.war2.HelloComponent",
                hello2.getText());
    }

}
