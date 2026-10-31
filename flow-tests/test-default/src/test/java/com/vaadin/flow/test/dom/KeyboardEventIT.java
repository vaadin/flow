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

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(KeyboardEventView.class)
public class KeyboardEventIT extends AbstractDefaultIT {
    @BrowserTest
    public void verify_that_keys_are_received_correctly() {
        open();

        // make sure both elements are present
        assertTrue(isElementPresent(By.id("input")));
        assertTrue(isElementPresent(By.id("paragraph")));

        WebElement input = findElement(By.id("input"));
        WebElement paragraph = findElement(By.id("paragraph"));

        input.sendKeys("q");

        Assertions.assertEquals("q:KeyQ", paragraph.getText());

        input.sendKeys("%");

        Assertions.assertEquals("%:Digit5", paragraph.getText());
        // next tests rely on
        // https://github.com/SeleniumHQ/selenium/blob/master/javascript/node/selenium-webdriver/lib/input.js#L52

        // arrow right
        input.sendKeys("\uE014");

        Assertions.assertEquals("ArrowRight:ArrowRight", paragraph.getText());

        // physical * key
        input.sendKeys("\uE024");

        Assertions.assertEquals("*:NumpadMultiply", paragraph.getText());
    }

    @BrowserTest // #5989
    public void verify_that_invalid_keyup_event_is_ignored() {
        open();

        WebElement input = findElement(By.id("input"));
        WebElement sendInvalidKeyUp = findElement(By.id("sendInvalidKeyUp"));
        WebElement paragraph = findElement(By.id("keyUpParagraph"));

        input.sendKeys("q");
        Assertions.assertEquals("q", paragraph.getText());

        sendInvalidKeyUp.click();

        Assertions.assertEquals("q", paragraph.getText());
    }
}
