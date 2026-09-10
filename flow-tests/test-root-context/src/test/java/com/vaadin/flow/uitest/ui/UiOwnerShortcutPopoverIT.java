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
 * A UI-owned shortcut must fire for keydowns on the top layer but not for
 * keydowns originating inside an open popover, which since #25624 is guarded
 * without a per-registration owner token.
 */
public class UiOwnerShortcutPopoverIT extends ChromeBrowserTest {

    private TestBenchElement eventLog;

    @Before
    public void init() {
        open();
        eventLog = $(DivElement.class)
                .id(UiOwnerShortcutPopoverView.EVENT_LOG_ID);
        $(NativeButtonElement.class).id(UiOwnerShortcutPopoverView.OPEN_BUTTON)
                .click();
    }

    @Test
    public void popoverOpen_uiShortcutFiresOnlyOutsideThePopover() {
        // Press inside the popover — must NOT fire the UI shortcut.
        pressShortcut(UiOwnerShortcutPopoverView.POPOVER_INPUT_ID);

        // Hide the popover and press on the top layer — MUST fire.
        final DivElement popover = $(DivElement.class)
                .id(UiOwnerShortcutPopoverView.POPOVER_ID);
        popover.getCommandExecutor()
                .executeScript("arguments[0].hidePopover();", popover);
        pressShortcut(UiOwnerShortcutPopoverView.OUTSIDE_INPUT_ID);

        // Ordered barrier: a plain round-trip issued after both keydowns. Flow
        // serializes requests, so once the sync marker appears, both shortcut
        // RPCs (including an erroneous one from the popover press) have been
        // applied and the count is final.
        $(NativeButtonElement.class).id(UiOwnerShortcutPopoverView.SYNC_BUTTON)
                .click();
        waitUntil(driver -> count(UiOwnerShortcutPopoverView.SYNC) >= 1);

        Assert.assertEquals(
                "UI shortcut must fire only for the top-layer keydown, not "
                        + "for the keydown originating inside the popover",
                1, count(UiOwnerShortcutPopoverView.UI_SHORTCUT));
    }

    private void pressShortcut(String inputId) {
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
