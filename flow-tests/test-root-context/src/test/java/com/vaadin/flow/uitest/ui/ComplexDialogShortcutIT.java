package com.vaadin.flow.uitest.ui;

import org.junit.Test;
import org.openqa.selenium.By;

public class ComplexDialogShortcutIT extends DialogShortcutIT {

    @Test
    public void dialogWithShortcutListenOnAndPreserveOnRefresh_refreshWhenDialogOpened_shortcutScopingWorks() {
        final int firstDialogIndex = openNewDialog();

        listenToShortcutOnDialog(firstDialogIndex);
        pressShortcutKey(getDialogInput(firstDialogIndex));
        // focus on dialog -> only dialog shortcut occurs
        validateLatestShortcutEvent(0,
                DialogShortcutView.DIALOG_ID + firstDialogIndex);

        init(); // need to reset elements too

        pressShortcutKey(getDialogInput(firstDialogIndex));
        // focus on dialog -> only dialog shortcut occurs
        validateLatestShortcutEvent(1, DialogShortcutView.DIALOG_ID + 0);
    }

    @Override
    protected int openReusedDialog() {
        int dialogId = super.openReusedDialog();
        waitForTransport(dialogId);
        return dialogId;
    }

    @Override
    protected int openNewDialog() {
        final int dialogId = super.openNewDialog();
        waitForTransport(dialogId);
        return dialogId;
    }

    private void waitForTransport(int expectedDialogCounter) {
        waitForElementPresent(By.id(
                ComplexDialogShortcutView.OVERLAY_ID + expectedDialogCounter));
        waitForElementPresent(By.id(
                ComplexDialogShortcutView.CONTENT_ID + expectedDialogCounter));
    }
}
