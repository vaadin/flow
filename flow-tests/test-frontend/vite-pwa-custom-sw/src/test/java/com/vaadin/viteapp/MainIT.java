/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.viteapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.testutil.ChromeDeviceTest;

import com.vaadin.flow.internal.JacksonUtils;
import tools.jackson.databind.node.ObjectNode;

public class MainIT extends ChromeDeviceTest {
    private static final Path SW_APP_TS_PATH = Path.of("src", "main",
            "frontend", "sw-app.ts");

    @Before
    public void init() {
        getDevTools().setCacheDisabled(true);
    }

    @Test
    public void openHomePage_changeServiceWorker_serviceWorkerIsUpdated()
            throws IOException {
        openAndReload("/");

        Assume.assumeFalse("Development mode feature", isProductionMode());

        checkLogsForErrors(msg -> !msg.contains("Failed to load"));

        var output = $("output").first();
        var paragraphs = output.findElements(By.tagName("p"));

        // Initial message from SW is not expected as the page is reloaded
        Assert.assertTrue("Expected no messages", paragraphs.isEmpty());

        var baseDir = new File(System.getProperty("user.dir", ".")).toPath();
        var swAppFile = baseDir.resolve(SW_APP_TS_PATH);
        var swAppContent = Files.readString(swAppFile);

        try {
            var swAppContentUpdated = swAppContent.replaceFirst("initial",
                    "updated");
            Files.writeString(swAppFile, swAppContentUpdated,
                    StandardCharsets.UTF_8);

            var messageSelector = By.cssSelector("output > p");
            waitForElementPresent(messageSelector);
            Assert.assertEquals("updated",
                    findElement(messageSelector).getText());
        } finally {
            Files.writeString(swAppFile, swAppContent, StandardCharsets.UTF_8);
        }
    }

    private void reloadPage() {
        executeScript("window.location.reload()");
        waitUntil(webDriver -> ((JavascriptExecutor) driver)
                .executeScript("return document.readyState")
                .equals("complete"));
    }

    private void openAndReload(String url) {
        getDriver().get(getRootURL() + url);
        waitForDevServer();
        waitForServiceWorkerReady();
        // Now that the SW is ready, reload the page
        // to fill the SW runtime cache with dev resources.
        reloadPage();
    }

    private static String readStringFromUrl(String url) throws IOException {
        return new String(readAllBytes(new URL(url).openStream()),
                StandardCharsets.UTF_8);
    }

    private static ObjectNode readJsonFromUrl(String url) throws IOException {
        return JacksonUtils.readTree(readStringFromUrl(url));
    }

    private static byte[] readAllBytes(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int count;
        byte[] data = new byte[1024];
        while ((count = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, count);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private boolean isProductionMode() throws IOException {
        ObjectNode stats = readJsonFromUrl(
                getRootURL() + "?v-r=init&location=");
        return ((ObjectNode) stats.get("appConfig")).get("productionMode").asBoolean();
    }

}
