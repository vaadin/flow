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

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResourceContentHashTest {

    @TempDir
    Path tempDir;

    private VaadinService service;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(VaadinService.class);
    }

    private URL createTempResource(String content) throws Exception {
        Path file = tempDir.resolve("test.css");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toUri().toURL();
    }

    @Test
    void getContentHash_knownContent_returnsExpectedHash() throws Exception {
        byte[] content = "body { color: red; }"
                .getBytes(StandardCharsets.UTF_8);
        URL url = createTempResource("body { color: red; }");
        Mockito.when(service.resolveResource("styles.css"))
                .thenReturn("styles.css");
        Mockito.when(service.getStaticResource("/styles.css")).thenReturn(url);

        String hash = ResourceContentHash.getContentHash(service, "styles.css");

        assertNotNull(hash);
        assertEquals(8, hash.length());

        String expectedHash = MessageDigestUtil.sha256Hex(content).substring(0,
                8);
        assertEquals(expectedHash, hash);
    }

    @Test
    void getContentHash_externalHttpUrl_returnsNull() {
        assertNull(ResourceContentHash.getContentHash(service,
                "http://cdn.example.com/styles.css"));
    }

    @Test
    void getContentHash_externalHttpsUrl_returnsNull() {
        assertNull(ResourceContentHash.getContentHash(service,
                "https://cdn.example.com/styles.css"));
    }

    @Test
    void getContentHash_externalUrlMixedCase_returnsNull() {
        assertNull(ResourceContentHash.getContentHash(service,
                "HTTPS://cdn.example.com/styles.css"));
    }

    @Test
    void getContentHash_nullUrl_returnsNull() {
        assertNull(ResourceContentHash.getContentHash(service, null));
    }

    @Test
    void getContentHash_blankUrl_returnsNull() {
        assertNull(ResourceContentHash.getContentHash(service, "  "));
    }

    @Test
    void getContentHash_missingResource_returnsNull() {
        Mockito.when(service.resolveResource("missing.css"))
                .thenReturn("missing.css");
        Mockito.when(service.getStaticResource("/missing.css"))
                .thenReturn(null);

        assertNull(ResourceContentHash.getContentHash(service, "missing.css"));
    }

    @Test
    void getContentHash_barePath_normalizedWithSlashPrefix() throws Exception {
        URL url = createTempResource("body { color: blue; }");
        Mockito.when(service.resolveResource("bare.css"))
                .thenReturn("bare.css");
        Mockito.when(service.getStaticResource("/bare.css")).thenReturn(url);

        String hash = ResourceContentHash.getContentHash(service, "bare.css");
        assertNotNull(hash);
        assertEquals(8, hash.length());
    }

    @Test
    void getContentHash_cachedAfterFirstCall() throws Exception {
        URL url = createTempResource("body {}");
        Mockito.when(service.resolveResource("cached.css"))
                .thenReturn("cached.css");
        Mockito.when(service.getStaticResource("/cached.css")).thenReturn(url);

        String hash1 = ResourceContentHash.getContentHash(service,
                "cached.css");
        String hash2 = ResourceContentHash.getContentHash(service,
                "cached.css");

        assertEquals(hash1, hash2);
        // Resource URL should only be looked up once due to caching
        Mockito.verify(service, Mockito.times(1))
                .getStaticResource("/cached.css");
    }

}
