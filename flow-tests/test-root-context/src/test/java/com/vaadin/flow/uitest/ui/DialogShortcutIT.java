package com.vaadin.flow.uitest.ui;

import java.util.concurrent.atomic.AtomicInteger;

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
    protected AtomicInteger dialogCounter = new AtomicInteger(-1);
    private int reusedDialogIndex = -1;

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

        pressShortcutKey(getDialogInput(reusedDialogIndex));
        // no shortcut in dialog -> ui still gets the shortcut
        validateLatestShortcutEvent(reusedDialogIndex,
                DialogShortcutView.UI_BUTTON);

        listenToShortcutOnDialog(reusedDialogIndex);

        pressShortcutKey(getDialogInput(reusedDialogIndex));
        validateLatestDialogShortcut(1, reusedDialogIndex);

        pressShortcutKey(uiLevelButton);
        validateLatestShortcutEvent(2, DialogShortcutView.UI_BUTTON);

        closeDialog(reusedDialogIndex);

        pressShortcutKey(uiLevelButton);
        validateLatestShortcutEvent(3, DialogShortcutView.UI_BUTTON);

        openReusedDialog();

        pressShortcutKey(getDialogInput(reusedDialogIndex));
        validateLatestDialogShortcut(4, reusedDialogIndex);
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

    protected int openReusedDialog() {
        findElement(By.id(DialogShortcutView.REUSABLE_DIALOG)).click();
        if (reusedDialogIndex == -1) {
            reusedDialogIndex = dialogCounter.incrementAndGet();
        }
        return reusedDialogIndex;
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
        final WebElement latestEvent = eventLog.findElements(By.tagName("div"))
                .get(indexFromTop);
        Assert.assertEquals("Invalid latest event",
                eventCounter + "-" + eventSourceId, latestEvent.getText());
    }

    protected void pressShortcutKey(TestBenchElement elementToFocus) {
        elementToFocus.focus();
        elementToFocus.sendKeys("x");
    }

    protected TestBenchElement getDialogInput(int dialogIndex) {
        return $(DivElement.class)
                .id(DialogShortcutView.CONTENT_ID + dialogIndex).$("input")
                .first();
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
