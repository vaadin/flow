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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.By;

/**
 * Normal tests with @Before are not implemented because each @Test starts new
 * Chrome process.
 */
public class ListBindingIT extends ChromeBrowserTest {

    @Test
    public void listDataBinding() {
        open();

        WebElement template = findElement(By.id("template"));

        checkInitialState(template);

        // Before running those methods list is set to ["1", "2", "3"]
        // see (ListBindingTemplate.INITIAL_STATE)

        checkListInsideList(template);

        assertMethodWorksCorrectly("addElement", template, "1", "2", "3", "4");

        assertMethodWorksCorrectly("addElementByIndex", template, "4", "1", "2",
                "3");

        assertMethodWorksCorrectly("addNumerousElements", template, "1", "2",
                "3", "4", "5");

        assertMethodWorksCorrectly("addNumerousElementsByIndex", template, "4",
                "5", "1", "2", "3");

        assertMethodWorksCorrectly("clearList", template);

        assertMethodWorksCorrectly("removeSecondElementByIndex", template, "1",
                "3");

        assertMethodWorksCorrectly("removeFirstElementWithIterator", template,
                "2", "3");

        assertMethodWorksCorrectly("swapFirstAndSecond", template, "2", "1",
                "3");

        assertMethodWorksCorrectly("sortDescending", template, "3", "2", "1");

        assertMethodWorksCorrectly("setInitialStateToEachMessage", template,
                ListBindingTemplate.INITIAL_STATE,
                ListBindingTemplate.INITIAL_STATE,
                ListBindingTemplate.INITIAL_STATE);
    }

    private void checkListInsideList(WebElement template) {
        resetState(template);
        List<WebElement> msgs = findInShadowRoot(template, By.className("msg"));

        // Click b message
        msgs.get(1).click();

        // Assert that the message was gotten correctly on the server side
        Assert.assertEquals("Couldn't validate element click selection.",
                findElement(By.id("selection")).getText(),
                "Clicked message: " + msgs.get(1).getText());

        msgs = findInShadowRoot(template, By.className("submsg"));
        Assert.assertEquals("Wrong amount of nested messages", 4, msgs.size());

        msgs.get(1).click();

        Assert.assertEquals("Couldn't validate list selection.",
                findElement(By.id("multi-selection")).getText(),
                "Clicked message List: 3 abc");

        msgs.get(3).click();

        Assert.assertEquals("Couldn't validate list selection.",
                findElement(By.id("multi-selection")).getText(),
                "Clicked message List: 1 d");
    }

    private void checkInitialState(WebElement template) {
        Assert.assertEquals(
                Collections.singletonList(ListBindingTemplate.INITIAL_STATE),
                getMessages(template));
    }

    private void assertMethodWorksCorrectly(String handlerName,
            WebElement template, String... expectedMessages) {
        resetState(template);
        getInShadowRoot(template, By.id(handlerName)).get().click();

        Assert.assertEquals(Arrays.asList(expectedMessages),
                getMessages(template));
    }

    private void resetState(WebElement template) {
        getInShadowRoot(template, By.id("reset")).get().click();
        Assert.assertEquals(ListBindingTemplate.RESET_STATE,
                getMessages(template));
    }

    private List<String> getMessages(WebElement template) {
        return findInShadowRoot(template, By.className("msg")).stream()
                .map(WebElement::getText).collect(Collectors.toList());
    }
}
