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

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Tests for validating the {@code removeAll()} feature, that should clear all
 * the server nodes and also the client nodes that the server doesn't know
 * about.
 * 
 * @author Vaadin Ltd.
 */
public class ClearNodeChildrenIT extends ChromeBrowserTest {

    private WebElement root;
    private WebElement message;

    @Before
    public void init() {
        open();
        waitForElementPresent(By.id("root"));
        root = findElement(By.id("root"));
        message = findInShadowRoot(root, By.id("message")).get(0);
    }

    @Test
    public void clearContainerWithClientSideNodes_allNodesAreRemoved() {
        processContainerWithClientSideNodes_allNodesAreRemoved(
                "clearContainer1",
                "Div 'containerWithElementChildren' cleared.", "");
    }

    @Test
    public void setTextToContainerWithClientSideNodes_allNodesAreRemoved() {
        processContainerWithClientSideNodes_allNodesAreRemoved(
                "setTextToContainer1",
                "Div 'containerWithElementChildren' text set to 'Hello World'.",
                "Hello World");
    }

    private void processContainerWithClientSideNodes_allNodesAreRemoved(
            String buttonToClick, String expectedMessage,
            String expectedInnerText) {
        WebElement container = findInShadowRoot(root,
                By.id("containerWithElementChildren")).get(0);
        List<WebElement> divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(2, divs.size());

        WebElement button = findInShadowRoot(root, By.id(buttonToClick)).get(0);
        String oldTtext = message.getText();
        button.click();
        waitForMessageToChange(oldTtext);

        assertMessageEndsWith(expectedMessage);
        divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(0, divs.size());
        Assert.assertEquals(expectedInnerText,
                container.getAttribute("innerText"));
    }

    @Test
    public void addTextNode_clearContainerWithClientSideNodes_allNodesAreRemoved() {
        addTextNode_processContainerWithClientSideNodes_allNodesAreRemoved(
                "clearContainer1",
                "Div 'containerWithElementChildren' cleared.", "");
    }

    @Test
    public void addTextNode_setTextToContainerWithClientSideNodes_allNodesAreRemoved() {
        addTextNode_processContainerWithClientSideNodes_allNodesAreRemoved(
                "setTextToContainer1",
                "Div 'containerWithElementChildren' text set to 'Hello World'.",
                "Hello World");
    }

    private void addTextNode_processContainerWithClientSideNodes_allNodesAreRemoved(
            String buttonToClick, String expectedMessage,
            String expectedInnerText) {
        WebElement container = findInShadowRoot(root,
                By.id("containerWithElementChildren")).get(0);
        List<WebElement> divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(2, divs.size());

        WebElement addTextNode = findInShadowRoot(root,
                By.id("addTextNodeToContainer1")).get(0);
        String oldTtext = message.getText();
        addTextNode.click();
        waitForMessageToChange(oldTtext);
        assertMessageEndsWith(
                "Added 'Text node' to div with id 'containerWithElementChildren'.");

        WebElement button = findInShadowRoot(root, By.id(buttonToClick)).get(0);
        oldTtext = message.getText();
        button.click();
        waitForMessageToChange(oldTtext);

        assertMessageEndsWith(expectedMessage);
        divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(0, divs.size());
        Assert.assertEquals(expectedInnerText,
                container.getAttribute("innerText"));
    }

    @Test
    public void clearContainerWithClientAndServerSideNodes_allNodesAreRemoved_serverNodesAreDetached() {
        processContainerWithClientAndServerSideNodes_allNodesAreRemoved_serverNodesAreDetached(
                "clearContainer1",
                "Div 'Server div 1' detached.\nDiv 'containerWithElementChildren' cleared.",
                "");
    }

    @Test
    public void setTextToContainerWithClientAndServerSideNodes_allNodesAreRemoved_serverNodesAreDetached() {
        processContainerWithClientAndServerSideNodes_allNodesAreRemoved_serverNodesAreDetached(
                "setTextToContainer1",
                "Div 'Server div 1' detached.\nDiv 'containerWithElementChildren' text set to 'Hello World'.",
                "Hello World");
    }

