package com.vaadin.hummingbird.uitest.ui;
/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

public class BasicTemplateIT extends PhantomJSTest {
    @Test
    public void testBasicTemplate() {
        open();

        WebElement bar = findElement(By.className("bar"));

        Assert.assertEquals("baz", bar.getText());
        Assert.assertTrue(isElementPresent(By.cssSelector(".bar input")));

        WebElement containerButton = findElement(
                By.cssSelector("#container > button"));

        // Click button to remove it
        containerButton.click();
        Assert.assertFalse(
                isElementPresent(By.cssSelector("#container > button")));
    }
}
