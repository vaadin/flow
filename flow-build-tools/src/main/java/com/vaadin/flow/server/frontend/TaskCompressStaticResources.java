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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FrontendUtils;

/**
 * Pre-compresses static resources served straight from META-INF/resources (e.g.
 * CSS referenced with {@code @StyleSheet}, JavaScript, SVG) into brotli
 * ({@code .br}) and gzip ({@code .gz}) siblings, mirroring the compression Vite
 * applies to bundled assets. These resources never enter the Vite bundle, so
 * without this task they would always be served uncompressed.
 * <p>
 * Runs on every production build, independent of whether the frontend bundle is
 * rebuilt or reused, and after {@link TaskProcessStylesheetCss} so the already
 * minified and inlined CSS gets compressed.
 * <p>
 * The JDK cannot produce brotli, so brotli is delegated to a small Node script
 * relying solely on Node's built-in {@code zlib}. Node is only used when it is
 * already available, so it is never downloaded merely to compress resources;
 * when Node is not available the task still produces gzip variants using the
 * JDK. Both variants are pre-compressed so the server can serve whichever the
 * client accepts.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskCompressStaticResources implements FallibleCommand {

    private static final String COMPRESSION_SCRIPT = "compress-static-resources.mjs";

    /**
     * File types that benefit from text compression. Kept in sync with the
     * {@value #COMPRESSION_SCRIPT} Node script used for brotli.
     */
    private static final List<String> COMPRESSIBLE_EXTENSIONS = List.of(".css",
            ".js", ".mjs", ".cjs", ".json", ".map", ".svg", ".html", ".htm",
            ".xml", ".txt");

    /**
     * Below this size the compressed variant tends to be as large as, or larger
     * than, the original, so serving precompressed brings no benefit. Kept in
     * sync with the {@value #COMPRESSION_SCRIPT} Node script.
     */
    private static final long MIN_SIZE_BYTES = 1024;

    private final Options options;

    /**
     * Creates a new task for compressing static resources.
     *
     * @param options
     *            the task options
     */
    TaskCompressStaticResources(Options options) {
        this.options = options;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (!options.isProductionMode()) {
            getLogger().debug(
                    "Skipping static resource compression in non-production mode");
            return;
        }

        File resourcesDir = options.getMetaInfResourcesDirectory();
        if (resourcesDir == null || !resourcesDir.exists()) {
            getLogger().debug(
                    "META-INF/resources directory not found, skipping static resource compression");
            return;
        }

        // Compression is a best-effort optimization: the uncompressed resource
        // is always available, so a failure here is logged as a warning and
        // never fails the build.

        // Brotli requires Node, which the JDK cannot provide. Use Node only if
        // it is already available so it is never downloaded solely for
        // compression; otherwise fall back to gzip, which the JDK provides.
        String nodeExecutable = findExistingNode();
        if (nodeExecutable != null) {
            compressWithNode(nodeExecutable, resourcesDir);
        } else {
            getLogger().debug(
                    "Node is not available without installation, compressing static resources with gzip only");
            gzipStaticResources(resourcesDir);
        }
    }

    private String findExistingNode() {
        try {
            return FrontendTools.fromOptions(options)
                    .getExistingNodeExecutable();
        } catch (RuntimeException e) {
            getLogger().debug("Could not determine whether Node is available",
                    e);
            return null;
        }
    }

    /**
     * Produces brotli and gzip siblings by running the Node compression script.
     */
    private void compressWithNode(String nodeExecutable, File resourcesDir) {
        File script = extractCompressionScript();
        if (script == null) {
            return;
        }
        try {
            List<String> command = List.of(nodeExecutable,
                    script.getAbsolutePath(), resourcesDir.getAbsolutePath());

            ProcessBuilder builder = FrontendUtils
                    .createProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                getLogger().warn(
                        "Compressing static resources exited with code {}, "
                                + "resources will be served uncompressed:\n{}",
                        exitCode, output);
            }
        } catch (IOException e) {
            getLogger().warn(
                    "Failed to compress static resources, they will be served "
                            + "uncompressed",
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            getLogger().warn(
                    "Interrupted while compressing static resources, they will "
                            + "be served uncompressed",
                    e);
        } finally {
            script.delete();
        }
    }

    /**
     * Produces gzip siblings using only the JDK, for when Node is not available
     * without an installation.
     */
    void gzipStaticResources(File resourcesDir) {
        try {
            Files.walkFileTree(resourcesDir.toPath(),
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file,
                                BasicFileAttributes attrs) throws IOException {
                            gzipIfNeeded(file, attrs.size());
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLogger().warn(
                    "Failed to gzip static resources, they will be served "
                            + "uncompressed",
                    e);
        }
    }

    private void gzipIfNeeded(Path file, long size) throws IOException {
        String name = file.getFileName().toString().toLowerCase(Locale.ENGLISH);
        if (!isCompressible(name)) {
            return;
        }

        // Always delete br file if exists as we only remake the gzip
        Files.deleteIfExists(file.resolveSibling(file.getFileName() + ".br"));
        Path gz = file.resolveSibling(file.getFileName() + ".gz");
        if (size < MIN_SIZE_BYTES) {
            // Remove any variants left by an earlier build where this file was
            // still above the threshold, so no stale compressed variant is
            // served now that it is served uncompressed.
            Files.deleteIfExists(gz);
            return;
        }
        try (InputStream in = Files.newInputStream(file);
                OutputStream out = new GZIPOutputStream(
                        Files.newOutputStream(gz)) {
                    {
                        def.setLevel(Deflater.BEST_COMPRESSION);
                    }
                }) {
            in.transferTo(out);
        }
    }

    private static boolean isCompressible(String fileName) {
        return COMPRESSIBLE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private File extractCompressionScript() {
        try (InputStream in = getClass()
                .getResourceAsStream(COMPRESSION_SCRIPT)) {
            if (in == null) {
                getLogger().warn(
                        "Could not locate the static resource compression "
                                + "script on the classpath, static resources "
                                + "will be served uncompressed");
                return null;
            }
            File script = File
                    .createTempFile("vaadin-compress-static-resources", ".mjs");
            script.deleteOnExit();
            Files.copy(in, script.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            return script;
        } catch (IOException e) {
            getLogger().warn(
                    "Failed to prepare the static resource compression script, "
                            + "static resources will be served uncompressed",
                    e);
            return null;
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(TaskCompressStaticResources.class);
    }
}
