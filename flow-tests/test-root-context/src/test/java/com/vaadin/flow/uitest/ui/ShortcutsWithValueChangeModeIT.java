/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

package com.vaadin.flow.uitest.ui;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Keys;
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
        sendKeys(Keys.CONTROL, Keys.ALT, "s");

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

    private void sendKeys(CharSequence... keys) {
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
        resetKeys();
    }

    private void resetKeys() {
        Actions actions = new Actions(driver);
        modifiers.forEach(actions::keyUp);
        actions.build().perform();
        // Implementation that worked for driver < 75.beta:
        // new Actions(driver).sendKeys(Keys.NULL).build().perform();
    }

}
