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
package com.vaadin.flow.uitest.ui.signal;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration tests for binding component value state to signals.
 */
public class BindValueIT extends ChromeBrowserTest {

    @Test
    public void setValue_throwBindingActiveException() {
        open();

        NativeButtonElement changeSignalValue = $(NativeButtonElement.class)
                .id("change-value-button");

        changeSignalValue.click();

        Assert.assertEquals("BindingActiveException",
                $(DivElement.class).id("value-info").getText());
    }

    @Test
    public void changeSignalValue_updatesInputValue() {
        open();

        NativeButtonElement changeSignalValue = $(NativeButtonElement.class)
                .id("change-signal-value-button");

        Assert.assertEquals("", getTargetInput().getValue());
        Assert.assertEquals("", getValueText());
        Assert.assertEquals("", getSignalValueText());
        changeSignalValue.click();
        Assert.assertEquals("bar", getTargetInput().getValue());
        Assert.assertEquals("bar", getValueText());
        Assert.assertEquals("bar", getSignalValueText());
        Assert.assertEquals("1", getCounterNumber());

        // expect no change when clicking again
        changeSignalValue.click();
        Assert.assertEquals("bar", getTargetInput().getValue());
        Assert.assertEquals("bar", getValueText());
        Assert.assertEquals("bar", getSignalValueText());
        Assert.assertEquals("1", getCounterNumber());
    }

    // This simulates internal value change via subclassing or similar
    @Test
    public void changeValueInternally_updatesModelAndSignalValue() {
        open();

        NativeButtonElement changeSignalValue = $(NativeButtonElement.class)
                .id("internal-change-value-button");

        Assert.assertEquals("", getTargetInput().getValue());
        Assert.assertEquals("", getValueText());
        Assert.assertEquals("", getSignalValueText());
        changeSignalValue.click();
        Assert.assertEquals("", getTargetInput().getValue());
        Assert.assertEquals("bar", getValueText());
        Assert.assertEquals("bar", getSignalValueText());
        Assert.assertEquals("1", getCounterNumber());

        // expect no change when clicking again
        changeSignalValue.click();
        Assert.assertEquals("", getTargetInput().getValue());
        Assert.assertEquals("bar", getValueText());
        Assert.assertEquals("bar", getSignalValueText());
        Assert.assertEquals("1", getCounterNumber());
    }

    @Test
    public void changeValueFromClient_updatesSignalValue() {
        open();

        InputTextElement targetInput = getTargetInput();

        Assert.assertEquals("", getValueText());
        Assert.assertEquals("", getSignalValueText());

        targetInput.setValue("foo");
        Assert.assertEquals("foo", getValueText());
        Assert.assertEquals("foo", getSignalValueText());
        Assert.assertEquals("1", getCounterNumber());
    }

    private InputTextElement getTargetInput() {
        return $(InputTextElement.class).id("target");
    }

    private String getValueText() {
        return $(DivElement.class).id("value-info").getText()
                .replaceFirst("Value:", "").trim();
    }

    private String getSignalValueText() {
        return $(DivElement.class).id("signal-value-info").getText()
                .replaceFirst("Signal:", "").trim();
    }

    private String getCounterNumber() {
        return $(DivElement.class).id("counter").getText()
                .replaceFirst("ValueChange #", "");
    }
}
