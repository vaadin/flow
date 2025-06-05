/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

public class StreamResourceIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/stream-resource";
    }

    @Test
    public void ensureStreamResourceWorks() throws IOException {
        open();

        WebElement link = $("a").id("download");
        String url = link.getAttribute("href");

        getDriver().manage().timeouts()
                .scriptTimeout(Duration.of(15, ChronoUnit.SECONDS));

        try (InputStream stream = download(url)) {
            List<String> lines = IOUtils.readLines(stream,
                    StandardCharsets.UTF_8);
            String text = lines.stream().collect(Collectors.joining());
            Assert.assertEquals("Hello world", text);
        }
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
