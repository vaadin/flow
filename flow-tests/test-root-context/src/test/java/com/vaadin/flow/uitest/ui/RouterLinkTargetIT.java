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

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class RouterLinkTargetIT extends ChromeBrowserTest {

    @Test
    public void openInNewBrowserTab_routeOpensInNewTab() {
        open();
        String firstWindow = getDriver().getWindowHandle();
        String url = getDriver().getCurrentUrl();

        findElement(By.id("new-tab-link")).click();

        waitUntil(driver -> driver.getWindowHandles().size() == 2);
        Assert.assertEquals(url, getDriver().getCurrentUrl());
        Assert.assertTrue(isElementPresent(By.id("new-tab-link")));

        String secondWindow = getDriver().getWindowHandles().stream()
                .filter(handle -> !handle.equals(firstWindow)).findFirst()
                .get();
        getDriver().switchTo().window(secondWindow);
        waitForElementPresent(By.id("target"));
        Assert.assertTrue(getDriver().getCurrentUrl().endsWith(
                RouterLinkTargetView.TargetView.class.getSimpleName()));
    }

    @Test
    public void selfTarget_navigatesInPlace() {
        open();

        findElement(By.id("self-link")).click();

        waitForElementPresent(By.id("target"));
        Assert.assertEquals(1, getDriver().getWindowHandles().size());
        Assert.assertTrue(getDriver().getCurrentUrl().endsWith(
                RouterLinkTargetView.TargetView.class.getSimpleName()));
    }
}