    private void processContainerWithClientAndServerSideNodes_allNodesAreRemoved_serverNodesAreDetached(
            String buttonToClick, String expectedMessage,
            String expectedInnerText) {
        WebElement container = findInShadowRoot(root,
                By.id("containerWithElementChildren")).get(0);
        List<WebElement> divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(2, divs.size());

        WebElement add = findInShadowRoot(root, By.id("addChildToContainer1"))
                .get(0);
        String oldTtext = message.getText();
        add.click();
        waitForMessageToChange(oldTtext);
        assertMessageEndsWith("Div 'Server div 1' attached.");

        divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(3, divs.size());

        WebElement button = findInShadowRoot(root, By.id(buttonToClick)).get(0);
        oldTtext = message.getText();
        button.click();
        waitForMessageToChange(oldTtext);

        assertMessageEndsWith(expectedMessage);
        divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(0, divs.size());
        Assert.assertEquals(expectedInnerText,
                container.getAttribute("innerText"));
    }

    @Test
    public void clearContainerWithTextNodes_allNodesAreRemoved() {
        processContainerWithTextNodes_allNodesAreRemoved("clearContainer2",
                "Div 'containerWithMixedChildren' cleared.", "");
    }

    @Test
    public void setTextToContainerWithTextNodes_allNodesAreRemoved() {
        processContainerWithTextNodes_allNodesAreRemoved("setTextToContainer2",
                "Div 'containerWithMixedChildren' text set to 'Hello World'.",
                "Hello World");
    }

    private void processContainerWithTextNodes_allNodesAreRemoved(
            String buttonToClick, String expectedMessage,
            String expectedInnerText) {
        WebElement container = findInShadowRoot(root,
                By.id("containerWithMixedChildren")).get(0);

        Assert.assertThat(container.getText(),
                CoreMatchers.allOf(CoreMatchers.containsString("Some text 1"),
                        CoreMatchers.containsString("Some text 2"),
                        CoreMatchers.containsString("Some text 3")));

        List<WebElement> divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(2, divs.size());

        WebElement button = findInShadowRoot(root, By.id(buttonToClick)).get(0);
        String oldTtext = message.getText();
        button.click();
        waitForMessageToChange(oldTtext);

        assertMessageEndsWith(expectedMessage);
        divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(0, divs.size());
        Assert.assertEquals(expectedInnerText,
                container.getAttribute("innerText"));
    }

    @Test
    public void addClientSideChildren_clearContainer_allNodesAreRemoved() {
        addClientSideChildren_processContainer_allNodesAreRemoved(
                "clearContainer3",
                "Div 'containerWithClientSideChildren' cleared.", "");
    }

    @Test
    public void addClientSideChildren_setTextToContainer_allNodesAreRemoved() {
        addClientSideChildren_processContainer_allNodesAreRemoved(
                "setTextToContainer3",
                "Div 'containerWithClientSideChildren' text set to 'Hello World'.",
                "Hello World");
    }

    private void addClientSideChildren_processContainer_allNodesAreRemoved(
            String buttonToClick, String expectedMessage,
            String expectedInnerText) {
        WebElement container = findInShadowRoot(root,
                By.id("containerWithClientSideChildren")).get(0);

        List<WebElement> divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(0, divs.size());

        WebElement add = findInShadowRoot(root, By.id("addClientSideChild"))
                .get(0);
        add.click();
        divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(1, divs.size());

        WebElement button = findInShadowRoot(root, By.id(buttonToClick)).get(0);
        String oldTtext = message.getText();
        button.click();
        waitForMessageToChange(oldTtext);

        assertMessageEndsWith(expectedMessage);
        divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(0, divs.size());
        Assert.assertEquals(expectedInnerText,
                container.getAttribute("innerText"));
    }

    @Test
    public void addClienAndServertSideChildren_clearContainer_allNodesAreRemoved_serverNodesAreDetached() {
        addClienAndServertSideChildren_processContainer_allNodesAreRemoved_serverNodesAreDetached(
                "clearContainer3",
                "Div 'Server div 1' detached.\nDiv 'containerWithClientSideChildren' cleared.",
                "");
    }

    @Test
    public void addClienAndServertSideChildren_setTextToContainer_allNodesAreRemoved_serverNodesAreDetached() {
        addClienAndServertSideChildren_processContainer_allNodesAreRemoved_serverNodesAreDetached(
                "setTextToContainer3",
                "Div 'Server div 1' detached.\nDiv 'containerWithClientSideChildren' text set to 'Hello World'.",
                "Hello World");
    }

