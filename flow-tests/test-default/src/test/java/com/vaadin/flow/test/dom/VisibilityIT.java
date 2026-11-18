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

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestFor(VisibilityView.class)
public class VisibilityIT extends AbstractDefaultIT {

    @BrowserTest
    public void checkParentVisibility() {
        open();

        // The element is initially hidden. It shouldn't be bound.
        assertFalse(isElementPresent(By.id("visibility")));
        assertFalse(isElementPresent(By.id("nested-label")));

        WebElement main = findElement(By.id("main"));
        WebElement div = main.findElement(By.tagName("div"));

        // make the element visible
        WebElement visibilityButton = findElement(By.id("updateVisibiity"));
        scrollIntoViewAndClick(visibilityButton);

        assertNull(div.getAttribute("hidden"));
        Assertions.assertEquals("visibility", div.getAttribute("id"));

        WebElement label = findElement(By.id("nested-label"));
        assertNull(label.getAttribute("hidden"));

        // change some properties for the element itself and it's child
        findElement(By.id("updateProperty")).click();

        Assertions.assertEquals("foo", div.getAttribute("class"));
        Assertions.assertEquals("bar", label.getAttribute("class"));

        // switch the visibility of the parent off and on
        scrollIntoViewAndClick(visibilityButton);

        Assertions.assertEquals(Boolean.TRUE.toString(),
                div.getAttribute("hidden"));

        scrollIntoViewAndClick(visibilityButton);

        assertNull(label.getAttribute("hidden"));
    }

    @BrowserTest
    public void checkChildVisibility() {
        open();

        WebElement visibilityButton = findElement(
                By.id("updateLabelVisibiity"));
        scrollIntoViewAndClick(visibilityButton);

        // The element is initially hidden. It shouldn't be bound.
        assertFalse(isElementPresent(By.id("visibility")));
        assertFalse(isElementPresent(By.id("nested-label")));

        // make the parent visible
        findElement(By.id("updateVisibiity")).click();

        // now the child element is not bound, so it's invisible
        assertFalse(isElementPresent(By.id("nested-label")));

        // change some properties for child while it's invisible
        findElement(By.id("updateProperty")).click();

        // The element is still unbound and can't be found
        assertFalse(isElementPresent(By.id("nested-label")));

        // make it visible now
        scrollIntoViewAndClick(visibilityButton);

        WebElement label = findElement(By.id("nested-label"));
        assertNull(label.getAttribute("hidden"));

        Assertions.assertEquals("bar", label.getAttribute("class"));

        // make it invisible
        scrollIntoViewAndClick(visibilityButton);

        Assertions.assertEquals(Boolean.TRUE.toString(),
                label.getAttribute("hidden"));
    }
}
