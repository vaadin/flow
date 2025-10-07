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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.WebElement;

public class ImageClickIT extends ChromeBrowserTest {
    @Test
    public void testClickOnImage() {
        open();

        WebElement message = findElement(By.id("message"));
        WebElement message2 = findElement(By.id("message2"));
        WebElement message3 = findElement(By.id("message3"));
        WebElement image = findElement(By.id("image"));

        Assert.assertEquals("Before click", message.getText());

        image.click();

        Assert.assertEquals("After click 1", message.getText());
        Assert.assertEquals("Single click 1", message2.getText());
        Assert.assertEquals("", message3.getText());
    }

    @Test
    public void testDoubleClickOnImage() {
        open();

        Actions act = new Actions(getDriver());

        WebElement message = findElement(By.id("message"));
        WebElement message2 = findElement(By.id("message2"));
        WebElement message3 = findElement(By.id("message3"));
        WebElement image = findElement(By.id("image"));

        Assert.assertEquals("Before click", message.getText());

        act.doubleClick(image).perform();

        Assert.assertEquals("After click 2", message.getText());
        Assert.assertEquals("Single click 1", message2.getText());
        Assert.assertEquals("Double click 1", message3.getText());
    }

}
