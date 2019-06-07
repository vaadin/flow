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
import org.junit.Ignore;
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
        resetKeys();
    }

    @Test
    @Ignore("ignored until selenium Actions::sendKeys is fixed for chrome 75 issue #5862")
    public void clickShortcutWorks() {
        sendKeys(Keys.ALT, "b");
        assertActualEquals("button");
    }

    @Test
    @Ignore("ignored until selenium Actions::sendKeys is fixed for chrome 75 issue #5862")
    public void focusShortcutWorks() {
        sendKeys(Keys.ALT, "f") ;

        WebElement input = findElement(By.id("input"));

        assertEquals(input, driver.switchTo().activeElement());
    }

    @Test
    @Ignore("ignored until selenium Actions::sendKeys is fixed for chrome 75 issue #5862")
    public void shortcutsOnlyWorkWhenComponentIsVisible() {
        sendKeys(Keys.ALT, "v");
        assertActualEquals("invisibleP");

        // make the paragraph disappear
        sendKeys(Keys.ALT, "i");
        assertActualEquals("toggled!");

        sendKeys(Keys.ALT, "v");
        assertActualEquals("toggled!"); // did not change

        // make the paragraph appear
        sendKeys(Keys.ALT, "i");
        assertActualEquals("toggled!");

        sendKeys(Keys.ALT, "v");
        assertActualEquals("invisibleP");
    }

    @Test
    @Ignore("ignored until selenium Actions::sendKeys is fixed for chrome 75 issue #5862")
    public void shortcutOnlyWorksWhenComponentIsEnabled() {
        sendKeys(Keys.CONTROL, "U"); // ctrl+shift+u

        // clicking the button disables it, and clicking again should not have
        // and effect
        assertActualEquals("DISABLED CLICKED");

        resetActual();
        assertActualEquals("testing...");

        sendKeys(Keys.CONTROL, "U"); // ctrl+shift+u
        assertActualEquals("testing...");
    }

    @Test
    public void listenOnScopesTheShortcut() {
        sendKeys(Keys.ALT, "s");
        assertActualEquals("testing..."); // nothing happened

        WebElement innerInput = findElement(By.id("focusTarget"));
        innerInput.sendKeys(Keys.ALT, "s");
        assertActualEquals("subview");

        // using the shortcut prevented "s" from being written
        Assert.assertEquals("", innerInput.getText());
    }

    @Test
    @Ignore("ignored until selenium Actions::sendKeys is fixed for chrome 75 issue #5862")
    public void shortcutsOnlyWorkWhenComponentIsAttached() {
        sendKeys(Keys.ALT, "a");
        assertActualEquals("testing..."); // nothing happens

        // attaches the component
        sendKeys(Keys.ALT, "y");
        assertActualEquals("toggled!");

        sendKeys(Keys.ALT, "a");
        assertActualEquals("attachable");

        // detaches the component
        sendKeys(Keys.ALT, "y");
        assertActualEquals("toggled!");

        sendKeys(Keys.ALT, "a");
        assertActualEquals("toggled!"); // nothing happens
    }

    @Test
    @Ignore("ignored until selenium Actions::sendKeys is fixed for chrome 75 issue #5862")
    public void modifyingShortcutShouldChangeShortcutEvent() {
        // the shortcut in this test flips its own modifiers
        sendKeys(Keys.ALT, "g");
        assertActualEquals("Alt");

        sendKeys("G");
        assertActualEquals("Shift");

        // check that things revert back.
        sendKeys(Keys.ALT, "g");
        assertActualEquals("Alt");
    }

    @Test
    public void clickShortcutAllowsKeyDefaults() throws InterruptedException {
        WebElement textField1 = findElement(By.id("click-input-1"));
        WebElement textField2 = findElement(By.id("click-input-2"));

        // ClickButton1: allows browser's default behavior
        textField1.sendKeys("value 1");
        Thread.sleep(100);
        textField1.sendKeys(Keys.ENTER);

        assertActualEquals("click: value 1");

        // ClickButton2: prevents browser's default behavior
        textField2.sendKeys("value 2");
        Thread.sleep(100);
        textField2.sendKeys(Keys.ENTER);

        assertActualEquals("click: ");
    }

    private void assertActualEquals(String expected) {
        Assert.assertEquals(expected,
                findElement(By.id("actual")).getAttribute("value"));
    }

    private void resetActual() {
        WebElement actual = findElement(By.id("actual"));
        WebElement blur = findElement(By.id("blur"));
        actual.clear();
        actual.sendKeys("testing...");
        blur.click();
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
