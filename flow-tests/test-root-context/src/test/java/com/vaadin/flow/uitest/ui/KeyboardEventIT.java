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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class KeyboardEventIT extends ChromeBrowserTest {
    @Test
    public void verify_that_keys_are_received_correctly() {
        open();

        // make sure both elements are present
        Assert.assertTrue(isElementPresent(By.id("input")));
        Assert.assertTrue(isElementPresent(By.id("paragraph")));

        WebElement input = findElement(By.id("input"));
        WebElement paragraph = findElement(By.id("paragraph"));

        input.sendKeys("q");

        Assert.assertEquals("q:KeyQ", paragraph.getText());

        input.sendKeys("%");

        Assert.assertEquals("%:Digit5", paragraph.getText());
        // next tests rely on
        // https://github.com/SeleniumHQ/selenium/blob/master/javascript/node/selenium-webdriver/lib/input.js#L52

        // arrow right
        input.sendKeys("\uE014");

        Assert.assertEquals("ArrowRight:ArrowRight", paragraph.getText());

        // physical * key
        input.sendKeys("\uE024");

        Assert.assertEquals("*:NumpadMultiply", paragraph.getText());
    }

    @Test // #5989
    public void verify_that_invalid_keyup_event_is_ignored() {
        open();

        WebElement input = findElement(By.id("input"));
        WebElement sendInvalidKeyUp = findElement(By.id("sendInvalidKeyUp"));
        WebElement paragraph = findElement(By.id("keyUpParagraph"));

        input.sendKeys("q");
        Assert.assertEquals("q", paragraph.getText());

        sendInvalidKeyUp.click();

        Assert.assertEquals("q", paragraph.getText());
    }
}
