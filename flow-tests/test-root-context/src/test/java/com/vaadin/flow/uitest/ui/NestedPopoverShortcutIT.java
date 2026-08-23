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
 * Captures the expected behaviour for issue #24974: the parent overlay's Alt+S
 * shortcut must not fire while the nested popover has focus.
 * <p>
 * A negative assertion ("did not fire") needs a synchronization barrier because
 * the shortcut fires asynchronously via a client-server round-trip. The test
 * presses the shortcut in the nested overlay (must not fire) and then in the
 * parent overlay (must fire). Flow processes RPCs in order, so once the parent
 * press has been logged, an erroneous nested fire would already be present too;
 * asserting the shortcut fired exactly once detects the regression.
 */
public class NestedPopoverShortcutIT extends ChromeBrowserTest {

    private TestBenchElement eventLog;

    @Before
    public void init() {
        open();
        eventLog = $(DivElement.class)
                .id(NestedPopoverShortcutView.EVENT_LOG_ID);
        $(NativeButtonElement.class)
                .id(NestedPopoverShortcutView.OPEN_PARENT_BUTTON).click();
        $(NativeButtonElement.class)
                .id(NestedPopoverShortcutView.OPEN_NESTED_BUTTON).click();
    }

    @Test
    public void nestedPopoverFocused_parentShortcutFiresOnlyForParent() {
        // Press in the nested overlay — must NOT fire the parent shortcut.
        pressShortcut(NestedPopoverShortcutView.NESTED_INPUT_ID);

        // Hide the nested overlay and press from the parent layer — MUST fire.
        final DivElement nestedOverlay = $(DivElement.class)
                .id(NestedPopoverShortcutView.NESTED_OVERLAY_ID);
        nestedOverlay.getCommandExecutor()
                .executeScript("arguments[0].hidePopover();", nestedOverlay);
        pressShortcut(NestedPopoverShortcutView.PARENT_INPUT_ID);

        // Ordered barrier: a plain round-trip issued after both keydowns. Flow
        // serializes requests, so once the sync marker appears, both shortcut
        // RPCs (including an erroneous one from the nested press) have been
        // applied and the count is final.
        $(NativeButtonElement.class).id(NestedPopoverShortcutView.SYNC_BUTTON)
                .click();
        waitUntil(driver -> count(NestedPopoverShortcutView.SYNC) >= 1);

        Assert.assertEquals(
                "Parent shortcut must fire only for the parent-layer keydown, "
                        + "not for the keydown originating in the nested popover",
                1, count(NestedPopoverShortcutView.PARENT_SHORTCUT));
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
