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
package com.vaadin.flow.test.components;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(LoadingIndicatorView.class)
public class LoadingIndicatorIT extends AbstractDefaultIT {

    @BrowserTest
    public void ensureSecondStyleWorks() throws InterruptedException {
        open();
        WebElement loadingIndicator = findElement(
                By.className("v-loading-indicator"));
        testBench().disableWaitForVaadin();
        findElement(By.id("wait5000")).click();
        assertFalse(hasCssClass(loadingIndicator, "second"));
        Thread.sleep(2000);
        assertTrue(hasCssClass(loadingIndicator, "second"));
    }

    @BrowserTest
    public void byDefault_loadingIndicator_usesDefaultTheme()
            throws InterruptedException {
        open();

        WebElement loadingIndicator = findElement(
                By.className("v-loading-indicator"));

        Assertions.assertEquals("4px", loadingIndicator.getCssValue("height"),
                "Default loading indicator theming should be applied");

        // if the next part of the test gets unstable in some environment, just
        // delete it
        testBench().disableWaitForVaadin();
        findElement(By.id("wait10000")).click();

        Thread.sleep(6000);

        // during third stage (wait) the height is bumped to 7px
        Assertions.assertEquals("7px", loadingIndicator.getCssValue("height"),
                "Default loading indicator theming is not applied");
    }

    @BrowserTest
    public void loadingIndicator_switchingToCustomTheme_noDefaultThemeApplied()
            throws InterruptedException {
        open();

        WebElement loadingIndicator = findElement(
                By.className("v-loading-indicator"));

        // Check that default theme is applied
        Assertions.assertEquals("4px", loadingIndicator.getCssValue("height"),
                "Default loading indicator theming should be applied");
        int count = findElements(By.cssSelector("head > style")).size();

        // Removes the style tag with default theme from the client
        findElement(By.id("disable-theme")).click();

        // Check that one style tag was removed from head
        Assertions.assertEquals(1,
                count - findElements(By.cssSelector("head > style")).size(),
                "One style tag should be removed");
        // Check that default theme is not being applied
        Assertions.assertEquals("auto", loadingIndicator.getCssValue("height"),
                "Default loading indicator theming should not be applied");
    }

    @BrowserTest
    public void fastRequest_indicatorNotShown() throws InterruptedException {
        open();
        WebElement loadingIndicator = findElement(
                By.className("v-loading-indicator"));
        testBench().disableWaitForVaadin();

        // Click button that triggers a fast request (100ms)
        findElement(By.id("wait100")).click();

        // Wait for the request to complete (200ms to be safe)
        Thread.sleep(200);

        // Verify indicator never showed (should not have "first" class)
        // The default firstDelay is 450ms, so a 100ms request should complete
        // before the indicator appears
        assertFalse(hasCssClass(loadingIndicator, "first"),
                "Loading indicator should not show for fast requests");
    }

    @BrowserTest
    public void slowRequest_indicatorShown() throws InterruptedException {
        open();
        WebElement loadingIndicator = findElement(
                By.className("v-loading-indicator"));
        testBench().disableWaitForVaadin();

        // Click button that triggers a slow request (1000ms)
        findElement(By.id("wait1000")).click();

        // Wait past the default firstDelay threshold (450ms)
        Thread.sleep(600);

        // Verify indicator is showing (should have "first" class)
        assertTrue(hasCssClass(loadingIndicator, "first"),
                "Loading indicator should show for slow requests");
    }
}
