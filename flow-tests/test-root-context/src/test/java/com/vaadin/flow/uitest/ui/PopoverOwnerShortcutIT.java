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
 * popover must still fire for keydowns originating in that popover, while a
 * shortcut owned by the UI must not (#25624).
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

    @Test
    public void uiOwnedShortcut_firesOnlyOutsideThePopover() {
        // Press inside the popover — must NOT fire the UI-owned shortcut.
        pressUiShortcut(PopoverOwnerShortcutView.FIELD_ID);

        // Hide the popover and press on the top layer — MUST fire.
        final DivElement popover = $(DivElement.class)
                .id(PopoverOwnerShortcutView.POPOVER_ID);
        popover.getCommandExecutor()
                .executeScript("arguments[0].hidePopover();", popover);
        pressUiShortcut(PopoverOwnerShortcutView.OUTSIDE_FIELD_ID);

        // Ordered barrier: a plain round-trip issued after both keydowns. Flow
        // serializes requests, so once the sync marker appears, both shortcut
        // RPCs (including an erroneous one from the popover press) have been
        // applied and the count is final.
        $(NativeButtonElement.class).id(PopoverOwnerShortcutView.SYNC_BUTTON)
                .click();
        waitUntil(driver -> count(PopoverOwnerShortcutView.SYNC) >= 1);

        Assert.assertEquals(
                "UI-owned shortcut must fire only for the top-layer keydown, "
                        + "not for the keydown originating inside the popover",
                1, count(PopoverOwnerShortcutView.UI_SHORTCUT));
    }

    private void pressUiShortcut(String inputId) {
        final InputTextElement input = $(InputTextElement.class).id(inputId);
        input.focus();
        // Send the chord directly to the element so the keydown originates
        // inside that field, not on the document.
        input.sendKeys(Keys.chord(Keys.ALT, "s"));
    }

    private long count(String text) {
        return eventLog.findElements(By.tagName("div")).stream()
                .filter(e -> e.getText().contains(text)).count();
    }
}
