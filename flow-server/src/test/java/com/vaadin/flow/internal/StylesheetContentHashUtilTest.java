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
package com.vaadin.flow.internal;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.VaadinService;

public class StylesheetContentHashUtilTest {

    private VaadinService service;

    @Before
    public void setUp() {
        service = Mockito.mock(VaadinService.class);
    }

    @Test
    public void getContentHash_knownContent_returnsExpectedHash() {
        byte[] content = "body { color: red; }"
                .getBytes(StandardCharsets.UTF_8);
        Mockito.when(service.getResourceAsStream("styles.css"))
                .thenReturn(new ByteArrayInputStream(content));

        String hash = StylesheetContentHashUtil.getContentHash(service,
                "styles.css");

        Assert.assertNotNull(hash);
        Assert.assertEquals(8, hash.length());

        // Verify it matches the first 8 chars of SHA-256
        String expectedHash = MessageDigestUtil.sha256Hex(content).substring(0,
                8);
        Assert.assertEquals(expectedHash, hash);
    }

    @Test
    public void getContentHash_externalHttpUrl_returnsNull() {
        Assert.assertNull(StylesheetContentHashUtil.getContentHash(service,
                "http://cdn.example.com/styles.css"));
    }

    @Test
    public void getContentHash_externalHttpsUrl_returnsNull() {
        Assert.assertNull(StylesheetContentHashUtil.getContentHash(service,
                "https://cdn.example.com/styles.css"));
    }

    @Test
    public void getContentHash_externalUrlMixedCase_returnsNull() {
        Assert.assertNull(StylesheetContentHashUtil.getContentHash(service,
                "HTTPS://cdn.example.com/styles.css"));
    }

    @Test
    public void getContentHash_nullUrl_returnsNull() {
        Assert.assertNull(
                StylesheetContentHashUtil.getContentHash(service, null));
    }

    @Test
    public void getContentHash_blankUrl_returnsNull() {
        Assert.assertNull(
                StylesheetContentHashUtil.getContentHash(service, "  "));
    }

    @Test
    public void getContentHash_missingResource_returnsNull() {
        Mockito.when(service.getResourceAsStream("missing.css"))
                .thenReturn(null);
        Mockito.when(service.getResourceAsStream("/missing.css"))
                .thenReturn(null);

        Assert.assertNull(StylesheetContentHashUtil.getContentHash(service,
                "missing.css"));
    }

    @Test
    public void getContentHash_barePath_fallsBackToSlashPrefixed() {
        // Bare path "styles.css" may not resolve in servlet context.
        // The utility should fall back to "/styles.css".
        byte[] content = "body { color: blue; }"
                .getBytes(StandardCharsets.UTF_8);
        Mockito.when(service.getResourceAsStream("bare.css")).thenReturn(null);
        Mockito.when(service.getResourceAsStream("/bare.css"))
                .thenReturn(new ByteArrayInputStream(content));

        String hash = StylesheetContentHashUtil.getContentHash(service,
                "bare.css");
        Assert.assertNotNull(hash);
        Assert.assertEquals(8, hash.length());
    }

    @Test
    public void getContentHash_cachedAfterFirstCall() {
        byte[] content = "body {}".getBytes(StandardCharsets.UTF_8);
        Mockito.when(service.getResourceAsStream("cached.css"))
                .thenReturn(new ByteArrayInputStream(content));

        String hash1 = StylesheetContentHashUtil.getContentHash(service,
                "cached.css");
        String hash2 = StylesheetContentHashUtil.getContentHash(service,
                "cached.css");

        Assert.assertEquals(hash1, hash2);
        // Resource should only be read once due to caching
        Mockito.verify(service, Mockito.times(1))
                .getResourceAsStream("cached.css");
    }

    @Test
    public void appendHashToUrl_withHash_appendsQueryParam() {
        String result = StylesheetContentHashUtil.appendHashToUrl("/styles.css",
                "abcd1234");
        Assert.assertEquals("/styles.css?v=abcd1234", result);
    }

    @Test
    public void appendHashToUrl_urlWithExistingQueryParam_usesAmpersand() {
        String result = StylesheetContentHashUtil
                .appendHashToUrl("/styles.css?theme=dark", "abcd1234");
        Assert.assertEquals("/styles.css?theme=dark&v=abcd1234", result);
    }

    @Test
    public void appendHashToUrl_nullHash_returnsOriginalUrl() {
        String result = StylesheetContentHashUtil.appendHashToUrl("/styles.css",
                null);
        Assert.assertEquals("/styles.css", result);
    }
}
