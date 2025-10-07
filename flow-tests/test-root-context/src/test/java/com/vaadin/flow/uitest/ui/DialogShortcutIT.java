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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.ui.DialogShortcutView.REUSABLE_DIALOG_ID;

public class DialogShortcutIT extends ChromeBrowserTest {

    private TestBenchElement eventLog;
    private TestBenchElement openDialogButton;
    private NativeButtonElement uiLevelButton;
    protected AtomicInteger dialogCounter;

    @Before
    public void init() {
        open();
        eventLog = $(DivElement.class).id(DialogShortcutView.EVENT_LOG_ID);
        openDialogButton = $(NativeButtonElement.class)
                .id(DialogShortcutView.OPEN_BUTTON);
        uiLevelButton = $(NativeButtonElement.class)
                .id(DialogShortcutView.UI_BUTTON);
        dialogCounter = new AtomicInteger(-1);
    }

    // #7799
    @Test
    public void dialogOpenedWithListenOnShortcut_sameShortcutListeningOnUi_focusDecidesWhichIsExecuted() {
        pressShortcutKey(uiLevelButton);
        validateLatestShortcutEvent(0, DialogShortcutView.UI_BUTTON);

        final int firstDialogIndex = openNewDialog();
        pressShortcutKey(getDialogInput(firstDialogIndex));
        // no shortcut in dialog -> ui still gets the shortcut
        validateLatestShortcutEvent(1, DialogShortcutView.UI_BUTTON);

        listenToShortcutOnDialog(firstDialogIndex);
        pressShortcutKey(getDialogInput(firstDialogIndex));
        // focus on dialog -> only dialog shortcut occurs
        validateLatestDialogShortcut(2, firstDialogIndex);

        // focus outside dialog -> ui level shortcut occurs
        pressShortcutKey(uiLevelButton);
        validateLatestShortcutEvent(3, DialogShortcutView.UI_BUTTON);
    }

    @Test
    public void dialogOpenedWithShortcutNoListenOn_sameShortcutListeningOnUi_bothExecuted() {
        pressShortcutKey(uiLevelButton);
        validateLatestShortcutEvent(0, DialogShortcutView.UI_BUTTON);

        final int dialogIndex = openNewDialog();
        pressShortcutKey(getDialogInput(dialogIndex));
        // no shortcut in dialog -> ui still gets the shortcut
        validateLatestShortcutEvent(1, DialogShortcutView.UI_BUTTON);

        listenToShortcutOnUI(dialogIndex);
        pressShortcutKey(getDialogInput(dialogIndex));
        // last even is on dialog
        validateLatestShortcutEvent(3, DialogShortcutView.UI_ID);
        validateShortcutEvent(1, 2, DialogShortcutView.UI_BUTTON);

        closeDialog(dialogIndex);
        pressShortcutKey(
                $(NativeButtonElement.class).id(DialogShortcutView.UI_BUTTON));
        validateLatestShortcutEvent(4, DialogShortcutView.UI_BUTTON);
    }

    @Test
    public void dialogOpenedWithListenOnShortcut_dialogReopened_oldShortcutStillWorks() {
        openReusedDialog();

        pressShortcutKey(getDialogInput(REUSABLE_DIALOG_ID));
        // no shortcut in dialog -> ui still gets the shortcut
        validateLatestShortcutEvent(0, DialogShortcutView.UI_BUTTON);

        listenToShortcutOnDialog(REUSABLE_DIALOG_ID);

        pressShortcutKey(getDialogInput(REUSABLE_DIALOG_ID));
        validateLatestDialogShortcut(1, REUSABLE_DIALOG_ID);

        pressShortcutKey(uiLevelButton);
        validateLatestShortcutEvent(2, DialogShortcutView.UI_BUTTON);

        closeDialog(REUSABLE_DIALOG_ID);

        pressShortcutKey(uiLevelButton);
        validateLatestShortcutEvent(3, DialogShortcutView.UI_BUTTON);

        openReusedDialog();

        pressShortcutKey(getDialogInput(REUSABLE_DIALOG_ID));
        validateLatestDialogShortcut(4, REUSABLE_DIALOG_ID);
    }

    // vaadin/vaadin-dialog#229
    @Test
    public void twoDialogsOpenedWithSameShortcutKeyOnListenOn_dialogWithFocusExecuted() {
        final int firstDialogIndex = openNewDialog();
        listenToShortcutOnDialog(firstDialogIndex);
        final int secondDialogIndex = openNewDialog();
        listenToShortcutOnDialog(secondDialogIndex);

        pressShortcutKey(getDialogInput(firstDialogIndex));
        validateLatestDialogShortcut(0, firstDialogIndex);

        pressShortcutKey(getDialogInput(secondDialogIndex));
        validateLatestDialogShortcut(1, secondDialogIndex);

        pressShortcutKey(getDialogInput(firstDialogIndex));
        validateLatestDialogShortcut(2, firstDialogIndex);

        pressShortcutKey(uiLevelButton);
        validateLatestShortcutEvent(3, DialogShortcutView.UI_BUTTON);
    }

