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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class FocusBlurIT extends ChromeBrowserTest {

    @Test
    public void focusAndBlur_serverSideFiredEvents_isFromClientFalse() {
        open();

        $(NativeButtonElement.class).id("focus").click();
        SpanElement focusEvent = $(SpanElement.class).id("focus-event");
        SpanElement blurEvent = $(SpanElement.class).id("blur-event");

        Assert.assertEquals("Focused: false", focusEvent.getText());
        Assert.assertEquals("Blurred: false", blurEvent.getText());
    }

    @Test
    void focusAndBlur_clientSideFiredEvents_isFromClientTrue() {
        open();

        var input = $(InputTextElement.class).id("input");
        input.click();
        input.sendKeys(Keys.TAB);

        SpanElement focusEvent = $(SpanElement.class).id("focus-event");
        SpanElement blurEvent = $(SpanElement.class).id("blur-event");

        Assert.assertEquals("Focused: true", focusEvent.getText());
        Assert.assertEquals("Blurred: true", blurEvent.getText());
    }
}
