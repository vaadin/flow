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
package com.vaadin.hummingbird.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

public class JsInTemplateIT extends PhantomJSTest {

    @Test
    public void jsEvaluated() {
        open();
        Assert.assertEquals("Initial", getFirstName());
        Assert.assertEquals("initial123", getUserName());
        Assert.assertEquals("Age: Unknown", getAge());
        WebElement itemCount = getItemCount();
        Assert.assertTrue(hasCssClass(itemCount, "hidden"));

        findElement(By.id("updateModel")).click();

        Assert.assertEquals("Another", getFirstName());
        Assert.assertEquals("another123", getUserName());
        Assert.assertEquals("Age: 42", getAge());
        itemCount = getItemCount();
        Assert.assertFalse(hasCssClass(itemCount, "hidden"));
        Assert.assertEquals("Items available: 3", itemCount.getText());
    }

    private WebElement getItemCount() {
        return findElement(By.id("itemCount"));
    }

    private String getUserName() {
        return findElement(By.id("userName")).getText();
    }

    private String getFirstName() {
        return findElement(By.id("firstName")).getText();
    }

    private String getAge() {
        return findElement(By.id("personAge")).getText();
    }
}
