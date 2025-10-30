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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ShortcutsWithValueChangeModeIT extends ChromeBrowserTest {

    private static final Set<Keys> modifiers = Stream
            .of(Keys.SHIFT, Keys.ALT, Keys.CONTROL, Keys.META)
            .collect(Collectors.toSet());

    private String text = "Some text";

    @Test
    public void lazyValueChange_shortcutExecution_valueSentToServer() {
        assertValueCommittedOnShortcutExecution(ValueChangeMode.LAZY, true);
    }

    @Test
    public void timeoutValueChange_shortcutExecution_valueSentToServer() {
        assertValueCommittedOnShortcutExecution(ValueChangeMode.TIMEOUT, true);
    }

    @Test
    public void eagerValueChange_shortcutExecution_valueSentToServer() {
        assertValueCommittedOnShortcutExecution(ValueChangeMode.EAGER, true);
    }

    @Test
    public void onChangeValueChange_shortcutExecution_valueNotSentToServer() {
        assertValueCommittedOnShortcutExecution(ValueChangeMode.ON_CHANGE,
                false);
        // trigger change event and check value
        InputTextElement input = $(InputTextElement.class).id("input");
        input.sendKeys(Keys.ENTER);
        triggerShortcut(true);
    }

    @Test
    public void onChangeValueChange_shortcutExecution_resetFocusOnActiveElement_valueSentToServer() {
        open(ValueChangeMode.ON_CHANGE.name());

        InputTextElement input = $(InputTextElement.class).id("input");
        input.focus();
        input.sendKeys(text);

        doTriggerShortcut(true, Keys.CONTROL, Keys.ENTER);
    }

    @Test
    public void onBlurValueChange_shortcutExecution_valueNotSentToServer() {
        assertValueCommittedOnShortcutExecution(ValueChangeMode.ON_BLUR, false);
        // trigger blur event and check value
        NativeButtonElement button = $(NativeButtonElement.class).id("button");
        button.focus();
        triggerShortcut(true);
    }

    private void assertValueCommittedOnShortcutExecution(ValueChangeMode mode,
            boolean expectValue) {
        open(mode.name());

        InputTextElement input = $(InputTextElement.class).id("input");
        input.focus();
        input.sendKeys(text);

        triggerShortcut(expectValue);
    }

    private void triggerShortcut(boolean expectValue) {
        doTriggerShortcut(expectValue, Keys.CONTROL, Keys.ALT, "s");
    }

    private void doTriggerShortcut(boolean expectValue, CharSequence... keys) {
        sendKeys(driver, keys);

        String paragraphText = $(ParagraphElement.class).id("value").getText();

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

    public static void sendKeys(WebDriver driver, CharSequence... keys) {
        Actions actions = new Actions(driver);
        for (CharSequence keySeq : keys) {
            if (modifiers.contains(keySeq)) {
                actions.keyDown(keySeq);
            } else {
                actions.sendKeys(keySeq);
            }
        }
        actions.build().perform();
        // Implementation that worked for driver < 75.beta:
        // new Actions(driver).sendKeys(keys).build().perform();
        // if keys are not reset, alt will remain down and start flip-flopping
        resetKeys(driver);
    }

    public static void resetKeys(WebDriver driver) {
        Actions actions = new Actions(driver);
        modifiers.forEach(actions::keyUp);
        actions.build().perform();
        // Implementation that worked for driver < 75.beta:
        // new Actions(driver).sendKeys(Keys.NULL).build().perform();
    }

}
