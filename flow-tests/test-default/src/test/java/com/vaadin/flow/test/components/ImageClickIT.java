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
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(ImageClickView.class)
public class ImageClickIT extends AbstractDefaultIT {
    @BrowserTest
    public void testClickOnImage() {
        open();

        WebElement message = findElement(By.id("message"));
        WebElement message2 = findElement(By.id("message2"));
        WebElement message3 = findElement(By.id("message3"));
        WebElement image = findElement(By.id("image"));

        Assertions.assertEquals("Before click", message.getText());

        image.click();

        Assertions.assertEquals("After click 1", message.getText());
        Assertions.assertEquals("Single click 1", message2.getText());
        Assertions.assertEquals("", message3.getText());
    }

    @BrowserTest
    public void testDoubleClickOnImage() {
        open();

        Actions act = new Actions(getDriver());

        WebElement message = findElement(By.id("message"));
        WebElement message2 = findElement(By.id("message2"));
        WebElement message3 = findElement(By.id("message3"));
        WebElement image = findElement(By.id("image"));

        Assertions.assertEquals("Before click", message.getText());

        act.doubleClick(image).perform();

        Assertions.assertEquals("After click 2", message.getText());
        Assertions.assertEquals("Single click 1", message2.getText());
        Assertions.assertEquals("Double click 1", message3.getText());
    }

}
