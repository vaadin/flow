/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.net.URL;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.uitest.ui.UploadView.INPUT_ID;
import static com.vaadin.flow.uitest.ui.UploadView.REFRESH_ID;
import static com.vaadin.flow.uitest.ui.UploadView.RESULT_ID;
import static com.vaadin.flow.uitest.ui.UploadView.UPLOAD_ID;

public class UploadIT extends ChromeBrowserTest {

    @Test
    public void uploadToServer_uploadSucceeds() {
        Assume.assumeTrue("Ignoring upload for remote agent test",
                getLocalExecution().isPresent());
        open();

        Assert.assertEquals("--empty--",
                $(SpanElement.class).id(RESULT_ID).getText());

        $(InputTextElement.class).id(INPUT_ID);
        WebElement fileInput = driver
                .findElement(By.cssSelector("input[type=file]"));
        URL uploadFileResource = getClass().getResource("/upload.txt");
        Assert.assertNotNull(uploadFileResource);

        String uploadFile = uploadFileResource.getFile();
        if (FrontendUtils.isWindows() && uploadFile.startsWith("/")) {
            uploadFile = uploadFile.substring(1);
        }
        fileInput.sendKeys(uploadFile);

        $(NativeButtonElement.class).id(UPLOAD_ID).click();
        $(NativeButtonElement.class).id(REFRESH_ID).click();

        Assert.assertEquals("Test upload.",
                $(SpanElement.class).id(RESULT_ID).getText());
    }
}
