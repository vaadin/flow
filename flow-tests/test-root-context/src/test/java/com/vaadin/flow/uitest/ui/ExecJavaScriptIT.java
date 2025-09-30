/*
 * Copyright 2000-2025 Vaadin Ltd.
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

public class ExecJavaScriptIT extends ChromeBrowserTest {
    @Test
    public void testExecuteJavaScript() {
        open();

        String alertText = getButton("alertButton").getText();
        String focusText = getButton("focusButton").getText();

        getButton("swapButton").click();

        Assert.assertEquals(focusText, getButton("alertButton").getText());
        Assert.assertEquals(alertText, getButton("focusButton").getText());

        getButton("createButton").click();

        WebElement findElement = findElement(By.className("newInput"));
        Assert.assertEquals("Value from js", findElement.getAttribute("value"));
    }

    @Test
    public void testElementExecuteJavaScriptWithAwait() {
        open();
        getButton("elementAwaitButton").click();
        WebElement result = waitUntil(
                d -> findElement(By.id("elementAwaitResult")));
        Assert.assertEquals("Element execute JS await result: 42",
                result.getText());
    }

    @Test
    public void testPageExecuteJavaScriptWithAwait() {
        open();
        getButton("pageAwaitButton").click();
        WebElement result = waitUntil(
                d -> findElement(By.id("pageAwaitResult")));
        Assert.assertEquals("Page execute JS await result: 72",
                result.getText());
    }

    @Test
    public void testBeanSerialization() {
        open();

        getButton("beanButton").click();

        WebElement result = waitUntil(d -> findElement(By.id("beanResult")));
        Assert.assertEquals(
                "simple: name=TestBean, value=42, active=true | nested: title=Outer, inner.name=Inner, inner.value=100",
                result.getText());

        WebElement status = waitUntil(d -> findElement(By.id("beanStatus")));
        Assert.assertEquals("Bean serialization completed", status.getText());
    }

    @Test
    public void testBeanReturnValue() {
        open();

        getButton("returnBeanButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("returnBeanResult")));
        Assert.assertEquals(
                "Returned: title=ReturnedNested, simple.name=InnerReturned, simple.value=777, simple.active=true",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("returnBeanStatus")));
        Assert.assertEquals("Bean returned", status.getText());
    }

    @Test
    public void testListSerialization() {
        open();

        getButton("listButton").click();

        WebElement result = waitUntil(d -> findElement(By.id("listResult")));
        Assert.assertEquals(
                "List: [0]: name=FirstItem, value=10, active=true | [1]: name=SecondItem, value=20, active=false | [2]: name=ThirdItem, value=30, active=true",
                result.getText());

        WebElement status = waitUntil(d -> findElement(By.id("listStatus")));
        Assert.assertEquals("List serialization completed", status.getText());
    }

    @Test
    public void testListReturnValue() {
        open();

        getButton("returnListButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("returnListResult")));
        Assert.assertEquals("Returned list with 2 items", result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("returnListStatus")));
        Assert.assertEquals("List returned", status.getText());
    }

    @Test
    public void testMapSerialization() {
        open();

        getButton("mapButton").click();

        WebElement result = waitUntil(d -> findElement(By.id("mapResult")));
        Assert.assertEquals(
                "Map: firstKey={name=FirstBean, value=100, active=true} | secondKey={name=SecondBean, value=200, active=false} | thirdKey={name=ThirdBean, value=300, active=true}",
                result.getText());

        WebElement status = waitUntil(d -> findElement(By.id("mapStatus")));
        Assert.assertEquals("Map serialization completed", status.getText());
    }

    @Test
    public void testMapReturnValue() {
        open();

        getButton("returnMapButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("returnMapResult")));
        Assert.assertEquals("Returned map with 2 keys", result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("returnMapStatus")));
        Assert.assertEquals("Map returned", status.getText());
    }

    private WebElement getButton(String id) {
        return findElement(By.id(id));
    }
}
