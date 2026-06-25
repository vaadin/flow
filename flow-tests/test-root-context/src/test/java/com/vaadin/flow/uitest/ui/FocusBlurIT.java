/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Keys;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class FocusBlurIT extends ChromeBrowserTest {

    @Test
    public void focusAndBlur_serverSideFiredEvents_isFromClientFalse() {
        open();

        $(NativeButtonElement.class).id("server-side").click();

        $(NativeButtonElement.class).id("focus").click();
        SpanElement focusEvent = $(SpanElement.class).id("focus-event");
        SpanElement blurEvent = $(SpanElement.class).id("blur-event");

        Assert.assertEquals("Focused: false", focusEvent.getText());
        Assert.assertEquals("Blurred: false", blurEvent.getText());
    }

    @Test
    public void focusAndBlur_clientSideFiredEvents_isFromClientTrue() {
        open();

        $(NativeButtonElement.class).id("client-side").click();

        var input = $(InputTextElement.class).id("input");
        input.click();
        input.sendKeys(Keys.TAB);

        SpanElement focusEvent = $(SpanElement.class).id("focus-event");
        SpanElement blurEvent = $(SpanElement.class).id("blur-event");

        Assert.assertEquals("Focused: true", focusEvent.getText());
        Assert.assertEquals("Blurred: true", blurEvent.getText());
    }
}
