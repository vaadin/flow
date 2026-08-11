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

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * Regression test for issue #24974: a shortcut owned by a component inside a
 * popover must still fire for keydowns originating in that popover.
 */
public class PopoverOwnerShortcutIT extends ChromeBrowserTest {

    private TestBenchElement eventLog;

    @Before
    public void init() {
        open();
        $(NativeButtonElement.class).id(PopoverOwnerShortcutView.OPEN_BUTTON)
                .click();
        eventLog = $(DivElement.class)
                .id(PopoverOwnerShortcutView.EVENT_LOG_ID);
    }

    @Test
    public void ownerInsidePopoverFocused_shortcutFires() {
        $(InputTextElement.class).id(PopoverOwnerShortcutView.FIELD_ID).focus();
        $(InputTextElement.class).id(PopoverOwnerShortcutView.FIELD_ID)
                .sendKeys(Keys.ENTER);

        waitUntil(driver -> eventLog.findElements(By.tagName("div")).stream()
                .anyMatch(e -> e.getText()
                        .contains(PopoverOwnerShortcutView.SAVED)));
    }
}
