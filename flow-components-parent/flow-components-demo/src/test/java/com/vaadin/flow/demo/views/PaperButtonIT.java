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
package com.vaadin.flow.demo.views;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.AbstractChromeTest;
import com.vaadin.testbench.By;

/**
 * Integration tests for the {@link PaperButtonView}.
 *
 */
public class PaperButtonIT extends AbstractChromeTest {

    @Test
    public void clickOnButtons_textIsDisaplayed() {
        open();

        waitForElementPresent(By.tagName("main-layout"));
        WebElement layout = findElement(By.tagName("main-layout"));
        List<WebElement> buttons = layout
                .findElements(By.tagName("paper-button"));
        Assert.assertEquals(4, buttons.size());

        WebElement message = layout.findElement(By.id("buttonsMessage"));

        int raisedCount = 0;
        int togglesCount = 0;
        int activeCount = 0;
        int disabledCount = 0;

        for (WebElement button : buttons) {
            if (isAttributeTrue(button, "raised")) {
                raisedCount++;
            }

            if (isAttributeTrue(button, "toggles")) {
                togglesCount++;
            }

            if (button.getAttribute("disabled") == null) {
                button.click();
                waitUntil(driver -> message.getText().equals(
                        "Button " + button.getText() + " was clicked."));

                if (isAttributeTrue(button, "toggles")
                        && isAttributeTrue(button, "active")) {
                    activeCount++;
                }
            } else {
                disabledCount++;
            }
        }

        Assert.assertEquals(2, raisedCount);
        Assert.assertEquals(1, togglesCount);
        Assert.assertEquals(1, activeCount);
        Assert.assertEquals(1, disabledCount);
    }

    private boolean isAttributeTrue(WebElement element, String attribute) {
        return element.getAttribute(attribute) != null
                && !"false".equals(element.getAttribute(attribute));
    }

    @Override
    protected String getTestPath() {
        return "/paper-button";
    }

}
