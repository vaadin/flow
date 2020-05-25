/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PreserveOnRefreshShortcutIT extends ChromeBrowserTest {

    @Test
    public void replaceComponentAfterRefresh_componentIsReplaced() {
        open();
        if (hasClientIssue("7587")) {
            return;
        }

        waitPageLoad();

        new Actions(getDriver()).sendKeys(Keys.ENTER).build().perform();
        List<WebElement> infos = findElements(By.className("info"));

        Assert.assertEquals(1, infos.size());
        Assert.assertEquals("Clicked", infos.get(0).getText());

        open();

        waitPageLoad();

        new Actions(getDriver()).sendKeys(Keys.ENTER).build().perform();
        infos = findElements(By.className("info"));

        Assert.assertEquals(2, infos.size());
        Assert.assertEquals("Clicked", infos.get(1).getText());

    }

    private void waitPageLoad() {
        waitUntil(driver -> !findElement(By.className("v-loading-indicator"))
                .isDisplayed());

        waitForElementVisible(By.id("trigger"));
    }

}
