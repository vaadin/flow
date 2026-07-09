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

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static com.vaadin.flow.uitest.ui.DisabledImageDownloadHandlerView.IFRAME_PAYLOAD;
import static com.vaadin.flow.uitest.ui.DisabledImageDownloadHandlerView.IMAGE_PAYLOAD;

/**
 * Regression test for issue #22772: an
 * {@link com.vaadin.flow.component.html.Image} or
 * {@link com.vaadin.flow.component.html.IFrame} backed by a
 * {@link com.vaadin.flow.server.streams.DownloadHandler} must still serve its
 * content when the owning component is disabled.
 */
public class DisabledImageDownloadHandlerIT extends AbstractStreamResourceIT {

    @Test
    public void disabledImage_downloadHandlerStillServesContent()
            throws IOException {
        open();

        WebElement image = findElement(By.id("disabled-image"));
        String url = image.getAttribute("src");
        Assert.assertNotNull("Image must have an src attribute", url);
        Assert.assertFalse(
                "Image src must not be empty when the owner is disabled",
                url.isEmpty());

        try (InputStream stream = download(url)) {
            String content = IOUtils.toString(stream, StandardCharsets.UTF_8);
            Assert.assertEquals(IMAGE_PAYLOAD, content);
        }
    }

    @Test
    public void disabledIFrame_downloadHandlerStillServesContent()
            throws IOException {
        open();

        WebElement iframe = findElement(By.id("disabled-iframe"));
        String url = iframe.getAttribute("src");
        Assert.assertNotNull("IFrame must have an src attribute", url);
        Assert.assertFalse(
                "IFrame src must not be empty when the owner is disabled",
                url.isEmpty());

        try (InputStream stream = download(url)) {
            String content = IOUtils.toString(stream, StandardCharsets.UTF_8);
            Assert.assertEquals(IFRAME_PAYLOAD, content);
        }
    }
}