    // #10362
    @Test
    public void shortcutAddedWithPreventDefault_inputFocused_enteringOtherKeysToInputWorks() {
        final int firstDialogIndex = openNewDialog();
        listenToShortcutOnDialog(firstDialogIndex);

        final InputTextElement dialogInput = getDialogInput(firstDialogIndex);
        pressShortcutKey(dialogInput);
        validateLatestDialogShortcut(0, firstDialogIndex);
        Assert.assertNotEquals(
                "Entered shortcut key should not be visible in input due to prevent default",
                DialogShortcutView.KEY_STRING, dialogInput.getValue());

        // use another key
        dialogInput.focus();
        dialogInput.sendKeys("fooxbar");
        // only x triggers event and value changes
        validateLatestDialogShortcut(1, firstDialogIndex);
        Assert.assertEquals("Entered value should be visible in input",
                "foobar", dialogInput.getValue());
    }

    // #10362
    @Test
    public void shortcutAddedWithAllowDefault_inputFocused_allKeysAcceptedToInput() {
        $(NativeButtonElement.class)
                .id(DialogShortcutView.ALLOW_BROWSER_DEFAULT_BUTTON).click();
        final int firstDialogIndex = openNewDialog();
        listenToShortcutOnDialog(firstDialogIndex);

        final InputTextElement dialogInput = getDialogInput(firstDialogIndex);
        pressShortcutKey(dialogInput);
        validateLatestDialogShortcut(0, firstDialogIndex);
        Assert.assertEquals(
                "Entered shortcut key should be visible in input due to allow default",
                DialogShortcutView.KEY_STRING, dialogInput.getValue());
        dialogInput.clear();

        dialogInput.focus();
        dialogInput.sendKeys("foo" + DialogShortcutView.KEY_STRING + "bar");
        // only x triggers event and value changes
        validateLatestDialogShortcut(1, firstDialogIndex);
        Assert.assertEquals("Entered value should be visible in input",
                "foo" + DialogShortcutView.KEY_STRING + "bar",
                dialogInput.getValue());
    }

    protected void openReusedDialog() {
        findElement(By.id(DialogShortcutView.REUSABLE_DIALOG_BUTTON)).click();
    }

    protected int openNewDialog() {
        openDialogButton.click();
        return dialogCounter.incrementAndGet();
    }

    private void closeDialog(int dialogIndex) {
        $(NativeButtonElement.class)
                .id(DialogShortcutView.DIALOG_CLOSE_BUTTON + dialogIndex)
                .click();
    }

    protected void validateLatestDialogShortcut(int eventCounter,
            int dialogId) {
        validateShortcutEvent(0, eventCounter,
                DialogShortcutView.DIALOG_ID + dialogId);
    }

    protected void validateLatestShortcutEvent(int eventCounter,
            String eventSourceId) {
        validateShortcutEvent(0, eventCounter, eventSourceId);
    }

    private void validateShortcutEvent(int indexFromTop, int eventCounter,
            String eventSourceId) {
        final WebElement latestEvent = waitUntil(driver -> eventLog.findElement(
                By.xpath(String.format("div[%d]", indexFromTop + 1))));
        Assert.assertEquals(
                "Invalid latest event with " + indexFromTop + ":" + ":"
                        + eventSourceId,
                eventCounter + "-" + eventSourceId, latestEvent.getText());
    }

    protected void pressShortcutKey(TestBenchElement elementToFocus) {
        elementToFocus.focus();
        elementToFocus.sendKeys("x");
    }

    protected InputTextElement getDialogInput(int dialogIndex) {
        return $(DivElement.class)
                .id(DialogShortcutView.CONTENT_ID + dialogIndex)
                .$(InputTextElement.class).first();
    }

    private void listenToShortcutOnUI(int dialogIndex) {
        $(NativeButtonElement.class)
                .id(DialogShortcutView.LISTEN_ON_UI_BUTTON + dialogIndex)
                .click();
    }

    protected void listenToShortcutOnDialog(int dialogIndex) {
        $(NativeButtonElement.class)
                .id(DialogShortcutView.LISTEN_ON_DIALOG_BUTTON + dialogIndex)
                .click();
    }

    private void listenToButtonShortcutOnUI(int dialogIndex) {
        $(NativeButtonElement.class)
                .id(DialogShortcutView.LISTEN_CLICK_ON_UI_BUTTON + dialogIndex)
                .click();
    }

    private void listenToButtonShortcutOnDialog(int dialogIndex) {
        $(NativeButtonElement.class).id(
                DialogShortcutView.LISTEN_CLICK_ON_DIALOG_BUTTON + dialogIndex)
                .click();
    }
}
