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
package com.example.application;

import org.junit.Test;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class StringFallbackIT extends ChromeBrowserTest {

    @Test
    public void sendDeclaredStrings_runFromTheBundle() {
        open();

        $(NativeButtonElement.class).id(StringFallbackView.SEND_DECLARED_ID)
                .click();

        SpanElement returned = $(SpanElement.class)
                .id(StringFallbackView.RETURNED_ID);
        waitUntil(driver -> ("returned " + "string ran".length())
                .equals(returned.getText()));
        SpanElement status = $(SpanElement.class)
                .id(StringFallbackView.STATUS_ID);
        waitUntil(driver -> "initialized"
                .equals(status.getDomAttribute("title")));
        checkLogsForErrors();
    }

    @Test
    public void clickWithDeclaredFilterAndEventData_eventDataRead() {
        open();

        $(NativeButtonElement.class).id(StringFallbackView.CLICK_ID).click();

        SpanElement clicked = $(SpanElement.class)
                .id(StringFallbackView.CLICKED_ID);
        waitUntil(driver -> "click from the bundle".equals(clicked.getText()));
        checkLogsForErrors();
    }

    @Test
    public void sendUndeclaredString_refused() {
        open();

        $(NativeButtonElement.class).id(StringFallbackView.SEND_UNDECLARED_ID)
                .click();

        SpanElement result = $(SpanElement.class)
                .id(StringFallbackView.UNDECLARED_RESULT_ID);
        waitUntil(driver -> "refused".equals(result.getText()));
    }

    @Override
    protected String getTestPath() {
        return "/string-fallback";
    }
}
