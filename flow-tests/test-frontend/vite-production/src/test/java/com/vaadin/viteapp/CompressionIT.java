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
package com.vaadin.viteapp;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.brotli.dec.BrotliInputStream;
import org.junit.Assert;
import org.junit.Test;

public class CompressionIT {

    // Static CSS loaded via @StyleSheet from META-INF/resources, i.e. a
    // resource that never enters the Vite bundle. The production build must
    // still pre-compress it so the server can deliver a .br/.gz variant, the
    // same way it does for bundled assets.
    private static final String STATIC_STYLESHEET = "/styles/static-stylesheet.css";
    private static final String STATIC_STYLESHEET_MARKER = "static-stylesheet-marker";

    private String getRootURL() {
        return "http://localhost:8888";
    }

    @Test
    public void resourcesAvailableAsUncompressed() throws Exception {
        String bundleName = getJsBundleName();

        String file = IOUtils.toString(new URL(getRootURL() + bundleName),
                StandardCharsets.UTF_8);
        Assert.assertTrue(file.contains("generated-flow-imports"));
    }

    @Test
    public void resourcesAvailableAsBrotli() throws Exception {
        String bundleName = getJsBundleName();

        URL compressedUrl = new URL(getRootURL() + bundleName + ".br");
        BrotliInputStream stream = new BrotliInputStream(
                compressedUrl.openStream());

        String file = IOUtils.toString(stream, StandardCharsets.UTF_8);
        Assert.assertTrue(file.contains("generated-flow-imports"));
    }

    @Test
    public void staticResourceServedAsBrotli() throws Exception {
        HttpURLConnection connection = openStylesheet("br");
        try {
            Assert.assertEquals(200, connection.getResponseCode());
            Assert.assertEquals("br",
                    connection.getHeaderField("Content-Encoding"));
            String css = IOUtils.toString(
                    new BrotliInputStream(connection.getInputStream()),
                    StandardCharsets.UTF_8);
            Assert.assertTrue(
                    "Brotli-encoded static stylesheet should decode to the "
                            + "original CSS",
                    css.contains(STATIC_STYLESHEET_MARKER));
        } finally {
            connection.disconnect();
        }
    }

    @Test
    public void staticResourceServedAsGzip() throws Exception {
        HttpURLConnection connection = openStylesheet("gzip");
        try {
            Assert.assertEquals(200, connection.getResponseCode());
            Assert.assertEquals("gzip",
                    connection.getHeaderField("Content-Encoding"));
            String css = IOUtils.toString(
                    new GZIPInputStream(connection.getInputStream()),
                    StandardCharsets.UTF_8);
            Assert.assertTrue(
                    "Gzip-encoded static stylesheet should decode to the "
                            + "original CSS",
                    css.contains(STATIC_STYLESHEET_MARKER));
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Requests the static stylesheet the way a browser would, advertising the
     * given encoding, without transparently decoding the response so the served
     * format can be inspected.
     */
    private HttpURLConnection openStylesheet(String acceptEncoding)
            throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                getRootURL() + STATIC_STYLESHEET).openConnection();
        connection.setRequestProperty("Accept-Encoding", acceptEncoding);
        return connection;
    }

    private String getJsBundleName() throws Exception {
        String indexHtml = IOUtils.toString(
                new URL(getRootURL() + "/index.html"), StandardCharsets.UTF_8);
        Pattern p = Pattern.compile(".* src=\"./VAADIN/build/([^\"]*).*",
                Pattern.DOTALL);

        Matcher m = p.matcher(indexHtml);
        if (!m.matches()) {
            throw new IllegalStateException("No script found");
        }
        return "/VAADIN/build/" + m.group(1);
    }

}
