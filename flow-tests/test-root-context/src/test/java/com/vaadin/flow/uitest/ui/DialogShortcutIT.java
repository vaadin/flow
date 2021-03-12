package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class DialogShortcutIT extends ChromeBrowserTest {

    private TestBenchElement eventLog;
    private TestBenchElement openDialogButton;
    private NativeButtonElement uiLevelButton;
    protected int dialogCounter = -1; // first one will get id 0

    @Before
    public void init() {
        open();
        eventLog = $(DivElement.class).id(DialogShortcutView.EVENT_LOG_ID);
        openDialogButton = $(NativeButtonElement.class)
                .id(DialogShortcutView.OPEN_BUTTON);
        uiLevelButton = $(NativeButtonElement.class)
                .id(DialogShortcutView.UI_BUTTON);
    }

    // #7799
    @Test
    public void dialogOpenedWithListenOnShortcut_sameShortcutListeningOnUi_focusDecidesWhichIsExecuted() {
        pressShortcutKey(
                uiLevelButton);
        validateLatestShortcutEvent(0, DialogShortcutView.UI_BUTTON);

        openNewDialog();
        pressShortcutKey(getFirstDialogInput());
        // no shortcut in dialog -> ui still gets the shortcut
        validateLatestShortcutEvent(1, DialogShortcutView.UI_BUTTON);

        listenToShortcutOnDialog(0);
        pressShortcutKey(getFirstDialogInput());
        // focus on dialog -> only dialog shortcut occurs
        validateLatestShortcutEvent(2, DialogShortcutView.DIALOG_ID + 0);

        // focus outside dialog -> ui level shortcut occurs
        pressShortcutKey(uiLevelButton);
        validateLatestShortcutEvent(3, DialogShortcutView.UI_BUTTON);
    }

    @Test
    public void dialogOpenedWithShortcutNoListenOn_sameShortcutListeningOnUi_bothExecuted() {
        pressShortcutKey(
                uiLevelButton);
        validateLatestShortcutEvent(0, DialogShortcutView.UI_BUTTON);

        openNewDialog();
        pressShortcutKey(getFirstDialogInput());
        // no shortcut in dialog -> ui still gets the shortcut
        validateLatestShortcutEvent(1, DialogShortcutView.UI_BUTTON);

        listenToShortcutOnUI(0);
        pressShortcutKey(getFirstDialogInput());
        // last even is on dialog
        validateLatestShortcutEvent(3, DialogShortcutView.UI_ID);
        validateShortcutEvent(1, 2, DialogShortcutView.UI_BUTTON);

        closeDialog(0);
        pressShortcutKey(
                $(NativeButtonElement.class).id(DialogShortcutView.UI_BUTTON));
        validateLatestShortcutEvent(4, DialogShortcutView.UI_BUTTON);
    }

    @Test
    public void dialogOpenedWithListenOnShortcut_dialogReopened_oldShortcutStillWorks() {
        openReusedDialog();

        pressShortcutKey(getFirstDialogInput());
        // no shortcut in dialog -> ui still gets the shortcut
        validateLatestShortcutEvent(0, DialogShortcutView.UI_BUTTON);

        listenToShortcutOnDialog(0);

        pressShortcutKey(getFirstDialogInput());
        validateLatestShortcutEvent(1, DialogShortcutView.DIALOG_ID + 0);

        pressShortcutKey(uiLevelButton);
        validateLatestShortcutEvent(2, DialogShortcutView.UI_BUTTON);

        closeDialog(0);

        pressShortcutKey(uiLevelButton);
        validateLatestShortcutEvent(3, DialogShortcutView.UI_BUTTON);

        openReusedDialog();

        pressShortcutKey(getFirstDialogInput());
        validateLatestShortcutEvent(4, DialogShortcutView.DIALOG_ID + 0);
    }

    // vaadin/vaadin-dialog#229
    @Test
    public void twoDialogsOpenedWithSameShortcutKeyOnListenOn_dialogWithFocusExecuted() {
        openNewDialog();
        listenToShortcutOnDialog(0);
        openNewDialog();
        listenToShortcutOnDialog(1);

        pressShortcutKey(
                getFirstDialogInput());
        validateLatestShortcutEvent(0, DialogShortcutView.DIALOG_ID + 0);

        pressShortcutKey(
                getDialogInput(1));
        validateLatestShortcutEvent(1, DialogShortcutView.DIALOG_ID + 1);

        pressShortcutKey(
                getFirstDialogInput());
        validateLatestShortcutEvent(2, DialogShortcutView.DIALOG_ID + 0);

        pressShortcutKey(uiLevelButton);
        validateLatestShortcutEvent(3, DialogShortcutView.UI_BUTTON);
    }
    protected void openReusedDialog() {
        findElement(By.id(DialogShortcutView.REUSABLE_DIALOG)).click();
        dialogCounter++;
    }

    protected void openNewDialog() {
        openDialogButton.click();
        dialogCounter++;
    }

    private void closeDialog(int dialogIndex) {
        $(NativeButtonElement.class)
                .id(DialogShortcutView.DIALOG_CLOSE_BUTTON + dialogIndex)
                .click();
    }

    protected void validateLatestShortcutEvent(int eventCounter,
            String eventSourceId) {
        validateShortcutEvent(0, eventCounter, eventSourceId);
    }

    private void validateShortcutEvent(int indexFromTop, int eventCounter,
            String eventSourceId) {
        final WebElement latestEvent = eventLog.findElements(By.tagName("div"))
                .get(indexFromTop);
        Assert.assertEquals("Invalid latest event",
                eventCounter + "-" + eventSourceId, latestEvent.getText());
    }

    protected void pressShortcutKey(TestBenchElement elementToFocus) {
        elementToFocus.focus();
        elementToFocus.sendKeys("x");
    }

    protected TestBenchElement getFirstDialogInput() {
        return getDialogInput(0);
    }

    private TestBenchElement getDialogInput(int dialogIndex) {
        return $(DivElement.class).id(DialogShortcutView.CONTENT_ID + dialogIndex)
                .$("input").first();
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
