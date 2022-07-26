/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ShortcutsWithLazyValueChangeIT extends ChromeBrowserTest {

    private static final Set<Keys> modifiers = Stream
            .of(Keys.SHIFT, Keys.ALT, Keys.CONTROL, Keys.META)
            .collect(Collectors.toSet());

    @Test
    public void debouncedEventFlushedOnShortcutExecution() {
        open();
        String text = "some text";

        InputTextElement input = $(InputTextElement.class).id("input");
        input.focus();
        input.sendKeys(text);

        sendKeys(Keys.CONTROL, "s");

        Assert.assertEquals(text,
                $(ParagraphElement.class).id("value").getText());

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
