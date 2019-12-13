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
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class LazyInputIT extends ChromeBrowserTest {

    @Test
    public void typeText_textIsPreserved() throws InterruptedException {
        open();

        WebElement input = findElement(By.id("text-input"));

        input.sendKeys("foo");
        waitUntil(driver -> getValue(input) != null);
        Assert.assertEquals("foo", getValue(input));

        StringBuilder result = new StringBuilder("foo");
        for (char ch = 'a'; ch < 'd'; ch++) {
            input.sendKeys(String.valueOf(ch));
            result.append(ch);
        }

        waitUntil(driver -> !getValue(input).equals("foo"));

        Assert.assertEquals(result.toString(), getValue(input));
        int previousLength = result.toString().length();

        for (char ch = 'e'; ch < 'k'; ch++) {
            input.sendKeys(String.valueOf(ch));
            result.append(ch);
            Thread.sleep(100);
        }

        waitUntil(driver -> getValue(input).length() != previousLength);
        Assert.assertEquals(result.toString(), getValue(input));
    }

    private String getValue(WebElement element) {
        return (String) executeScript("return arguments[0].value", element);
    }
}
