/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.components;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(StreamResourceView.class)
public class StreamResourceIT extends AbstractDefaultIT {

    @BrowserTest
    public void getDynamicVaadinResource() throws IOException {
        open();

        assertDownloadedContent("link", "file%20name");
    }

    @BrowserTest
    public void getDynamicVaadinPlusResource() throws IOException {
        open();

        assertDownloadedContent("plus-link", "file%2B.jpg");
    }

    @BrowserTest
    public void detach_attachALink_getDynamicVaadinResource()
            throws IOException {
        open();

        findElement(By.id("detach-attach")).click();

        assertDownloadedContent("link", "file%20name");
    }

    private void assertDownloadedContent(String downloadId, String filename)
            throws IOException {
        WebElement link = findElement(By.id(downloadId));
        assertEquals("", link.getDomAttribute("router-ignore"),
                "Anchor element should have router-ignore attribute");
        String url = link.getDomProperty("href");

        getDriver().manage().timeouts()
                .scriptTimeout(Duration.of(15, ChronoUnit.SECONDS));

        try (InputStream stream = download(url)) {
            assertEquals("foo", String.join("",
                    IOUtils.readLines(stream, StandardCharsets.UTF_8)));
        }

        assertEquals(filename, FilenameUtils.getName(url));
    }

    /*
     * Stolen from stackexchange.
     *
     * It's not possible to use a straight way to download the link externally
     * since it will use another session and the link will be invalid in this
     * session. So either this pure client side way or external download with
     * cookies copy (which allows preserve the session) needs to be used.
     */
    private InputStream download(String url) {
        String script = "var url = arguments[0];"
                + "var callback = arguments[arguments.length - 1];"
                + "var xhr = new XMLHttpRequest();"
                + "xhr.open('GET', url, true);"
                + "xhr.responseType = \"arraybuffer\";" +
                // force the HTTP response, response-type header to be array
                // buffer
                "xhr.onload = function() {"
                + "  var arrayBuffer = xhr.response;"
                + "  var byteArray = new Uint8Array(arrayBuffer);"
                + "  callback(byteArray);" + "};" + "xhr.send();";
        Object response = ((JavascriptExecutor) getDriver())
                .executeAsyncScript(script, url);
        // Selenium returns an Array of Long, we need byte[]
        List<?> byteList = (ArrayList<?>) response;
        byte[] bytes = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            bytes[i] = ((Long) byteList.get(i)).byteValue();
        }
        return new ByteArrayInputStream(bytes);
    }
}
