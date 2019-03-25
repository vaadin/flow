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
    private static final String N1 = "number1";
    private static final String N2 = "number2";
    private static final String SUM = "sum";
    private static final String ERR = "error";

    @Override
    protected String getTestPath() {
        return "/fireEvent.html";
    }

    @Test
    public void customEventsGetSentToTheClientSide() {
        open();

        waitForElementVisible(By.id("calc"));

        WebElement button = findElement(By.id("button"));
        WebElement number1 = findElement(By.id(N1));
        WebElement number2 = findElement(By.id(N2));

        Assert.assertEquals("Sum should be 0", "0", value(SUM));
        Assert.assertEquals("Error should be empty", "", value(ERR));

        number1.sendKeys("4.5");
        number2.sendKeys("3.5");

        Assert.assertEquals("4.5", number1.getAttribute("value"));
        Assert.assertEquals("3.5", number2.getAttribute("value"));

        button.click();

        Assert.assertEquals("Sum should be 8", "8", value(SUM));
        Assert.assertEquals("Error should be empty", "",
                value(ERR));

        number1.clear();
        number2.clear();

        Assert.assertEquals("", value(N1));
        Assert.assertEquals("", value(N2));

        button.click();

        Assert.assertEquals("Sum should not have changed", "8", value(SUM));
        Assert.assertEquals("Error should have been raised", "empty String",
                value(ERR));
    }

    private String value(String id) {
        return findElement(By.id(id)).getText();
    }
}
