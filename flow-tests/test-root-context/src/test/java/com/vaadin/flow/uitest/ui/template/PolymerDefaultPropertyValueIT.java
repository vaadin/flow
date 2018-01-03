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
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

public class PolymerDefaultPropertyValueIT extends ChromeBrowserTest {

    @Test
    public void initialModelValues_polymerHasDefaultValues() {
        open();

        WebElement template = findElement(By.id("template"));
        WebElement text = getInShadowRoot(template, By.id("text"));

        Assert.assertEquals("foo", text.getText());

        WebElement name = getInShadowRoot(template, By.id("name"));
        Assert.assertEquals("bar", name.getText());

        WebElement msg = getInShadowRoot(template, By.id("message"));
        Assert.assertEquals("updated-message", msg.getText());

        WebElement email = getInShadowRoot(template, By.id("email"));
        Assert.assertEquals("foo@example.com", email.getText());

        findElement(By.id("show-email")).click();

        WebElement serverSideEmailValue = findElement(By.id("email-value"));
        Assert.assertEquals("foo@example.com", serverSideEmailValue.getText());
    }
}
