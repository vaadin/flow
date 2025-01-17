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

package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Keys;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ShadowRootShortcutsWithValueChangeModeIT
        extends ChromeBrowserTest {

    private String text = "Some text";

    @Test
    public void onChangeValueChange_shortcutExecution_valueNotSentToServer() {
        assertValueCommittedOnShortcutExecution(ValueChangeMode.ON_CHANGE,
                false);
        DivElement div = $(DivElement.class).id("test-element");

        // trigger change event and check value
        InputTextElement input = div.$(InputTextElement.class).id("input");
        input.sendKeys(Keys.ENTER);
        triggerShortcut(true);
    }

    @Test
    public void onChangeValueChange_shortcutExecution_resetFocusOnActiveElement_valueSentToServer() {
        open(ValueChangeMode.ON_CHANGE.name());

        DivElement div = $(DivElement.class).id("test-element");
        InputTextElement input = div.$(InputTextElement.class).id("input");
        input.focus();
        input.sendKeys(text);

        doTriggerShortcut(true, Keys.CONTROL, Keys.ENTER);
    }

    private void assertValueCommittedOnShortcutExecution(ValueChangeMode mode,
            boolean expectValue) {
        open(mode.name());

        DivElement div = $(DivElement.class).id("test-element");
        InputTextElement input = div.$(InputTextElement.class).id("input");
        input.focus();
        input.sendKeys(text);

        triggerShortcut(expectValue);
    }

    private void triggerShortcut(boolean expectValue) {
        doTriggerShortcut(expectValue, Keys.CONTROL, Keys.ALT, "s");
    }

    private void doTriggerShortcut(boolean expectValue, CharSequence... keys) {
        ShortcutsWithValueChangeModeIT.sendKeys(driver, keys);

        DivElement div = $(DivElement.class).id("test-element");
        String paragraphText = div.$(ParagraphElement.class).id("value")
                .getText();

        if (expectValue) {
            Assert.assertEquals(
                    "Expecting input value to be in sync with server value",
                    text, paragraphText);
        } else {
            Assert.assertEquals(
                    "Expecting input value not to be synced with server", "",
                    paragraphText);
        }
    }

}
