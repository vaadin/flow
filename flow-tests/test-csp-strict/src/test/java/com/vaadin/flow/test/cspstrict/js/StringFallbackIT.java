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
package com.vaadin.flow.test.cspstrict.js;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.test.cspstrict.AbstractCspStrictIT;
import com.vaadin.testbench.BrowserTest;

/**
 * String JavaScript that the application declares with
 * {@code @JsStringFallback} runs from the bundle with the same {@code this},
 * parameters and return value as the string would have.
 */
public class StringFallbackIT extends AbstractCspStrictIT {

    @Override
    protected String getTestPath() {
        return "/string-fallback";
    }

    @BrowserTest
    public void declaredStrings_runFromTheBundle() {
        open();

        waitUntil(ExpectedConditions.textToBe(By.id("returned"),
                "returned " + "string JavaScript ran".length()));
        SpanElement status = $(SpanElement.class).id("status");
        Assertions.assertEquals("string JavaScript ran", status.getText());
        Assertions.assertEquals("initialized", status.getDomAttribute("title"));

        $(NativeButtonElement.class).id("button").click();

        waitUntil(ExpectedConditions.textToBe(By.id("clicked"),
                "click from the bundle"));
        assertNoCspViolations();
    }
}
