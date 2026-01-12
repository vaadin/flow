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

import java.util.ArrayList;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PageIT extends ChromeBrowserTest {

    @Test
    public void testPageTitleUpdates() {
        open();

        updateTitle("Page title 1");
        verifyTitle("Page title 1");

        updateTitle("FOObar");
        verifyTitle("FOObar");
    }

    @Test
    public void testOnlyMostRecentPageUpdate() {
        open();

        updateTitle("Page title 1");
        verifyTitle("Page title 1");

        findElement(By.id("input")).sendKeys("title 2" + Keys.TAB);
        findElement(By.id("override")).click();

        verifyTitle("OVERRIDDEN");
    }

    @Test
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
            Assert.fail("Page title does not match. Expected: " + title
                    + ", Actual: " + driver.getTitle());
        }
    }

    private void updateTitle(String title) {
        $(InputTextElement.class).id("input").setValue(title);
        findElement(By.id("button")).click();
    }

    @Test
    public void testReload() {
        open();

        InputTextElement input = $(InputTextElement.class).id("input");
        input.setValue("foo");
        Assert.assertEquals("foo", input.getPropertyString("value"));
        $(DivElement.class).id("reload").click();
        input = $(InputTextElement.class).id("input");
        Assert.assertEquals("", input.getValue());
    }

    @Test
    @Ignore("Ignored because of fusion issue: https://github.com/vaadin/flow/issues/7575")
    public void testSetLocation() {
        open();

        findElement(By.id("setLocation")).click();
        Assert.assertThat(getDriver().getCurrentUrl(),
                Matchers.endsWith(BaseHrefView.class.getName()));
    }

    @Test
    @Ignore("Ignored because of fusion issue: https://github.com/vaadin/flow/issues/7575")
    public void testOpenUrlInNewTab() {
        open();

        findElement(By.id("open")).click();
        ArrayList<String> tabs = new ArrayList<>(
                getDriver().getWindowHandles());
        Assert.assertThat(
                getDriver().switchTo().window(tabs.get(1)).getCurrentUrl(),
                Matchers.endsWith(BaseHrefView.class.getName()));
    }

    @Test
    @Ignore("Ignored because of fusion issue: https://github.com/vaadin/flow/issues/7575")
    public void testOpenUrlInIFrame() throws InterruptedException {
        open();

        findElement(By.id("openInIFrame")).click();

        waitUntil(driver -> !getIframeUrl().equals("about:blank"));

        Assert.assertThat(getIframeUrl(),
                Matchers.endsWith(BaseHrefView.class.getName()));
    }

    @Test
    public void fetchPageDirection_noDirectionSetExplicitly_leftToRightIsPassedToCallback() {
        open();

        Assert.assertEquals("",
                findElement(By.id("direction-value")).getText());

        findElement(By.id("fetch-direction")).click();

        Assert.assertEquals("LEFT_TO_RIGHT",
                findElement(By.id("direction-value")).getText());
    }

    @Test
    public void fetchPageDirection_setRTLDirection_rightToLeftIsPassedToCallback() {
        open();

        Assert.assertEquals("",
                findElement(By.id("direction-value")).getText());

        findElement(By.id("set-RTL-direction")).click();

        findElement(By.id("fetch-direction")).click();

        Assert.assertEquals("RIGHT_TO_LEFT",
                findElement(By.id("direction-value")).getText());
    }

    @Test
    public void fetchPageDirection_setLTRDirection_leftToRightIsPassedToCallback() {
        open();

        Assert.assertEquals("",
                findElement(By.id("direction-value")).getText());

        findElement(By.id("set-LTR-direction")).click();

        findElement(By.id("fetch-direction")).click();

        Assert.assertEquals("LEFT_TO_RIGHT",
                findElement(By.id("direction-value")).getText());
    }

    private String getIframeUrl() {
        return (String) ((JavascriptExecutor) driver).executeScript(
                "return document.getElementById('newWindow').contentWindow.location.href;");
    }
}
