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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

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
 * minified and inlined CSS gets compressed. Compression is delegated to a small
 * Node script relying solely on Node's built-in {@code zlib}, so no extra
 * dependency is needed (Node is already required for the frontend build).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskCompressStaticResources implements FallibleCommand {

    private static final String COMPRESSION_SCRIPT = "compress-static-resources.mjs";

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

        if (options.getNpmFolder() == null) {
            getLogger().debug(
                    "Node environment not configured, skipping static resource compression");
            return;
        }

        // Compression is a best-effort optimization: the uncompressed resource
        // is always available, so a failure here is logged as a warning and
        // never fails the build.
        File script = extractCompressionScript();
        if (script == null) {
            return;
        }
        try {
            FrontendTools tools = FrontendTools.fromOptions(options);
            List<String> command = List.of(tools.getNodeExecutable(),
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
