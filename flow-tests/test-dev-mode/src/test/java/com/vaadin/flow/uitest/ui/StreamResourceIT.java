/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class StreamResourceIT extends ChromeBrowserTest {


    @Test
    public void getDynamicVaadinResource() throws IOException {
        open();

        assertDownloadedContent("link", "file%20name");
    }


    @Test
    public void getDynamicVaadinPlusResource() throws IOException {
        open();

        assertDownloadedContent("plus-link", "file%2B.jpg");
    }


    @Test
    public void detact_attachALink_getDynamicVaadinResource()
            throws IOException {
        open();

        findElement(By.id("detach-attach")).click();

        assertDownloadedContent("link", "file%20name");
    }

    private void assertDownloadedContent(String downloadId, String filename)
            throws IOException {
        WebElement link = findElement(By.id(downloadId));
        Assert.assertEquals(
                "Anchor element should have router-ignore " + "attribute", "",
                link.getAttribute("router-ignore"));
        String url = link.getAttribute("href");

        getDriver().manage().timeouts().setScriptTimeout(15, TimeUnit.SECONDS);

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            String text = lines.stream().collect(Collectors.joining());
            Assert.assertEquals("foo", text);
        }

        Assert.assertEquals(filename, FilenameUtils.getName(url));
    }

    /*
     * Stolen from stackexchange.
     *
     * It's not possible to use a straight way to download the link externally
     * since it will use another session and the link will be invalid in this
     * session. So either this pure client side way or external download with
     * cookies copy (which allows preserve the session) needs to be used.
     */
    public InputStream download(String url) throws IOException {
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
        ArrayList<?> byteList = (ArrayList<?>) response;
        byte[] bytes = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            Long byt = (Long) byteList.get(i);
            bytes[i] = byt.byteValue();
        }
        return new ByteArrayInputStream(bytes);
    }

}
