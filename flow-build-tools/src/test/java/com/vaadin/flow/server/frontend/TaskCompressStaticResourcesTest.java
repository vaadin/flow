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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the JDK gzip fallback used when Node is not available.
 */
class TaskCompressStaticResourcesTest {

    @TempDir
    File resourcesDir;

    private final TaskCompressStaticResources task = new TaskCompressStaticResources(
            Mockito.mock(Options.class));

    @Test
    void gzipStaticResources_largeCompressibleFile_getsGzipSibling()
            throws IOException {
        byte[] css = ("a{}".repeat(1000)).getBytes(StandardCharsets.UTF_8);
        Path css1 = new File(resourcesDir, "styles.css").toPath();
        Files.write(css1, css);

        task.gzipStaticResources(resourcesDir);

        Path gz = css1.resolveSibling("styles.css.gz");
        assertTrue(Files.exists(gz), "Expected a gzip sibling to be created");
        try (GZIPInputStream in = new GZIPInputStream(
                Files.newInputStream(gz))) {
            assertArrayEquals(css, in.readAllBytes(),
                    "Gzip sibling should decode to the original content");
        }
    }

    @Test
    void gzipStaticResources_smallFile_removesStaleCompressedSiblings()
            throws IOException {
        Path small = new File(resourcesDir, "tiny.css").toPath();
        Files.write(small, "a{}".getBytes(StandardCharsets.UTF_8));
        // Stale variants from a previous build where the file was larger.
        Path staleGz = small.resolveSibling("tiny.css.gz");
        Path staleBr = small.resolveSibling("tiny.css.br");
        Files.write(staleGz, new byte[] { 1, 2, 3 });
        Files.write(staleBr, new byte[] { 4, 5, 6 });

        task.gzipStaticResources(resourcesDir);

        assertFalse(Files.exists(staleGz),
                "Stale gzip sibling should be removed");
        assertFalse(Files.exists(staleBr),
                "Stale brotli sibling should be removed");
    }

    @Test
    void gzipStaticResources_nonCompressibleFile_isLeftAlone()
            throws IOException {
        byte[] data = new byte[2048];
        Path png = new File(resourcesDir, "image.png").toPath();
        Files.write(png, data);

        task.gzipStaticResources(resourcesDir);

        assertFalse(Files.exists(png.resolveSibling("image.png.gz")),
                "Non-compressible file should not be gzipped");
    }
}
