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

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * Regression test for issue #25213: pressing the shortcut key right after the
 * overlay opens, while focus is still on the element hosting the overlay, must
 * trigger a shortcut owned by a component slotted into that overlay.
 */
public class PopoverHostFocusShortcutIT extends ChromeBrowserTest {

    private TestBenchElement eventLog;

    @Before
    public void init() {
        open();
        $(NativeButtonElement.class)
                .id(PopoverHostFocusShortcutView.OPEN_BUTTON).click();
        eventLog = $(DivElement.class)
                .id(PopoverHostFocusShortcutView.EVENT_LOG_ID);
    }

    @Test
    public void overlayHostFocused_shortcutFires() {
        // No explicit focus call: the view focuses the host on open, the same
        // way the Dialog focus trap does.
        new Actions(getDriver()).sendKeys(Keys.ENTER).perform();

        waitUntil(driver -> eventLog.findElements(By.tagName("div")).stream()
                .anyMatch(e -> e.getText()
                        .contains(PopoverHostFocusShortcutView.CONFIRMED)));
    }

    @Test
    public void slottedFieldFocused_shortcutFires() {
        final InputTextElement field = $(InputTextElement.class)
                .id(PopoverHostFocusShortcutView.FIELD_ID);
        field.focus();
        field.sendKeys(Keys.ENTER);

        waitUntil(driver -> eventLog.findElements(By.tagName("div")).stream()
                .anyMatch(e -> e.getText()
                        .contains(PopoverHostFocusShortcutView.CONFIRMED)));
    }
}
