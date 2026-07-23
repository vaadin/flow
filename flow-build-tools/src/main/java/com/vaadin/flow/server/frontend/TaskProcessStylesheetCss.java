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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.internal.CssBundler;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Process CSS files in META-INF/resources for production build.
 * <p>
 * This task scans for {@code @StyleSheet} annotations and processes only the
 * CSS files that are actually referenced. {@code @import} statements are
 * inlined and CSS content minified. Files are processed in-place in the build
 * output directory.
 * <p>
 * Only local CSS files (those starting with {@code ./}) are processed. External
 * URLs (http/https) referenced in {@code @StyleSheet} annotations are not
 * affected by this task.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
public class TaskProcessStylesheetCss implements FallibleCommand {

    private final Options options;

    /**
     * Creates a new task for processing stylesheet CSS files.
     *
     * @param options
     *            the task options
     */
    public TaskProcessStylesheetCss(Options options) {
        this.options = options;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (!options.isProductionMode()) {
            getLogger().debug("Skipping CSS processing in non-production mode");
            return;
        }

        File resourcesDir = options.getMetaInfResourcesDirectory();
        if (resourcesDir == null || !resourcesDir.exists()) {
            getLogger().debug(
                    "META-INF/resources directory not found, skipping CSS processing");
            return;
        }

        // Scan for @StyleSheet annotations and collect referenced CSS paths
        Set<String> referencedCssPaths = scanStyleSheetAnnotations();
        if (referencedCssPaths.isEmpty()) {
            getLogger().debug("No @StyleSheet annotations found");
        } else {
            getLogger().info("Found {} @StyleSheet reference(s)",
                    referencedCssPaths.size());

            for (String cssPath : referencedCssPaths) {
                File cssFile = resolveCssFile(resourcesDir, cssPath);
                if (cssFile != null && cssFile.exists()) {
                    try {
                        processCssFile(cssFile);
                    } catch (IOException e) {
                        getLogger().warn("Failed to process CSS file: {}",
                                cssFile.getName(), e);
                    }
                } else {
                    getLogger().debug(
                            "CSS file not found in META-INF/resources: {}",
                            cssPath);
                }
            }
        }

        // Pre-compress the processed CSS along with any other compressible
        // static resource, so the server can serve brotli/gzip variants the
        // same way it does for Vite-bundled assets. This runs on every
        // production build, independent of whether the frontend bundle is
        // rebuilt or reused.
        compressStaticResources(resourcesDir);
    }

    /**
     * Pre-compresses compressible static resources under the given directory
     * into {@code .br} and {@code .gz} siblings by running a small Node script
     * that relies solely on Node's built-in {@code zlib}. Node is already
     * required for the frontend build, so no extra dependency is needed.
     *
     * @param resourcesDir
     *            the META-INF/resources output directory to compress
     */
    private void compressStaticResources(File resourcesDir)
            throws ExecutionFailedException {
        if (options.getNpmFolder() == null) {
            getLogger().debug(
                    "Node environment not configured, skipping static resource compression");
            return;
        }
        File script = extractCompressionScript();
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
                throw new ExecutionFailedException(
                        "Compressing static resources failed with exit code "
                                + exitCode + ":\n" + output);
            }
        } catch (IOException e) {
            throw new ExecutionFailedException(
                    "Failed to compress static resources", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionFailedException(
                    "Interrupted while compressing static resources", e);
        } finally {
            script.delete();
        }
    }

    private File extractCompressionScript() throws ExecutionFailedException {
        try (InputStream in = getClass()
                .getResourceAsStream("compress-static-resources.mjs")) {
            if (in == null) {
                throw new ExecutionFailedException(
                        "Could not locate the static resource compression "
                                + "script on the classpath");
            }
            File script = File
                    .createTempFile("vaadin-compress-static-resources", ".mjs");
            script.deleteOnExit();
            Files.copy(in, script.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            return script;
        } catch (IOException e) {
            throw new ExecutionFailedException(
                    "Failed to prepare the static resource compression script",
                    e);
        }
    }

    /**
     * Scans the classpath for @StyleSheet annotations and collects the
     * referenced CSS file paths.
     *
     * @return set of CSS file paths referenced by @StyleSheet annotations
     */
    private Set<String> scanStyleSheetAnnotations() {
        Set<String> cssPaths = new HashSet<>();
        ClassFinder classFinder = options.getClassFinder();

        if (classFinder == null) {
            getLogger().debug("ClassFinder not available, skipping scan");
            return cssPaths;
        }

        try {
            Set<Class<?>> annotatedClasses = classFinder
                    .getAnnotatedClasses(StyleSheet.class);

            for (Class<?> clazz : annotatedClasses) {
                StyleSheet[] annotations = clazz
                        .getAnnotationsByType(StyleSheet.class);
                for (StyleSheet annotation : annotations) {
                    String value = annotation.value();
                    if (isLocalStylesheet(value)) {
                        cssPaths.add(normalizeStylesheetPath(value));
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warn("Error scanning for @StyleSheet annotations", e);
        }

        return cssPaths;
    }

    /**
     * Checks if the stylesheet path is a local file (not an external URL).
     *
     * @param path
     *            the stylesheet path from the annotation
     * @return true if it's a local file path
     */
    private boolean isLocalStylesheet(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String lower = path.toLowerCase();
        // External URLs are not processed
        return !lower.startsWith("http://") && !lower.startsWith("https://");
    }

    /**
     * Normalizes the stylesheet path by removing leading ./ if present.
     *
     * @param path
     *            the stylesheet path
     * @return normalized path
     */
    private String normalizeStylesheetPath(String path) {
        if (path.startsWith("./")) {
            return path.substring(2);
        }
        return path;
    }

    /**
     * Resolves a CSS path to a file in the META-INF/resources directory.
     *
     * @param resourcesDir
     *            the META-INF/resources directory
     * @param cssPath
     *            the CSS file path
     * @return the resolved file, or null if path is invalid
     */
    private File resolveCssFile(File resourcesDir, String cssPath) {
        if (cssPath == null || cssPath.isBlank()) {
            return null;
        }
        return new File(resourcesDir, cssPath);
    }

    private void processCssFile(File cssFile) throws IOException {
        getLogger().debug("Processing CSS file: {}", cssFile.getName());

        File nodeModulesFolder = options.getNodeModulesFolder();

        // Inline @import statements and rewrite relative url() references so
        // they remain correct after imports are inlined into the entry file.
        String content = CssBundler.inlineImportsForStaticResourcesRelative(
                cssFile.getParentFile(), cssFile, nodeModulesFolder);

        content = CssBundler.minifyCss(content);
        Files.writeString(cssFile.toPath(), content);
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(TaskProcessStylesheetCss.class);
    }
}
