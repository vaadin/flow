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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import static com.vaadin.flow.internal.FrontendUtils.TAILWIND_CSS;

/**
 * Generate <code>tailwind.css</code> if it is missing in the generated frontend
 * folder.
 * <p>
 * If a <code>tailwind-custom.css</code> file exists in the frontend folder, it
 * will be imported into the generated file, allowing users to add custom
 * Tailwind CSS directives such as {@code @theme} blocks.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
public class TaskGenerateTailwindCss extends AbstractTaskClientGenerator {

    private static final String RELATIVE_SOURCE_PATH_MARKER = "#relativeSourcePath#";
    private static final String CUSTOM_IMPORT_MARKER = "/* #customImport# */";
    private static final String CSS_IMPORT_MARKER = "/* #cssImport# */";
    private static final String TAILWIND_CUSTOM_CSS = "tailwind-custom.css";
    private static final String STYLES_CSS = "styles.css";
    private static final Logger log = LoggerFactory
            .getLogger(TaskGenerateTailwindCss.class);

    private String relativeSourcePath;
    private String customImportReplacement;
    private String themeImportReplacement;

    private final File tailwindCss;

    /**
     * Create a task to generate <code>tailwind.css</code> integration file.
     *
     * @param options
     *            the task options
     */
    TaskGenerateTailwindCss(Options options) {

        tailwindCss = new File(options.getFrontendGeneratedFolder(),
                TAILWIND_CSS);
        relativeSourcePath = options.getFrontendGeneratedFolder().toPath()
                .relativize(options.getNpmFolder().toPath().resolve("src"))
                .toString();
        // Use forward slash as a separator
        relativeSourcePath = relativeSourcePath.replace(File.separator, "/");

        // Check if custom Tailwind CSS file exists
        File customCssFile = new File(options.getFrontendDirectory(),
                TAILWIND_CUSTOM_CSS);
        if (customCssFile.exists()) {
            String relativeCustomPath = options.getFrontendGeneratedFolder()
                    .toPath().relativize(customCssFile.toPath()).toString();
            // Use forward slash as a separator
            relativeCustomPath = relativeCustomPath.replace(File.separator,
                    "/");
            customImportReplacement = "@import '" + relativeCustomPath + "';\n";
        } else {
            customImportReplacement = "";
        }

        // Import theme CSS files and @CssImport files to enable @apply
        // directive processing
        themeImportReplacement = buildCssImports(options);
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream indexStream = getClass()
                .getResourceAsStream(TAILWIND_CSS)) {
            var template = StringUtil.toUTF8String(indexStream);
            template = template.replace(RELATIVE_SOURCE_PATH_MARKER,
                    relativeSourcePath);
            template = template.replace(CUSTOM_IMPORT_MARKER,
                    customImportReplacement);
            template = template.replace(CSS_IMPORT_MARKER,
                    themeImportReplacement);
            return template;
        }
    }

    @Override
    protected File getGeneratedFile() {
        return tailwindCss;
    }

    @Override
    protected boolean shouldGenerate() {
        return true;
    }

    private String buildCssImports(Options options) {
        StringBuilder imports = new StringBuilder();

        addThemeImportIfAvailable(options, imports);

        addCssImports(options, imports);

        return imports.toString();
    }

    /**
     * Add import to theme style.css if a legacy style theme is used.
     * 
     * @param options
     *            the task options
     * @param imports
     *            the imports string builder
     */
    private static void addThemeImportIfAvailable(Options options,
            StringBuilder imports) {

        FrontendDependenciesScanner frontendDependenciesScanner = options
                .getFrontendDependenciesScanner();
        String themeName = "";
        if (frontendDependenciesScanner != null) {
            if (frontendDependenciesScanner.getThemeDefinition() != null) {
                themeName = frontendDependenciesScanner.getThemeDefinition()
                        .getName();
            }
        }

        // Import theme's styles.css if theme exists
        if (themeName != null && !themeName.isEmpty()) {
            File themesFolder = new File(options.getFrontendDirectory(),
                    "themes");
            File themeFolder = new File(themesFolder, themeName);

            // Import styles.css if it exists
            File stylesCss = new File(themeFolder, STYLES_CSS);
            if (stylesCss.exists()) {
                String relativePath = options.getFrontendGeneratedFolder()
                        .toPath().relativize(stylesCss.toPath()).toString()
                        .replace(File.separator, "/");
                imports.append("@import '").append(relativePath).append("';\n");
            }
        }
    }

    /**
     * Add all found CssImport and StyleSheet with found files into imports.
     * 
     * @param options
     *            the task options
     * @param imports
     *            the imports string builder
     */
    private void addCssImports(Options options, StringBuilder imports) {

        Collection<String> cssImports = options.getFrontendDependenciesScanner()
                .getCss().values().stream().flatMap(List::stream)
                .map(CssData::getValue).collect(Collectors.toList());
        cssImports.addAll(collectStyleSheetAnnotations(options));

        // Import all @CssImport CSS files that exist in the frontend directory
        if (cssImports != null) {
            Path frontendFolder = options.getFrontendGeneratedFolder()
                    .getParentFile().toPath();
            for (String cssPath : cssImports) {
                if (cssPath != null && !cssPath.isEmpty()) {
                    String cssFile = resolveCssFile(options, cssPath);
                    if (cssFile != null) {
                        imports.append("@import '").append(cssFile)
                                .append("';\n");
                    }
                }
            }
        }
    }

    private String resolveCssFile(Options options, String cssPath) {
        // Handle Frontend/ alias
        if (cssPath.startsWith("Frontend/")) {
            cssPath = cssPath.substring("Frontend/".length());
        }
        // Handle ./ prefix
        if (cssPath.startsWith("./")) {
            cssPath = cssPath.substring(2);
        }

        Path frontendFolder = options.getFrontendGeneratedFolder()
                .getParentFile().toPath();

        // Try frontend directory first
        File cssFile = new File(options.getFrontendDirectory(), cssPath);
        if (cssFile.exists()) {
            String relativePath = frontendFolder.relativize(cssFile.toPath())
                    .toString().replace(File.separator, "/");
            return "Frontend/" + relativePath;
        }
        // Try jar resources folder
        if (options.getJarFrontendResourcesFolder() != null) {
            cssFile = new File(options.getJarFrontendResourcesFolder(),
                    cssPath);
            if (cssFile.exists()) {
                String relativePath = frontendFolder
                        .relativize(cssFile.toPath()).toString()
                        .replace(File.separator, "/");
                return "Frontend/" + relativePath;
            }
        }
        // Try resources directory
        File resourcesFolder = options.getJavaResourceFolder();
        if (resourcesFolder == null) {
            resourcesFolder = options.getNpmFolder().toPath()
                    .resolve("src/main/resources").toFile();
        }
        if (resourcesFolder != null && resourcesFolder.exists()) {
            cssFile = new File(resourcesFolder,
                    "META-INF/resources/" + cssPath);
            if (cssFile.exists()) {
                return options.getFrontendGeneratedFolder().toPath()
                        .relativize(cssFile.toPath()).toString()
                        .replace(File.separator, "/");
            }
        }

        return null;
    }

    /**
     * Scans the classpath for @StyleSheet annotations and collects the
     * referenced CSS file paths.
     *
     * @return set of CSS file paths referenced by @StyleSheet annotations
     */
    private Set<String> collectStyleSheetAnnotations(Options options) {
        Set<String> cssPaths = new HashSet<>();
        ClassFinder classFinder = options.getClassFinder();

        if (classFinder == null) {
            log.debug("ClassFinder not available, skipping scan");
            return cssPaths;
        }

        try {
            for (Class<?> clazz : classFinder
                    .getAnnotatedClasses(StyleSheet.class)) {
                for (StyleSheet annotation : clazz
                        .getAnnotationsByType(StyleSheet.class)) {
                    String value = annotation.value();
                    if (isLocalStylesheet(value)) {
                        cssPaths.add(value);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error scanning for @StyleSheet annotations", e);
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
        // External URLs are ignored
        return !lower.startsWith("http://") && !lower.startsWith("https://");
    }

}
