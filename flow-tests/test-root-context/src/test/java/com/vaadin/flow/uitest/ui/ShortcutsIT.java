/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ShortcutsIT extends ChromeBrowserTest {

    @Test
    public void clickShortcutWorks() {
        open();

        sendKeys(Keys.ALT, "b");
        Assert.assertEquals("button", getValue());
    }

    @Test
    public void focusShortcutWorks() {
        open();

        sendKeys(Keys.ALT, "f") ;

        WebElement input = findElement(By.id("input"));

        assertEquals(input, driver.switchTo().activeElement());
    }

    @Test
    public void shortcutsOnlyWorkWhenComponentIsVisible() {
        open();

        sendKeys(Keys.ALT, "v");
        Assert.assertEquals("invisibleP", getValue());

        // make the paragraph disappear
        sendKeys(Keys.ALT, "i");
        Assert.assertEquals("toggled!", getValue());

        sendKeys(Keys.ALT, "v");
        Assert.assertEquals("toggled!", getValue()); // did not change

        // make the paragraph appear
        sendKeys(Keys.ALT, "i");
        Assert.assertEquals("toggled!", getValue());

        sendKeys(Keys.ALT, "v");
        Assert.assertEquals("invisibleP", getValue());
    }

    @Test
    public void listenOnScopesTheShortcut() {
        open();

        sendKeys(Keys.ALT, "s");
        Assert.assertEquals("testing...", getValue()); // nothing happened

        WebElement innerInput = findElement(By.id("focusTarget"));
        innerInput.sendKeys(Keys.ALT, "s");
        Assert.assertEquals("subview", getValue());

        // using the shortcut prevented "s" from being written
        Assert.assertEquals("", innerInput.getText());
    }

    @Test
    public void shortcutsOnlyWorkWhenComponentIsAttached() {
        open();

        sendKeys(Keys.ALT, "a");
        Assert.assertEquals("testing...", getValue()); // nothing happens

        // attaches the component
        sendKeys(Keys.ALT, "y");
        Assert.assertEquals("toggled!", getValue());

        sendKeys(Keys.ALT, "a");
        Assert.assertEquals("attachable", getValue());

        // detaches the component
        sendKeys(Keys.ALT, "y");
        Assert.assertEquals("toggled!", getValue());

        sendKeys(Keys.ALT, "a");
        Assert.assertEquals("toggled!", getValue()); // nothing happens
    }

    private String getValue() {
        WebElement expected = findElement(By.id("expected"));
        return expected.getText();
    }

    private void sendKeys(CharSequence... keys) {
        new Actions(driver).sendKeys(keys).build().perform();
        // if keys are not reset, alt will remain down and start flip-flopping
        resetKeys();
    }

    private void resetKeys() {
        new Actions(driver).sendKeys(Keys.NULL).build().perform();
    }

    private void _wait(long millis) {
        driver.manage().timeouts().implicitlyWait(millis, TimeUnit.MILLISECONDS);
    }
}