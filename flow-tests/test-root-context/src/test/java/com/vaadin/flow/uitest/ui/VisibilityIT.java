/*
 * Copyright 2000-2017 Vaadin Ltd.
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

public class VisibilityIT extends ChromeBrowserTest {

    @Test
    public void checkVisibility() {
        open();

        Assert.assertFalse(isElementPresent(By.id("visibility")));

        WebElement main = findElement(By.id("main"));
        WebElement div = main.findElement(By.tagName("div"));

        Assert.assertEquals(Boolean.TRUE.toString(),
                div.getAttribute("hidden"));

        WebElement button = findElement(By.id("update"));

        button.click();

        Assert.assertNull(div.getAttribute("hidden"));
        Assert.assertEquals("visibility", div.getAttribute("id"));

        button.click();

        Assert.assertEquals(Boolean.TRUE.toString(),
                div.getAttribute("hidden"));
    }
}
