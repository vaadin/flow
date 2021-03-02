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

public class ModalDialogIT extends ChromeBrowserTest {

    private TestBenchElement eventLog;
    private TestBenchElement modalDialogButton;
    private TestBenchElement modelessDialogButton;

    @Before
    public void init() {
        open();
        eventLog = $(DivElement.class).id(ModalDialogView.EVENT_LOG);
        modalDialogButton = $(NativeButtonElement.class)
                .id(ModalDialogView.OPEN_MODAL_BUTTON);
        modelessDialogButton = $(NativeButtonElement.class)
                .id(ModalDialogView.OPEN_MODELESS_BUTTON);
    }

    // #7799
    @Test
    public void modalDialogOpened_sameShortcutsListeningOnUi_noShortcutTriggered() {
        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);

        openDialog(modalDialogButton);
        pressShortcutKey(getDialogInput());
        // no event occurred
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);
        listenToButtonShortcutOnUI();
        pressShortcutKey(getDialogInput());
        // no event occurred since shortcut is listened on ui which is inert
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);

        closeDialog();
        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(1, ModalDialogView.UI_BUTTON);
    }

    @Test
    public void modalDialogOpened_sameShortcutListeningOnUiAndDialog_onlyDialogShortcutExecuted() {
        pressShortcutKey(
                $(NativeButtonElement.class).id(ModalDialogView.UI_BUTTON));
        validateLatestShortcutEvent(0, ModalDialogView.UI_BUTTON);

        openDialog(modalDialogButton);
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
    public void modelessDialogOpened_sameShortcutListeningOnUiAndDialog_bothExecuted() {
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
        final WebElement latestEvent = eventLog.findElements(By.tagName("div"))
                .get(indexFromTop);
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
