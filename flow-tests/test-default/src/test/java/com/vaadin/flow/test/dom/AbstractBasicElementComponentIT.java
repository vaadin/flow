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
package com.vaadin.flow.test.dom;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared checks for the views that build the same UI once with the Element API
 * and once with components.
 */
public abstract class AbstractBasicElementComponentIT
        extends AbstractDefaultIT {

    @BrowserTest
    public void ensureDomUpdatesAndEventsDoSomething() {
        open();
        assertDomUpdatesAndEventsDoSomething();
    }

    protected void assertDomUpdatesAndEventsDoSomething() {
        Assertions.assertEquals(0, getThankYouCount());
        $(InputTextElement.class).first().setValue("abc");

        // Need to call WebElement.click(), because TestBenchElement.click()
        // clicks with JS which doesn't move the mouse, and breaks the mouse
        // coordinate test.
        $(NativeButtonElement.class).first().getWrappedElement().click();

        Assertions.assertEquals(1, getThankYouCount());

        String buttonText = getThankYouElements().get(0).getText();
        String expected = "Thank you for clicking \"Click me\" at \\((\\d+),(\\d+)\\)! The field value is abc";
        assertTrue(buttonText.matches(expected),
                "Expected '" + expected + "', was '" + buttonText + "'");

        // Clicking removes the element
        getThankYouElements().get(0).click();

        Assertions.assertEquals(0, getThankYouCount());

        WebElement helloElement = findElement(By.id("hello-world"));

        Assertions.assertEquals("Hello world", helloElement.getText());
        Assertions.assertEquals("hello", helloElement.getAttribute("class"));

        helloElement.click();
        Assertions.assertEquals("Stop touching me!", helloElement.getText());
        Assertions.assertEquals("", helloElement.getAttribute("class"));

        // Clicking again shouldn't have any effect
        helloElement.click();
        Assertions.assertEquals("Stop touching me!", helloElement.getText());
    }

    private int getThankYouCount() {
        return getThankYouElements().size();
    }

    private List<WebElement> getThankYouElements() {
        return findElements(By.cssSelector(".thankYou"));
    }
}
