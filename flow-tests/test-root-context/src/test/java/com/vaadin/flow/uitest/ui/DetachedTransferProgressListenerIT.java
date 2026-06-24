/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import static com.vaadin.flow.uitest.ui.DetachedTransferProgressListenerView.DOWNLOAD_AND_REMOVE;
import static com.vaadin.flow.uitest.ui.DetachedTransferProgressListenerView.REMOVED_COMPONENT_DONE;

public class DetachedTransferProgressListenerIT
        extends AbstractStreamResourceIT {

    @Test
    public void downloadRemovesComponent_successfullyUpdatesUI()
            throws IOException {
        open();

        WebElement link = findElement(By.id(DOWNLOAD_AND_REMOVE));
        link.click();

        try {
            waitUntil(
                    driver -> isElementPresent(By.id(REMOVED_COMPONENT_DONE)));
        } catch (TimeoutException e) {
            Assert.fail("Success element never present.");
        }

    }
}
