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
import org.openqa.selenium.Keys;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(FocusBlurView.class)
public class FocusBlurIT extends AbstractDefaultIT {

    @BrowserTest
    public void focusAndBlur_serverSideFiredEvents_isFromClientFalse() {
        open();

        $(NativeButtonElement.class).id("server-side").click();

        $(NativeButtonElement.class).id("focus").click();
        waitForElementPresent(By.id("blur-event"));
        SpanElement focusEvent = $(SpanElement.class).id("focus-event");
        SpanElement blurEvent = $(SpanElement.class).id("blur-event");

        Assertions.assertEquals("Focused: false", focusEvent.getText());
        Assertions.assertEquals("Blurred: false", blurEvent.getText());
    }

    @BrowserTest
    public void focusAndBlur_clientSideFiredEvents_isFromClientTrue() {
        open();

        $(NativeButtonElement.class).id("client-side").click();

        var input = $(InputTextElement.class).id("input");
        input.click();
        input.sendKeys(Keys.TAB);

        SpanElement focusEvent = $(SpanElement.class).id("focus-event");
        SpanElement blurEvent = $(SpanElement.class).id("blur-event");

        Assertions.assertEquals("Focused: true", focusEvent.getText());
        Assertions.assertEquals("Blurred: true", blurEvent.getText());
    }
}
