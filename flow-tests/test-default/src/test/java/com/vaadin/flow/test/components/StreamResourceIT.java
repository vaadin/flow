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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(StreamResourceView.class)
public class StreamResourceIT extends AbstractStreamResourceIT {

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
        Assertions.assertEquals("", link.getDomAttribute("router-ignore"),
                "Anchor element should have router-ignore attribute");
        String url = link.getDomProperty("href");

        getDriver().manage().timeouts()
                .scriptTimeout(Duration.of(15, ChronoUnit.SECONDS));

        try (InputStream stream = download(url)) {
            Assertions.assertEquals("foo", String.join("",
                    IOUtils.readLines(stream, StandardCharsets.UTF_8)));
        }

        Assertions.assertEquals(filename, FilenameUtils.getName(url));
    }
}
