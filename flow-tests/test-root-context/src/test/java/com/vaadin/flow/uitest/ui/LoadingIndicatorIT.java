/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class LoadingIndicatorIT extends ChromeBrowserTest {

    @Test
    public void ensureSecondStyleWorks() throws InterruptedException {
        open("first=100", "second=1000", "third=100000");
        WebElement loadingIndicator = findElement(
                By.className("v-loading-indicator"));
        testBench().disableWaitForVaadin();
        findElement(By.id("wait5000")).click();
        Assert.assertFalse(hasCssClass(loadingIndicator, "second"));
        Thread.sleep(2000);
        Assert.assertTrue(hasCssClass(loadingIndicator, "second"));
    }

    @Test
    public void byDefault_loadingIndicator_usesDefaultTheme()
            throws InterruptedException {
        open();

        WebElement loadingIndicator = findElement(
                By.className("v-loading-indicator"));

        Assert.assertEquals(
                "Default loading indicator theming should be applied", "4px",
                loadingIndicator.getCssValue("height"));

        // if the next part of the test gets unstable in some environment, just
        // delete it
        testBench().disableWaitForVaadin();
        findElement(By.id("wait10000")).click();

        Thread.sleep(6000);

        // during third stage (wait) the height is bumped to 7px
        Assert.assertEquals("Default loading indicator theming is not applied",
                "7px", loadingIndicator.getCssValue("height"));
    }

    @Test
    public void loadingIndicator_switchingToCustomTheme_noDefaultThemeApplied()
            throws InterruptedException {
        open();

        WebElement loadingIndicator = findElement(
                By.className("v-loading-indicator"));

        // Check that default theme is applied
        Assert.assertEquals(
                "Default loading indicator theming should be applied",
                "4px", loadingIndicator.getCssValue("height"));
        int count = findElements(By.cssSelector("head > style")).size();

        // Removes the style tag with default theme from the client
        findElement(By.id("disable-theme")).click();

        // Check that one style tag was removed from head
        Assert.assertEquals("One style tag should be removed",
                1, count - findElements(By.cssSelector("head > style")).size());
        // Check that default theme is not being applied
        Assert.assertEquals(
                "Default loading indicator theming should not be applied",
                "auto", loadingIndicator.getCssValue("height"));
    }
}
