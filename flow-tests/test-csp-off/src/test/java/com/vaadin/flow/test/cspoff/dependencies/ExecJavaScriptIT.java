/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.cspoff.dependencies;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.cspoff.AbstractCspOffIT;
import com.vaadin.flow.test.cspoff.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(ExecJavaScriptView.class)
public class ExecJavaScriptIT extends AbstractCspOffIT {
    @BrowserTest
    public void testExecuteJavaScript() {
        open();

        String alertText = getButton("alertButton").getText();
        String focusText = getButton("focusButton").getText();

        getButton("swapButton").click();

        Assertions.assertEquals(focusText, getButton("alertButton").getText());
        Assertions.assertEquals(alertText, getButton("focusButton").getText());

        getButton("createButton").click();

        WebElement findElement = findElement(By.className("newInput"));
        Assertions.assertEquals("Value from js",
                findElement.getAttribute("value"));
    }

    @BrowserTest
    public void testElementExecuteJavaScriptWithAwait() {
        open();
        getButton("elementAwaitButton").click();
        WebElement result = waitUntil(
                d -> findElement(By.id("elementAwaitResult")));
        Assertions.assertEquals("Element execute JS await result: 42",
                result.getText());
    }

    @BrowserTest
    public void testPageExecuteJavaScriptWithAwait() {
        open();
        getButton("pageAwaitButton").click();
        WebElement result = waitUntil(
                d -> findElement(By.id("pageAwaitResult")));
        Assertions.assertEquals("Page execute JS await result: 72",
                result.getText());
    }

    @BrowserTest
    public void testVariadicJsDefinition() {
        open();

        getButton("variadicButton").click();

        // Each argument reaches the browser on its own, and the generated
        // function collects the ones that follow the fixed parameter
        WebElement result = waitUntil(
                d -> findElement(By.id("variadicResult")));
        Assertions.assertEquals("Variadic call: a-1-true-b", result.getText());
    }

