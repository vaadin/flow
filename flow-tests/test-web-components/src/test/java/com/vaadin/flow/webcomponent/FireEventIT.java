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
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class FireEventIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/updateProperty.html";
    }

    @Test
    public void propertiesWrittenOnServerSideAreUpdatedToWebComponent() {
        open();

        waitForElementVisible(By.id("calc"));

        WebElement component = findElement(By.id("calc"));
        WebElement button = component.findElement(By.tagName("button"));
        WebElement number1 = component.findElement(By.id("number1"));
        WebElement number2 = component.findElement(By.id("number2"));

        Assert.assertEquals("Sum should be 0", 0.0,
                getSum(), 0.0);
        Assert.assertEquals("Error should be empty", "",
                getError());

        number1.sendKeys("4.5");
        number2.sendKeys("3.5");
        button.click();

        Assert.assertEquals("Sum should be 8", 8.0,
                getSum(), 0.0);
        Assert.assertEquals("Error should be empty", "",
                getError());

        number1.clear();
        number2.clear();
        button.click();

        Assert.assertEquals("Sum should not have changed", 8.0,
                getSum(), 0.0);
        Assert.assertEquals("Error should have been raised", "error here",
                getError());
    }

    private double getSum() {
        WebElement count = findElement(By.id("sum"));
        return Integer.parseInt(count.getText());
    }

    private String getError() {
        WebElement count = findElement(By.id("error"));
        return count.getText();
    }
}
