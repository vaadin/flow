/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.scroll;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

public class CustomScrollCallbacksIT extends AbstractScrollIT {
    @Test
    @Ignore("Ignored because of fusion issue: https://github.com/vaadin/flow/issues/7584")
    public void customCallbacks_customResults() throws InterruptedException {
        open();
        assertView("null");
        assertLog("");

        // Scroll to bottom
        scrollBy(0, 2000);

        int bottom = getScrollY();

        findElement(By.id("navigate")).click();

        assertView("navigated");

        assertLog("[0,0]");
        /*
         * Scroll position should not be reset, but might have changed slightly
         * because of more log rows
         */
        checkPageScroll(0, bottom, 50);

        findElement(By.id("back")).click();

        assertView("null");
        assertLog("[0,0]\n[42,-" + bottom + "]");
        /*
         * Scroll position should not be reset, but might have changed slightly
         * because of more log rows
         */
        checkPageScroll(0, bottom, 50);
    }

    private void assertView(String expected) {
        String text = findElement(By.id("view")).getText();
        Assert.assertEquals("Current view: " + expected, text);
    }

    private void assertLog(String expected) {
        String text = findElement(By.id("log")).getText();
        Assert.assertEquals(expected, text);
    }
}
