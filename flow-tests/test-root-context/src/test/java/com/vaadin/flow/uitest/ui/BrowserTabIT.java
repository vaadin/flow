/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.uitest.ui.BrowserTabView.TOKEN_ID;

public class BrowserTabIT extends ChromeBrowserTest {

    @Test
    public void reloadAndFullPageNavigation_sameTab_attributeIsKept() {
        open();
        String token = getToken();

        open();
        Assert.assertEquals("Reload should keep the tab attribute", token,
                getToken());

        getDriver()
                .get(getRootURL() + "/view/com.vaadin.flow.uitest.ui.PageView");
        waitForElementPresent(By.id("input"));
        open();
        Assert.assertEquals(
                "Navigating away and back should keep the tab attribute", token,
                getToken());
    }

    @Test
    public void otherTab_attributeIsNotShared() {
        open();
        String firstWindow = getDriver().getWindowHandle();
        String token = getToken();

        ((JavascriptExecutor) getDriver()).executeScript(
                "window.open(arguments[0], '_blank');", getTestURL());
        String secondWindow = getDriver().getWindowHandles().stream()
                .filter(handle -> !handle.equals(firstWindow)).findFirst()
                .orElseThrow();
        getDriver().switchTo().window(secondWindow);

        Assert.assertNotEquals("Another tab should get its own attributes",
                token, getToken());
    }

    private String getToken() {
        waitForElementPresent(By.id(TOKEN_ID));
        return findElement(By.id(TOKEN_ID)).getText();
    }
}