    private void addClienAndServertSideChildren_processContainer_allNodesAreRemoved_serverNodesAreDetached(
            String buttonToClick, String expectedMessage,
            String expectedInnerText) {
        WebElement container = findInShadowRoot(root,
                By.id("containerWithClientSideChildren")).get(0);

        List<WebElement> divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(0, divs.size());

        WebElement addClientNode = findInShadowRoot(root,
                By.id("addClientSideChild")).get(0);
        addClientNode.click();
        divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(1, divs.size());

        WebElement addServerNode = findInShadowRoot(root,
                By.id("addChildToContainer3")).get(0);
        String oldTtext = message.getText();
        addServerNode.click();
        waitForMessageToChange(oldTtext);
        assertMessageEndsWith("Div 'Server div 1' attached.");

        divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(2, divs.size());

        WebElement button = findInShadowRoot(root, By.id(buttonToClick)).get(0);
        oldTtext = message.getText();
        button.click();
        waitForMessageToChange(oldTtext);

        assertMessageEndsWith(expectedMessage);
        divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(0, divs.size());
        Assert.assertEquals(expectedInnerText,
                container.getAttribute("innerText"));
    }

    @Test
    public void addNodeToSlot_clearContainer_allNodesAreRemoved_serverNodesAreDetached() {
        addNodeToSlot_processContainer_allNodesAreRemoved_serverNodesAreDetached(
                "clear", "Div 'Server div 1' detached.\nDiv 'root' cleared.",
                "");
    }

    @Test
    public void addNodeToSlot_setTextToContainer_allNodesAreRemoved_serverNodesAreDetached() {
        addNodeToSlot_processContainer_allNodesAreRemoved_serverNodesAreDetached(
                "setText",
                "Div 'Server div 1' detached.\nDiv 'root' text set to 'Hello World'.",
                "Hello World");
    }

    private void addNodeToSlot_processContainer_allNodesAreRemoved_serverNodesAreDetached(
            String buttonToClick, String expectedMessage,
            String expectedInnerText) {
        List<WebElement> divs = root.findElements(By.tagName("div"));
        Assert.assertEquals(0, divs.size());

        WebElement add = findInShadowRoot(root, By.id("addChildToSlot")).get(0);
        String oldTtext = message.getText();
        add.click();
        waitForMessageToChange(oldTtext);
        assertMessageEndsWith("Div 'Server div 1' attached.");

        WebElement button = findInShadowRoot(root, By.id(buttonToClick)).get(0);
        oldTtext = message.getText();
        button.click();
        waitForMessageToChange(oldTtext);

        assertMessageEndsWith(expectedMessage);
        divs = root.findElements(By.tagName("div"));
        Assert.assertEquals(0, divs.size());
        Assert.assertEquals(expectedInnerText, root.getAttribute("innerText"));
    }

    @Test
    public void addNodeToNestedContainer_clearParentContainer_allNodesAreRemoved() {
        addNodeToNestedContainer_processParentContainer_allNodesAreRemoved(
                "clearContainer4", "Div 'containerWithContainer' cleared.", "");
    }

    @Test
    public void addNodeToNestedContainer_setTextToParentContainer_allNodesAreRemoved() {
        addNodeToNestedContainer_processParentContainer_allNodesAreRemoved(
                "setTextToContainer4",
                "Div 'containerWithContainer' text set to 'Hello World'.",
                "Hello World");
    }

    private void addNodeToNestedContainer_processParentContainer_allNodesAreRemoved(
            String buttonToClick, String expectedMessage,
            String expectedInnerText) {
        WebElement container = findInShadowRoot(root, By.id("nestedContainer"))
                .get(0);

        List<WebElement> divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(0, divs.size());

        WebElement add = findInShadowRoot(root,
                By.id("addChildToNestedContainer")).get(0);
        String oldTtext = message.getText();
        add.click();
        waitForMessageToChange(oldTtext);
        assertMessageEndsWith("Div 'Server div 1' attached.");

        divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(1, divs.size());

        WebElement button = findInShadowRoot(root, By.id(buttonToClick)).get(0);
        oldTtext = message.getText();
        button.click();
        waitForMessageToChange(oldTtext);

        /*
         * Note that in this setup, the server-side components are not detached.
         */
        assertMessageEndsWith(expectedMessage);

        container = findInShadowRoot(root, By.id("containerWithContainer"))
                .get(0);
        divs = container.findElements(By.tagName("div"));
        Assert.assertEquals(0, divs.size());
        Assert.assertEquals(expectedInnerText, container.getText());
    }

    private void assertMessageEndsWith(String text) {
        Assert.assertThat(message.getText(), CoreMatchers.endsWith(text));
    }

    private void waitForMessageToChange(String oldText) {
        waitUntilNot(driver -> message.getText().equals(oldText));
    }

}
