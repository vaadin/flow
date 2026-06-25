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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static java.time.temporal.ChronoUnit.SECONDS;

public class TransferProgressListenerIT extends AbstractStreamResourceIT {

    @Test
    public void downloadServletResource_listenersAdded_listenersInvoked()
            throws IOException {
        open();
        waitForStatus(TransferProgressListenerView.WHEN_START_ID,
                "File download whenStart status: started");
        waitForStatus(TransferProgressListenerView.ON_PROGRESS_ID,
                "File download onProgress status: 294/-1");
        waitForStatus(TransferProgressListenerView.ON_COMPLETE_ID,
                "File download whenComplete status: completed");
        waitForStatus(TransferProgressListenerView.ON_ERROR_ID,
                "File download onError status: error");
        waitForStatus(TransferProgressListenerView.ON_CALLBACK_ERROR_ID,
                "File download onError status: callback error");
    }

    private void waitForStatus(String id, String status) {
        waitUntil(driver -> {
            WebElement element = findElement(By.id(id));
            return element.getText().equals(status);
        });
    }
}
