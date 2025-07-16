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

package com.vaadin.flow.watermarked.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class WatermarkIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "";
    }

    @Test
    public void shouldAddWatermark() {
        open();
        WebElement watermark = $("body").single()
                // Should unwrap to get shadow root, because TestBenchElement
                // throws unsupported operation exception
                .getWrappedElement().getShadowRoot()
                // By.tagName is not working, using css selector as a workaround
                .findElement(By.cssSelector("vaadin-watermark"));
        Assert.assertTrue(
                "Expected Watermark component to be shown, but was not",
                watermark.isDisplayed());
        Assert.assertTrue(
                "Expected subscription needed message to be shown, but was not",
                watermark.getShadowRoot().findElements(By.cssSelector("*"))
                        .stream().map(WebElement::getText)
                        .anyMatch(text -> text.contains(
                                "Commercial features require a subscription")));
    }

}
