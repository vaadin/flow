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
package com.vaadin.flow.test.dom;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.fail;

@TestFor(PageView.class)
public class PageIT extends AbstractDefaultIT {

    @BrowserTest
    public void testPageTitleUpdates() {
        open();

        updateTitle("Page title 1");
        verifyTitle("Page title 1");

        updateTitle("FOObar");
        verifyTitle("FOObar");
    }

    @BrowserTest
    public void testOnlyMostRecentPageUpdate() {
        open();

        updateTitle("Page title 1");
        verifyTitle("Page title 1");

        findElement(By.id("input")).sendKeys("title 2" + Keys.TAB);
        findElement(By.id("override")).click();

        verifyTitle("OVERRIDDEN");
    }

    @BrowserTest
    public void testPageTitleClears() {
        open();

        findElement(By.id("override")).click();
        verifyTitle("OVERRIDDEN");

        updateTitle("");
        verifyTitle("");
    }

    private void verifyTitle(String title) {
        try {
            waitUntil(driver -> driver.getTitle().equals(title));
        } catch (TimeoutException te) {
            fail("Page title does not match. Expected: " + title + ", Actual: "
                    + getDriver().getTitle());
        }
    }

    private void updateTitle(String title) {
        $(InputTextElement.class).id("input").setValue(title);
        findElement(By.id("button")).click();
    }

    @BrowserTest
    public void testReload() {
        open();

        InputTextElement input = $(InputTextElement.class).id("input");
        input.setValue("foo");
        Assertions.assertEquals("foo", input.getPropertyString("value"));
        WebElement oldInput = findElement(By.id("input"));
        $(DivElement.class).id("reload").click();
        // Wait for the old element to become stale (page unloads)
        waitUntil(ExpectedConditions.stalenessOf(oldInput));
        // Wait for the new page to load and the input element to reappear
        waitForElementPresent(By.id("input"));
        input = $(InputTextElement.class).id("input");
        Assertions.assertEquals("", input.getValue());
    }

    @BrowserTest
    public void testSetLocation() {
        open();

        findElement(By.id("setLocation")).click();
        waitUntil(driver -> driver.getCurrentUrl().endsWith("/visibility"));
    }

    @BrowserTest
    public void testOpenUrlInNewTab() {
        open();

        findElement(By.id("open")).click();
        waitUntil(driver -> driver.getWindowHandles().size() == 2);
        List<String> tabs = new ArrayList<>(getDriver().getWindowHandles());
        tabs.remove(getDriver().getWindowHandle());
        getDriver().switchTo().window(tabs.get(0));
        waitUntil(driver -> driver.getCurrentUrl().endsWith("/visibility"));
    }

    @BrowserTest
    public void testOpenUrlInIFrame() {
        open();

        findElement(By.id("openInIFrame")).click();

        waitUntil(driver -> getIframeUrl().endsWith("/visibility"));
    }

    @BrowserTest
    public void fetchPageDirection_noDirectionSetExplicitly_leftToRightIsPassedToCallback() {
        open();

        Assertions.assertEquals("",
                findElement(By.id("direction-value")).getText());

        findElement(By.id("fetch-direction")).click();

        Assertions.assertEquals("LEFT_TO_RIGHT",
                findElement(By.id("direction-value")).getText());
    }

    @BrowserTest
    public void fetchPageDirection_setRTLDirection_rightToLeftIsPassedToCallback() {
        open();

        Assertions.assertEquals("",
                findElement(By.id("direction-value")).getText());

        findElement(By.id("set-RTL-direction")).click();

        findElement(By.id("fetch-direction")).click();

        Assertions.assertEquals("RIGHT_TO_LEFT",
                findElement(By.id("direction-value")).getText());
    }

    @BrowserTest
    public void fetchPageDirection_setLTRDirection_leftToRightIsPassedToCallback() {
        open();

        Assertions.assertEquals("",
                findElement(By.id("direction-value")).getText());

        findElement(By.id("set-LTR-direction")).click();

        findElement(By.id("fetch-direction")).click();

        Assertions.assertEquals("LEFT_TO_RIGHT",
                findElement(By.id("direction-value")).getText());
    }

    private String getIframeUrl() {
        return (String) ((JavascriptExecutor) getDriver()).executeScript(
                "return document.getElementById('newWindow').contentWindow.location.href;");
    }
}
