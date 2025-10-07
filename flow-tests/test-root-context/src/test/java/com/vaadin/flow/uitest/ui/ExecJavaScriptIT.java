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
    public void testComponentArraySerialization() {
        open();

        getButton("componentArrayButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("componentArrayResult")));
        String resultText = result.getText();

        // Should contain component objects for both components
        Assert.assertTrue("Should contain Component Array prefix",
                resultText.startsWith("Component Array: "));
        Assert.assertTrue("Should contain first component object",
                resultText.contains("[0]: component object"));
        Assert.assertTrue("Should contain second component object",
                resultText.contains("[1]: component object"));

        WebElement status = waitUntil(
                d -> findElement(By.id("componentArrayStatus")));
        Assert.assertEquals("Component array serialization completed",
                status.getText());
    }

    @Test
    public void testBeanWithComponentSerialization() {
        open();

        getButton("beanWithComponentButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("beanWithComponentResult")));
        String resultText = result.getText();

        // Should contain bean data and component object
        Assert.assertTrue("Should contain bean name",
                resultText.contains("name=TestBeanComponent"));
        Assert.assertTrue("Should contain bean value",
                resultText.contains("value=123"));
        Assert.assertTrue("Should contain component object",
                resultText.contains("component=object"));

        WebElement status = waitUntil(
                d -> findElement(By.id("beanWithComponentStatus")));
        Assert.assertEquals("Bean with component serialization completed",
                status.getText());
    }

    @Test
    public void testClientCallableBeanParameter() {
        open();

        getButton("clientCallableBeanButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("clientCallableBeanResult")));
        Assert.assertEquals(
                "ClientCallable Bean: name=ClientCallableTest, value=99, active=true",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("clientCallableBeanStatus")));
        Assert.assertEquals("ClientCallable bean handled", status.getText());
    }

    @Test
    public void testClientCallableListParameter() {
        open();

        getButton("clientCallableListButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("clientCallableListResult")));
        Assert.assertEquals(
                "ClientCallable List: [0]: name=Item1, value=111, active=true | [1]: name=Item2, value=222, active=false",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("clientCallableListStatus")));
        Assert.assertEquals("ClientCallable list handled", status.getText());
    }

    @Test
    public void testClientCallableNestedBeanParameter() {
        open();

        getButton("clientCallableNestedButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("clientCallableNestedResult")));
        Assert.assertEquals(
                "ClientCallable Nested: title=ClientCallableNested, simple.name=NestedInner, simple.value=333, simple.active=false",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("clientCallableNestedStatus")));
        Assert.assertEquals("ClientCallable nested bean handled",
                status.getText());
    }

    private WebElement getButton(String id) {
        return findElement(By.id(id));
    }
}
