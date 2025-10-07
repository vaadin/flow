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

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ModalDialogIT extends ChromeBrowserTest {

    private TestBenchElement eventLog;
    private TestBenchElement modalStrictDialogButton;
    private TestBenchElement modalVisualDialogButton;
    private TestBenchElement modelessDialogButton;

    @Override
    protected void open(String... parameters) {
        super.open(parameters);
        eventLog = $(DivElement.class).id(ModalDialogView.EVENT_LOG);
        modalStrictDialogButton = $(NativeButtonElement.class)
                .id(ModalDialogView.OPEN_MODAL_STRICT_BUTTON);
        modalVisualDialogButton = $(NativeButtonElement.class)
                .id(ModalDialogView.OPEN_MODAL_VISUAL_BUTTON);
        modelessDialogButton = $(NativeButtonElement.class)
                .id(ModalDialogView.OPEN_MODELESS_BUTTON);
    }

    // #7799
    @Test
    public void modalStrictDialogOpened_sameShortcutsListeningOnUi_noShortcutTriggered() {
        open();

        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);

        openDialog(modalStrictDialogButton);
        pressShortcutKey(getDialogInput());
        // no event occurred
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);
        listenToButtonShortcutOnUI();
        pressShortcutKey(getDialogInput());
        // event occurred since when a shortcut is registered on UI, it is
        // listened on the topmost modal component instead.
        validateLatestShortcutEvent(1, ModalDialogView.DIALOG_BUTTON);

        closeDialog();
        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(2, ModalDialogView.UI_BUTTON);
    }

    @Test
    public void modalDialogOpenInitially_dialogClosed_shortcutsInViewTrigger() {
        open("open_dialog=modal");

        // shortcuts on view should not trigger while dialog is open
        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        Assert.assertTrue("No event should be logged",
                eventLog.$(DivElement.class).all().isEmpty());

        closeDialog();

        // shortcuts on view should trigger when dialog has been closed
        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);
    }

    @Test
    public void modalDialogOpened_sameShortcutListeningOnUiAndDialog_onlyDialogShortcutExecuted() {
        open();

        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);

        openDialog(modalStrictDialogButton);
        pressShortcutKey(getDialogInput());
        // no event occurred
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);

        listenToButtonShortcutOnDialog();
        pressShortcutKey(getDialogInput());
        validateLatestShortcutEvent(1, ModalDialogView.DIALOG_BUTTON);

        closeDialog();
        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(2, ModalDialogView.UI_BUTTON);
    }

    @Test
    public void modelessDialogOpened_sharesShortcutWithUI_bothExecuted() {
        open();

        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);

        openDialog(modelessDialogButton);
        listenToButtonShortcutOnUI();
        pressShortcutKey(getDialogInput());

        validateShortcutEvent(1, 1, ModalDialogView.UI_BUTTON);
        validateLatestShortcutEvent(2, ModalDialogView.DIALOG_BUTTON);

        closeDialog();
        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(3, ModalDialogView.UI_BUTTON);
    }

    @Test
    public void modalVisualDialogOpened_sharesShortcutWithUI_bothExecuted() {
        open();

        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);

        openDialog(modalVisualDialogButton);
        listenToButtonShortcutOnUI();
        pressShortcutKey(getDialogInput());

        validateShortcutEvent(1, 1, ModalDialogView.UI_BUTTON);
        validateLatestShortcutEvent(2, ModalDialogView.DIALOG_BUTTON);

        closeDialog();
        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(3, ModalDialogView.UI_BUTTON);
    }

    @Test
    public void modelessDialogOpened_sameShortcutListeningOnUiAndDialog_bothExecuted() {
        open();

        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);

        openDialog(modelessDialogButton);
        listenToButtonShortcutOnDialog();
        pressShortcutKey(getDialogInput());

        validateLatestShortcutEvent(1, ModalDialogView.DIALOG_BUTTON);
        pressShortcutKey(getDialogInput());
        validateLatestShortcutEvent(2, ModalDialogView.DIALOG_BUTTON);

        closeDialog();
        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(3, ModalDialogView.UI_BUTTON);
    }

    @Test
    public void modalVisualDialogOpened_sameShortcutListeningOnUiAndDialog_bothExecuted() {
        open();

        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);

        openDialog(modalVisualDialogButton);
        listenToButtonShortcutOnDialog();
        pressShortcutKey(getDialogInput());

        validateLatestShortcutEvent(1, ModalDialogView.DIALOG_BUTTON);
        pressShortcutKey(getDialogInput());
        validateLatestShortcutEvent(2, ModalDialogView.DIALOG_BUTTON);

        closeDialog();
        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(3, ModalDialogView.UI_BUTTON);
    }

    private void openDialog(WebElement modalDialogButton) {
        modalDialogButton.click();
    }

    private void closeDialog() {
        $(NativeButtonElement.class).id(ModalDialogView.DIALOG_CLOSE_BUTTON)
                .click();
    }

    private void validateLatestShortcutEvent(int eventCounter,
            String eventSourceId) {
        validateShortcutEvent(0, eventCounter, eventSourceId);
    }

    private void validateShortcutEvent(int indexFromTop, int eventCounter,
            String eventSourceId) {
        final WebElement latestEvent = waitUntil(driver -> eventLog.findElement(
                By.xpath(String.format("div[%d]", indexFromTop + 1))));
        Assert.assertEquals("Invalid latest event",
                eventCounter + "-" + eventSourceId, latestEvent.getText());
    }

    private void pressShortcutKey(TestBenchElement elementToFocus) {
        elementToFocus.focus();
        elementToFocus.sendKeys("x");
    }

    private TestBenchElement getDialogInput() {
        return $(DivElement.class).id(ModalDialogView.DIALOG).$("input")
                .first();
    }

    private void listenToButtonShortcutOnUI() {
        $(NativeButtonElement.class).id(ModalDialogView.LISTEN_ON_UI_BUTTON)
                .click();
    }

    private void listenToButtonShortcutOnDialog() {
        $(NativeButtonElement.class).id(ModalDialogView.LISTEN_ON_DIALOG_BUTTON)
                .click();
    }
}
