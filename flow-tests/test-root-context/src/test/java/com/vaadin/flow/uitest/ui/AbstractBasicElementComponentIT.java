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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractBasicElementComponentIT
        extends ChromeBrowserTest {

    @Test
    public void ensureDomUpdatesAndEventsDoSomething() {
        open();
        assertDomUpdatesAndEventsDoSomething();
    }

    protected void assertDomUpdatesAndEventsDoSomething() {
        Assert.assertEquals(0, getThankYouCount());
        $(InputTextElement.class).first().setValue("abc");

        // Need to call WebElement.click(), because TestBenchElement.click()
        // clicks with JS which doesn't move the mouse on IE11, and breaks the
        // mouse coordinate test.
        $(NativeButtonElement.class).first().getWrappedElement().click();

        Assert.assertEquals(1, getThankYouCount());

        String buttonText = getThankYouElements().get(0).getText();
        String expected = "Thank you for clicking \"Click me\" at \\((\\d+),(\\d+)\\)! The field value is abc";
        Assert.assertTrue(
                "Expected '" + expected + "', was '" + buttonText + "'",
                buttonText.matches(expected));

        // Clicking removes the element
        getThankYouElements().get(0).click();

        Assert.assertEquals(0, getThankYouCount());

        WebElement helloElement = findElement(By.id("hello-world"));

        Assert.assertEquals("Hello world", helloElement.getText());
        Assert.assertEquals("hello", helloElement.getAttribute("class"));

        helloElement.click();
        Assert.assertEquals("Stop touching me!", helloElement.getText());
        Assert.assertEquals("", helloElement.getAttribute("class"));

        // Clicking again shouldn't have any effect
        helloElement.click();
        Assert.assertEquals("Stop touching me!", helloElement.getText());
    }

    protected int getThankYouCount() {
        return getThankYouElements().size();
    }

    protected List<WebElement> getThankYouElements() {
        return findElements(By.cssSelector(".thankYou"));
    }

}
