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
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

public class ConvertToBeanIT extends ChromeBrowserTest {

    @Test
    public void convertToBean_valuesAreUpdated() {
        open();

        WebElement template = findElement(By.id("template"));
        getInShadowRoot(template, By.id("day")).sendKeys("2");
        getInShadowRoot(template, By.id("month")).sendKeys("5");
        getInShadowRoot(template, By.id("year")).sendKeys("2000");

        getInShadowRoot(template, By.id("click")).click();

        String text = getInShadowRoot(template, By.id("msg")).getText();
        Assert.assertEquals("02.05.2000", text);
    }
}
