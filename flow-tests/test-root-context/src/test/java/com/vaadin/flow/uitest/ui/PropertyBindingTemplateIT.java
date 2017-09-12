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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * @author Vaadin Ltd
 *
 */
public class PropertyBindingTemplateIT extends ChromeBrowserTest {

    @Before
    public void setUp() {
        open();
    }

    @Test
    public void initialValue() {
        WebElement input = findElement(By.cssSelector("input"));

        Assert.assertEquals("Foo", input.getAttribute("value"));
        Assert.assertTrue(
                Boolean.parseBoolean(input.getAttribute("booleanprop")));
        Assert.assertEquals("1.1", input.getAttribute("doubleprop"));
    }

    @Test
    public void updateValue() {
        WebElement button = findElement(By.cssSelector("button"));
        button.click();

        WebElement input = findElement(By.cssSelector("input"));

        Assert.assertEquals("Bar", input.getAttribute("value"));
        Assert.assertFalse(
                Boolean.parseBoolean(input.getAttribute("booleanprop")));
        Assert.assertEquals("2.2", input.getAttribute("doubleprop"));
    }
}