    @BrowserTest
    public void testVariadicJsDefinitionWithoutTrailingArguments() {
        open();

        getButton("variadicNoArgumentsButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("variadicResult")));
        Assertions.assertEquals("Variadic call: a", result.getText());
    }

    @BrowserTest
    public void testCallFunctionWithArguments() {
        open();

        getButton("callFunctionButton").click();

        // The name of the function and the arguments are sent, and the
        // browser looks the function up on the element
        WebElement result = waitUntil(d -> findElement(By.id("callResult")));
        Assertions.assertEquals("Function call: a-1-true", result.getText());
    }

    @BrowserTest
    public void testCallFunctionThroughProperty() {
        open();

        getButton("callOnPropertyButton").click();

        // The property the function is read from is its this, so the label it
        // reads is the one of the connector rather than of the element
        WebElement result = waitUntil(d -> findElement(By.id("callResult")));
        Assertions.assertEquals("Function call: connector!", result.getText());
    }

    @BrowserTest
    public void testCallFunctionThatIsNotThere() {
        open();

        getButton("callMissingButton").click();

        WebElement result = waitUntil(d -> findElement(By.id("callResult")));
        Assertions.assertEquals("Function call: failed", result.getText());
    }

    @BrowserTest
    public void testBeanSerialization() {
        open();

        getButton("beanButton").click();

        WebElement result = waitUntil(d -> findElement(By.id("beanResult")));
        Assertions.assertEquals(
                "simple: name=TestBean, value=42, active=true | nested: title=Outer, inner.name=Inner, inner.value=100",
                result.getText());

        WebElement status = waitUntil(d -> findElement(By.id("beanStatus")));
        Assertions.assertEquals("Bean serialization completed",
                status.getText());
    }

    @BrowserTest
    public void testBeanReturnValue() {
        open();

        getButton("returnBeanButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("returnBeanResult")));
        Assertions.assertEquals(
                "Returned: title=ReturnedNested, simple.name=InnerReturned, simple.value=777, simple.active=true",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("returnBeanStatus")));
        Assertions.assertEquals("Bean returned", status.getText());
    }

    @BrowserTest
    public void testListSerialization() {
        open();

        getButton("listButton").click();

        WebElement result = waitUntil(d -> findElement(By.id("listResult")));
        Assertions.assertEquals(
                "List: [0]: name=FirstItem, value=10, active=true | [1]: name=SecondItem, value=20, active=false | [2]: name=ThirdItem, value=30, active=true",
                result.getText());

        WebElement status = waitUntil(d -> findElement(By.id("listStatus")));
        Assertions.assertEquals("List serialization completed",
                status.getText());
    }

    @BrowserTest
    public void testListReturnValue() {
        open();

        getButton("returnListButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("returnListResult")));
        Assertions.assertEquals("Returned list with 2 items", result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("returnListStatus")));
        Assertions.assertEquals("List returned", status.getText());
    }

    @BrowserTest
    public void testMapSerialization() {
        open();

        getButton("mapButton").click();

        WebElement result = waitUntil(d -> findElement(By.id("mapResult")));
        String resultText = result.getText();

        // Verify Map prefix and all entries are present
        assertTrue(resultText.startsWith("Map: "), "Should start with 'Map: '");
        assertTrue(
                resultText.contains(
                        "first: name=FirstKey, value=100, active=true"),
                "Should contain 'first' key");
        assertTrue(
                resultText.contains(
                        "second: name=SecondKey, value=200, active=false"),
                "Should contain 'second' key");
        assertTrue(
                resultText.contains(
                        "third: name=ThirdKey, value=300, active=true"),
                "Should contain 'third' key");

        WebElement status = waitUntil(d -> findElement(By.id("mapStatus")));
        Assertions.assertEquals("Map serialization completed",
                status.getText());
    }

    @BrowserTest
    public void testMapReturnValue() {
        open();

        getButton("returnMapButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("returnMapResult")));
        Assertions.assertEquals("Returned map with 2 entries",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("returnMapStatus")));
        Assertions.assertEquals("Map returned", status.getText());
    }

    @BrowserTest
    public void testComponentArraySerialization() {
        open();

        getButton("componentArrayButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("componentArrayResult")));
        String resultText = result.getText();

        // Should contain component objects for both components
        assertTrue(resultText.startsWith("Component Array: "),
                "Should contain Component Array prefix");
        assertTrue(resultText.contains("[0]: component object"),
                "Should contain first component object");
        assertTrue(resultText.contains("[1]: component object"),
                "Should contain second component object");

        WebElement status = waitUntil(
                d -> findElement(By.id("componentArrayStatus")));
        Assertions.assertEquals("Component array serialization completed",
                status.getText());
    }

    @BrowserTest
    public void testBeanWithComponentSerialization() {
        open();

        getButton("beanWithComponentButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("beanWithComponentResult")));
        String resultText = result.getText();

        // Should contain bean data and component object
        assertTrue(resultText.contains("name=TestBeanComponent"),
                "Should contain bean name");
        assertTrue(resultText.contains("value=123"),
                "Should contain bean value");
        assertTrue(resultText.contains("component=object"),
                "Should contain component object");

        WebElement status = waitUntil(
                d -> findElement(By.id("beanWithComponentStatus")));
        Assertions.assertEquals("Bean with component serialization completed",
                status.getText());
    }

    @BrowserTest
    public void testClientCallableBeanParameter() {
        open();

        getButton("clientCallableBeanButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("clientCallableBeanResult")));
        Assertions.assertEquals(
                "ClientCallable Bean: name=ClientCallableTest, value=99, active=true",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("clientCallableBeanStatus")));
        Assertions.assertEquals("ClientCallable bean handled",
                status.getText());
    }

    @BrowserTest
    public void testClientCallableListParameter() {
        open();

        getButton("clientCallableListButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("clientCallableListResult")));
        Assertions.assertEquals(
                "ClientCallable List: [0]: name=Item1, value=111, active=true | [1]: name=Item2, value=222, active=false",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("clientCallableListStatus")));
        Assertions.assertEquals("ClientCallable list handled",
                status.getText());
    }

    @BrowserTest
    public void testClientCallableNestedBeanParameter() {
        open();

        getButton("clientCallableNestedButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("clientCallableNestedResult")));
        Assertions.assertEquals(
                "ClientCallable Nested: title=ClientCallableNested, simple.name=NestedInner, simple.value=333, simple.active=false",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("clientCallableNestedStatus")));
        Assertions.assertEquals("ClientCallable nested bean handled",
                status.getText());
    }

    @BrowserTest
    public void testClientCallableReturnBean() {
        open();

        getButton("returnBeanButton2").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("returnBeanResult")));
        Assertions.assertEquals(
                "Returned Bean: name=ReturnedBean, value=777, active=true",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("returnBeanStatus")));
        Assertions.assertEquals("Bean returned successfully", status.getText());
    }

    @BrowserTest
    public void testClientCallableReturnBeanList() {
        open();

        getButton("returnListButton2").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("returnListResult")));
        Assertions.assertEquals(
                "Returned List: [0]: name=ListItem1, value=111, active=true | [1]: name=ListItem2, value=222, active=false | [2]: name=ListItem3, value=333, active=true",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("returnListStatus")));
        Assertions.assertEquals("List returned successfully", status.getText());
    }

    @BrowserTest
    public void testClientCallableReturnNestedBean() {
        open();

        getButton("returnNestedButton2").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("returnNestedResult")));
        Assertions.assertEquals(
                "Returned Nested: title=ReturnedNested, simple.name=ReturnedInner, simple.value=999, simple.active=false",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("returnNestedStatus")));
        Assertions.assertEquals("Nested bean returned successfully",
                status.getText());
    }

    @BrowserTest
    public void testClientCallableReturnIntegerList() {
        open();

        getButton("returnIntegerListButton").click();

        WebElement result = waitUntil(
                d -> findElement(By.id("returnIntegerListResult")));
        Assertions.assertEquals("Returned Integer List: 100, 200, 300",
                result.getText());

        WebElement status = waitUntil(
                d -> findElement(By.id("returnIntegerListStatus")));
        Assertions.assertEquals("Integer list returned successfully",
                status.getText());
    }

    private WebElement getButton(String id) {
        return findElement(By.id(id));
    }
}
