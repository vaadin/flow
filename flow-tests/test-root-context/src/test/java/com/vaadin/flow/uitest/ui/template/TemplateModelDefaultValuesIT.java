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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.PhantomJSTest;

public class TemplateModelDefaultValuesIT extends PhantomJSTest {

    @Test
    public void modelRootDefaultValues() {
        open();
        WebElement container = findElement(By.id("root"));
        List<WebElement> divs = container.findElements(By.tagName("div"));

        int i = 0;
        Assert.assertEquals("booleanValue: false", divs.get(i++).getText());
        Assert.assertEquals("booleanObject:", divs.get(i++).getText());
        Assert.assertEquals("intValue: 0", divs.get(i++).getText());
        Assert.assertEquals("intObject:", divs.get(i++).getText());
        Assert.assertEquals("doubleValue: 0", divs.get(i++).getText());
        Assert.assertEquals("doubleObject:", divs.get(i++).getText());
        Assert.assertEquals("string:", divs.get(i++).getText());
        Assert.assertEquals("person.age: <undefined>", divs.get(i++).getText());
        Assert.assertEquals("definedPerson.age: 0", divs.get(i++).getText());
    }

    @Test
    public void modelBeanDefaultValues() {
        open();
        WebElement container = findElement(By.id("bean"));
        List<WebElement> divs = container.findElements(By.tagName("div"));

        int i = 0;
        Assert.assertEquals("booleanValue: false", divs.get(i++).getText());
        Assert.assertEquals("booleanObject:", divs.get(i++).getText());
        Assert.assertEquals("intValue: 0", divs.get(i++).getText());
        Assert.assertEquals("intObject:", divs.get(i++).getText());
        Assert.assertEquals("doubleValue: 0", divs.get(i++).getText());
        Assert.assertEquals("doubleObject:", divs.get(i++).getText());
        Assert.assertEquals("string:", divs.get(i++).getText());
        Assert.assertEquals("person.age: <undefined>", divs.get(i++).getText());
        Assert.assertEquals("definedPerson.age: 0", divs.get(i++).getText());
    }
}
