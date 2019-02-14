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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ShortcutsIT extends ChromeBrowserTest {

    @Before
    public void before() {
        open();
    }

    @Test
    public void clickShortcutWorks() {
        sendKeys(Keys.ALT, "b");
        Assert.assertEquals("button", getActual());
    }

    @Test
    public void focusShortcutWorks() {
        sendKeys(Keys.ALT, "f") ;

        WebElement input = findElement(By.id("input"));

        assertEquals(input, driver.switchTo().activeElement());
    }

    @Test
    public void shortcutsOnlyWorkWhenComponentIsVisible() {
        sendKeys(Keys.ALT, "v");
        Assert.assertEquals("invisibleP", getActual());

        // make the paragraph disappear
        sendKeys(Keys.ALT, "i");
        Assert.assertEquals("toggled!", getActual());

        sendKeys(Keys.ALT, "v");
        Assert.assertEquals("toggled!", getActual()); // did not change

        // make the paragraph appear
        sendKeys(Keys.ALT, "i");
        Assert.assertEquals("toggled!", getActual());

        sendKeys(Keys.ALT, "v");
        Assert.assertEquals("invisibleP", getActual());
    }

    @Test
    public void listenOnScopesTheShortcut() {
        sendKeys(Keys.ALT, "s");
        Assert.assertEquals("testing...", getActual()); // nothing happened

        WebElement innerInput = findElement(By.id("focusTarget"));
        innerInput.sendKeys(Keys.ALT, "s");
        Assert.assertEquals("subview", getActual());

        // using the shortcut prevented "s" from being written
        Assert.assertEquals("", innerInput.getText());
    }

    @Test
    public void shortcutsOnlyWorkWhenComponentIsAttached() {
        sendKeys(Keys.ALT, "a");
        Assert.assertEquals("testing...", getActual()); // nothing happens

        // attaches the component
        sendKeys(Keys.ALT, "y");
        Assert.assertEquals("toggled!", getActual());

        sendKeys(Keys.ALT, "a");
        Assert.assertEquals("attachable", getActual());

        // detaches the component
        sendKeys(Keys.ALT, "y");
        Assert.assertEquals("toggled!", getActual());

        sendKeys(Keys.ALT, "a");
        Assert.assertEquals("toggled!", getActual()); // nothing happens
    }

    @Test
    public void modifyingShortcutShouldChangeShortcutEvent() {
        // the shortcut in this test flips its own modifiers
        sendKeys(Keys.ALT, "g");
        Assert.assertEquals("Alt", getActual());

        sendKeys("G");
        Assert.assertEquals("Shift", getActual());

        // check that things revert back.
        sendKeys(Keys.ALT, "g");
        Assert.assertEquals("Alt", getActual());
    }

    @Test
    public void clickShortcutAllowsKeyDefaults() {
        WebElement textField1 = findElement(By.id("click-input-1"));
        WebElement textField2 = findElement(By.id("click-input-2"));

        // ClickButton1: has default values
        textField1.sendKeys("value 1", Keys.ENTER);

        Assert.assertEquals("click: value 1", getActual());

        // ClickButton2: prevents key default behavior
        textField2.sendKeys("value 2", Keys.ENTER);

        Assert.assertEquals("click: ", getActual());
    }

    private String getActual() {
        WebElement actual = findElement(By.id("actual"));
        return actual.getAttribute("value");
    }

    private void sendKeys(CharSequence... keys) {
        new Actions(driver).sendKeys(keys).build().perform();
        // if keys are not reset, alt will remain down and start flip-flopping
        resetKeys();
    }

    private void resetKeys() {
        new Actions(driver).sendKeys(Keys.NULL).build().perform();
    }
}
