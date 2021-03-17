package com.vaadin.flow.uitest.ui;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

@Ignore("Disabled due flaky behavior, see #10284")
public class ComplexDialogShortcutIT extends DialogShortcutIT {

    @Test
    public void dialogWithShortcutListenOnAndPreserveOnRefresh_refreshWhenDialogOpened_shortcutScopingWorks() {
        openNewDialog();

        listenToShortcutOnDialog(0);
        pressShortcutKey(getFirstDialogInput());
        // focus on dialog -> only dialog shortcut occurs
        validateLatestShortcutEvent(0, DialogShortcutView.DIALOG_ID + 0);

        init(); // need to reset elements too

        pressShortcutKey(getFirstDialogInput());
        // focus on dialog -> only dialog shortcut occurs
        validateLatestShortcutEvent(1, DialogShortcutView.DIALOG_ID + 0);
    }

    @Override
    protected void openReusedDialog() {
        super.openReusedDialog();
        waitForTransport(0);
    }

    @Override
    protected void openNewDialog() {
        super.openNewDialog();
        waitForTransport(dialogCounter);
    }

    private void waitForTransport(int expectedDialogCounter) {
        waitForElementPresent(By.id(
                ComplexDialogShortcutView.OVERLAY_ID + expectedDialogCounter));
        waitForElementPresent(By.id(
                ComplexDialogShortcutView.CONTENT_ID + expectedDialogCounter));
    }
}
