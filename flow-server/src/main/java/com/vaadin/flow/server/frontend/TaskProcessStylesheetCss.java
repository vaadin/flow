/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.server.ExecutionFailedException;
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
            return;
        }

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

        // Get node_modules folder for resolving npm package imports
        File nodeModulesFolder = options.getNodeModulesFolder();

        // Inline @import statements for local files and node_modules
        String content = CssBundler.inlineImports(cssFile.getParentFile(),
                cssFile, null, nodeModulesFolder);

        // Minify the CSS
        content = CssBundler.minifyCss(content);

        // Write back to the same file
        Files.writeString(cssFile.toPath(), content);
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(TaskProcessStylesheetCss.class);
    }
}
