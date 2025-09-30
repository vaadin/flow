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
    public void testBeanSerializationSimpleTypes() {
        open();

        // Test simple bean with only primitive types
        getButton("simpleBeanButton").click();

        // Wait for the result div to appear
        WebElement result = waitUntil(
                d -> findElement(By.id("simpleBeanResult")));
        Assert.assertEquals("name=TestBean, value=42, active=true",
                result.getText());

        // Verify status message
        WebElement status = waitUntil(
                d -> findElement(By.id("simpleBeanStatus")));
        Assert.assertEquals("Simple bean sent and received", status.getText());
    }

    @Test
    public void testBeanSerializationNestedBeans() {
        open();

        // Test nested beans
        getButton("nestedBeanButton").click();

        // Wait for the result div to appear
        WebElement result = waitUntil(
                d -> findElement(By.id("nestedBeanResult")));
        Assert.assertEquals("title=Outer, simple.name=Inner, simple.value=100",
                result.getText());

        // Verify status message
        WebElement status = waitUntil(
                d -> findElement(By.id("nestedBeanStatus")));
        Assert.assertEquals("Nested bean sent and received", status.getText());
    }

    @Test
    public void testListSerializationPrimitives() {
        open();

        // Test list of primitives
        getButton("listPrimitivesButton").click();

        // Wait for the result div to appear
        WebElement result = waitUntil(
                d -> findElement(By.id("listPrimitivesResult")));
        Assert.assertEquals("strings=first,second,third ints=1,2,3,4,5",
                result.getText());

        // Verify status message
        WebElement status = waitUntil(
                d -> findElement(By.id("listPrimitivesStatus")));
        Assert.assertEquals("Primitive lists sent and received",
                status.getText());
    }

    @Test
    public void testListSerializationBeans() {
        open();

        // Test list of beans
        getButton("listBeansButton").click();

        // Wait for the result div to appear
        WebElement result = waitUntil(
                d -> findElement(By.id("listBeansResult")));
        Assert.assertEquals("count=3 [0]=Bean1:10 [1]=Bean2:20 [2]=Bean3:30",
                result.getText());

        // Verify status message
        WebElement status = waitUntil(
                d -> findElement(By.id("listBeansStatus")));
        Assert.assertEquals("Bean list sent and received", status.getText());
    }

    @Test
    public void testListSerializationComponents() {
        open();

        // Test list of components
        getButton("listComponentsButton").click();

        // Wait for the result div to appear
        WebElement result = waitUntil(
                d -> findElement(By.id("listComponentsResult")));
        Assert.assertEquals(
                "count=3 [0]=vaadin-button:Button 1 [1]=vaadin-button:Button 2 [2]=div:Div 1",
                result.getText());

        // Verify status message
        WebElement status = waitUntil(
                d -> findElement(By.id("listComponentsStatus")));
        Assert.assertEquals("Component list sent and received",
                status.getText());
    }

    @Test
    public void testListSerializationMixed() {
        open();

        // Test mixed list with primitives, beans, and components
        getButton("listMixedButton").click();

        // Wait for the result div to appear
        WebElement result = waitUntil(
                d -> findElement(By.id("listMixedResult")));
        Assert.assertEquals(
                "count=5 [0]=string:string value [1]=number:42 [2]=bean:MixedBean [3]=element:vaadin-button [4]=null",
                result.getText());

        // Verify status message
        WebElement status = waitUntil(
                d -> findElement(By.id("listMixedStatus")));
        Assert.assertEquals("Mixed list sent and received", status.getText());
    }

    @Test
    public void testBeanSerializationWithComponents() {
        open();

        // Test bean with component references
        getButton("componentBeanButton").click();

        // Wait for the components to be added
        waitUntil(d -> findElement(By.id("beanButton")));
        waitUntil(d -> findElement(By.id("beanDiv")));

        // Wait for the result div to appear
        WebElement result = waitUntil(
                d -> findElement(By.id("componentBeanResult")));
        String text = result.getText();

        // Verify the bean fields were received
        Assert.assertTrue("Should contain label",
                text.contains("label=Main bean"));

        // Verify the button component was resolved correctly
        Assert.assertTrue("Should contain button tag",
                text.contains("button.tag=button"));
        Assert.assertTrue("Should contain button text",
                text.contains("button.text=Bean Button"));

        // Verify the nested bean fields
        Assert.assertTrue("Should contain nested description",
                text.contains("nested.desc=Nested with component"));

        // Verify the nested div component was resolved correctly
        Assert.assertTrue("Should contain div tag",
                text.contains("nested.div.tag=div"));
        Assert.assertTrue("Should contain div text",
                text.contains("nested.div.text=Bean Div"));

        // Verify status message
        WebElement status = waitUntil(
                d -> findElement(By.id("componentBeanStatus")));
        Assert.assertEquals("Component bean sent and received",
                status.getText());
    }

    @Test
    public void testMapPrimitives() {
        open();
        WebElement button = findElement(By.id("mapPrimitivesButton"));
        button.click();

        waitUntil(driver -> findElement(By.id("mapPrimitivesStatus")) != null);

        WebElement result = findElement(By.id("mapPrimitivesResult"));
        String text = result.getText();
        // Keys should be sorted: one, three, two
        Assert.assertEquals("Map primitives should match", "one=1,three=3,two=2",
                text);

        WebElement status = findElement(By.id("mapPrimitivesStatus"));
        Assert.assertEquals("Primitive map sent and received", status.getText());
    }

    @Test
    public void testMapComponents() {
        open();
        WebElement button = findElement(By.id("mapComponentsButton"));
        button.click();

        waitUntil(driver -> findElement(By.id("mapComponentsStatus")) != null);

        WebElement result = findElement(By.id("mapComponentsResult"));
        String text = result.getText();
        // Should contain sorted keys with component tags
        Assert.assertTrue("Should contain button1",
                text.contains("button1=vaadin-button"));
        Assert.assertTrue("Should contain button2",
                text.contains("button2=vaadin-button"));
        Assert.assertTrue("Should contain div", text.contains("div=div"));

        WebElement status = findElement(By.id("mapComponentsStatus"));
        Assert.assertEquals("Component map sent and received", status.getText());
    }

    @Test
    public void testMapMixedTypes() {
        open();
        WebElement button = findElement(By.id("mapMixedButton"));
        button.click();

        waitUntil(driver -> findElement(By.id("mapMixedStatus")) != null);

        WebElement result = findElement(By.id("mapMixedResult"));
        String text = result.getText();
        // Check all different value types in the map
        Assert.assertTrue("Should contain component",
                text.contains("component=element:vaadin-button"));
        Assert.assertTrue("Should contain list", text.contains("list=array:3"));
        Assert.assertTrue("Should contain null value",
                text.contains("nullValue=null"));
        Assert.assertTrue("Should contain number", text.contains("number=number:42"));
        Assert.assertTrue("Should contain string",
                text.contains("string=string:hello"));

        WebElement status = findElement(By.id("mapMixedStatus"));
        Assert.assertEquals("Mixed map sent and received", status.getText());
    }

    private WebElement getButton(String id) {
        return findElement(By.id(id));
    }
}
