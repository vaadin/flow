/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_PREFIX_ALIAS;
import static com.vaadin.flow.shared.ApplicationConstants.FRONTEND_PROTOCOL_PREFIX;

/**
 * Common logic for generate import file JS content.
 *
 * @author Vaadin Ltd
 *
 */
abstract class AbstractUpdateImports implements Runnable {

    private static final String CSS_PREPARE = "function addCssBlock(block) {\n"
            + " const tpl = document.createElement('template');\n"
            + " tpl.innerHTML = block;\n"
            + " document.head.appendChild(tpl.content);\n" + "}";
    private static final String CSS_PRE = "import $css_%d from '%s';%n"
            + "addCssBlock(`";
    private static final String CSS_POST = "`);";
    private static final String CSS_BASIC_TPL = CSS_PRE
            + "<custom-style><style%s>${$css_%d}</style></custom-style>"
            + CSS_POST;
    private static final String CSS_MODULE_TPL = CSS_PRE
            + "<dom-module id=\"%s\"><template><style%s>${$css_%d}</style></template></dom-module>"
            + CSS_POST;
    private static final String CSS_THEME_FOR_TPL = CSS_PRE
            + "<dom-module id=\"flow_css_mod_%d\" theme-for=\"%s\"><template><style%s>${$css_%d}</style></template></dom-module>"
            + CSS_POST;

    private static final String IMPORT_TEMPLATE = "import '%s';";

    // Used to recognize and sort FRONTEND/ imports in the final
    // generated-flow-imports.js
    private static final Pattern FRONTEND_IMPORT_LINE = Pattern.compile(
            String.format(IMPORT_TEMPLATE, WEBPACK_PREFIX_ALIAS + "\\S*"));

    private final File frontendDir;

    private final File npmDir;

    private final File generatedDir;

    AbstractUpdateImports(File frontendDirectory, File npmDirectory,
            File generatedDirectory) {
        frontendDir = frontendDirectory;
        npmDir = npmDirectory;
        generatedDir = generatedDirectory;
    }

    @Override
    public void run() {
        List<String> lines = new ArrayList<>();

        lines.addAll(getThemeLines());
        lines.addAll(getCssLines());

        collectModules(lines);

        writeImportLines(lines);
    }

    protected abstract void writeImportLines(List<String> lines);

    /**
     * Get all ES6 modules needed for run the application. Modules that are
     * theme dependencies are guaranteed to precede other modules in the result.
     *
     * @return list of JS modules
     */
    protected abstract List<String> getModules();

    /**
     * Get all the JS files used by the application.
     *
     * @return the set of JS files
     */
    protected abstract Set<String> getScripts();

    /**
     * Get a resource from the classpath.
     *
     * @param name
     *            class literal
     * @return the resource
     */
    protected abstract URL getResource(String name);

    /**
     * Get the {@link ThemeDefinition} of the application.
     *
     * @return the theme definition
     */
    protected abstract ThemeDefinition getThemeDefinition();

    /**
     * Get the {@link AbstractTheme} instance used in the application.
     *
     * @return the theme instance
     */
    protected abstract AbstractTheme getTheme();

    /**
     * Get all the CSS files used by the application.
     *
     * @return the set of CSS files
     */
    protected abstract Set<CssData> getCss();

    /**
     * Get theme lines for the generated imports file content.
     *
     * @return theme related generated JS lines
     */
    protected abstract Collection<String> getThemeLines();

    /**
     * Get generated modules to import.
     *
     * @return generated modules
     */
    protected abstract Collection<String> getGeneratedModules();

    /**
     * Get logger for this instance.
     *
     * @return a logger
     */
    protected abstract Logger getLogger();

    List<String> resolveModules(Collection<String> modules,
            boolean isJsModule) {
        return modules.stream()
                .map(module -> resolveResource(module, isJsModule)).sorted()
                .collect(Collectors.toList());
    }

    protected Collection<String> getCssLines() {
        Set<CssData> css = getCss();
        if (css.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<String> lines = new ArrayList<>();
        addLines(lines, CSS_PREPARE);

        Set<String> cssNotFound = new HashSet<>();
        int i = 0;

        for (CssData cssData : css) {
            if (!addCssLines(lines, cssData, i)) {
                cssNotFound.add(cssData.getValue());
            }
            i++;
        }
        if (!cssNotFound.isEmpty()) {
            String prefix = String.format(
                    "Failed to find the following css files in the `node_modules` or `%s` directory tree:",
                    frontendDir.getPath());
            String suffix = String.format(
                    "Check that they exist or are installed. If you use a custom directory "
                            + "for your resource files instead of the default `frontend` folder "
                            + "then make sure it's correctly configured (e.g. set '%s' property)",
                    FrontendUtils.PARAM_FRONTEND_DIR);
            throw new IllegalStateException(
                    notFoundMessage(cssNotFound, prefix, suffix));
        }
        lines.add("");
        return lines;
    }

    protected void updateImportsFile(File importsFile, List<String> newContent)
            throws IOException {
        List<String> oldContent = importsFile.exists()
                ? FileUtils.readLines(importsFile, StandardCharsets.UTF_8)
                : null;

        if (newContent.equals(oldContent)) {
            if (getLogger().isInfoEnabled()) {
                getLogger().info("No js modules to update '{}' file",
                        importsFile);
            }
        } else {
            FileUtils.forceMkdir(importsFile.getParentFile());
            FileUtils.writeStringToFile(importsFile,
                    String.join("\n", newContent), StandardCharsets.UTF_8);
            if (getLogger().isInfoEnabled()) {
                getLogger().info("Updated {}", importsFile);
            }
        }
    }

    protected String resolveResource(String importPath, boolean isJsModule) {
        String resolved = importPath;
        if (!importPath.startsWith("@")) {

            if (importPath.startsWith(FRONTEND_PROTOCOL_PREFIX)) {
                resolved = importPath.replaceFirst(FRONTEND_PROTOCOL_PREFIX,
                        "./");
                if (isJsModule) {
                    // Remove this when all flow components annotated with
                    // @JsModule have the './' prefix instead of 'frontend://'
                    getLogger().warn(
                            "Do not use the '{}' protocol in '@JsModule', changing '{}' to '{}', please update your component.",
                            FRONTEND_PROTOCOL_PREFIX, importPath, resolved);
                }
            }

            // We only should check here those paths starting with './' when all
            // flow components
            // have the './' prefix
            String resource = resolved.replaceFirst("^\\./+", "");
            if (hasMetaInfResource(resource)) {
                if (!resolved.startsWith("./")) {
                    getLogger().warn(
                            "Use the './' prefix for files in JAR files: '{}', please update your component.",
                            importPath);
                }
                resolved = FLOW_NPM_PACKAGE_NAME + resource;
            }
        }
        return resolved;
    }

    protected void addLines(Collection<String> lines, String content) {
        lines.addAll(Arrays.asList(content.split("\\R")));
    }

    private void collectModules(List<String> lines) {
        Set<String> modules = new LinkedHashSet<>();
        modules.addAll(resolveModules(getModules(), true));
        modules.addAll(resolveModules(getScripts(), false));

        modules.addAll(getGeneratedModules());

        modules.removeIf(UrlUtil::isExternal);

        ArrayList<String> externals = new ArrayList<>();
        ArrayList<String> internals = new ArrayList<>();

        for (String module : getModuleLines(modules)) {
            if (FRONTEND_IMPORT_LINE.matcher(module).matches()) {
                internals.add(module);
            } else {
                externals.add(module);
            }
        }

        lines.addAll(externals);
        lines.addAll(internals);
    }

    private Set<String> getUniqueEs6ImportPaths(Collection<String> modules) {
        Set<String> npmNotFound = new HashSet<>();
        Set<String> resourceNotFound = new HashSet<>();
        Set<String> es6ImportPaths = new LinkedHashSet<>();
        AbstractTheme theme = getTheme();
        Set<String> visited = new HashSet<>();

        for (String originalModulePath : modules) {
            String translatedModulePath = originalModulePath;
            String localModulePath = null;
            if (theme != null
                    && translatedModulePath.contains(theme.getBaseUrl())) {
                translatedModulePath = theme.translateUrl(translatedModulePath);
                String themePath = theme.getThemeUrl();

                // (#5964) Allows:
                // - custom @Theme with files placed in /frontend
                // - customize an already themed component
                // @vaadin/vaadin-grid/theme/lumo/vaadin-grid.js ->
                // theme/lumo/vaadin-grid.js
                localModulePath = translatedModulePath
                        .replaceFirst("@.+" + themePath, themePath);
            }

            if (localModulePath != null
                    && frontendFileExists(localModulePath)) {
                es6ImportPaths.add(toValidBrowserImport(localModulePath));
            } else if (importedFileExists(translatedModulePath)) {
                es6ImportPaths.add(toValidBrowserImport(translatedModulePath));
            } else if (importedFileExists(originalModulePath)) {
                es6ImportPaths.add(toValidBrowserImport(originalModulePath));
            } else if (originalModulePath.startsWith("./")) {
                resourceNotFound.add(originalModulePath);
            } else {
                npmNotFound.add(originalModulePath);
                es6ImportPaths.add(originalModulePath);
            }

            if (theme != null) {
                handleImports(originalModulePath, theme, es6ImportPaths,
                        visited);
            }
        }

        if (!resourceNotFound.isEmpty()) {
            String prefix = "Failed to find the following files: ";
            String suffix = String.format("%n  Locations searched were:"
                    + "%n      - `%s` in this project"
                    + "%n      - `%s` in included JARs"
                    + "%n%n  Please, double check that those files exist. If you use a custom directory "
                    + "for your resource files instead of default "
                    + "`frontend` folder then make sure you it's correctly configured "
                    + "(e.g. set '%s' property)", frontendDir.getPath(),
                    Constants.RESOURCES_FRONTEND_DEFAULT,
                    FrontendUtils.PARAM_FRONTEND_DIR);
            throw new IllegalStateException(
                    notFoundMessage(resourceNotFound, prefix, suffix));
        }

        if (!npmNotFound.isEmpty() && getLogger().isInfoEnabled()) {
            getLogger().info(notFoundMessage(npmNotFound,
                    "Failed to find the following imports in the `node_modules` tree:",
                    "If the build fails, check that npm packages are installed.\n\n"
                            + "  To fix the build remove `node_modules` directory to reset modules.\n"
                            + "  In addition you may run `npm install` to fix `node_modules` tree structure."));
        }

        return es6ImportPaths;
    }

    private Collection<String> getModuleLines(Set<String> modules) {
        return getUniqueEs6ImportPaths(modules).stream()
                .map(path -> String.format(IMPORT_TEMPLATE, path))
                .collect(Collectors.toList());
    }

    private boolean frontendFileExists(String jsImport) {
        File file = getFile(frontendDir, jsImport);
        return file.exists();
    }

    private boolean importedFileExists(String importName) {
        File file = getImportedFrontendFile(importName);
        if (file != null) {
            return true;
        }

        // full path import e.g
        // /node_modules/@vaadin/vaadin-grid/vaadin-grid-column.js
        boolean found = isFile(getNodeModulesDir(), importName);
        if (importName.toLowerCase().endsWith(".css")) {
            return found;
        }
        // omitted the .js extension e.g.
        // /node_modules/@vaadin/vaadin-grid/vaadin-grid-column
        found = found || isFile(getNodeModulesDir(), importName + ".js");
        // has a package.json file e.g. /node_modules/package-name/package.json
        found = found || isFile(getNodeModulesDir(), importName, PACKAGE_JSON);
        // file was generated by flow
        found = found || isFile(generatedDir,
                generatedResourcePathIntoRelativePath(importName));

        return found;
    }

    /**
     * Returns a file for the {@code jsImport} path ONLY if it's either in the
     * {@code "frontend"} folder or
     * {@code "node_modules/@vaadin/flow-frontend/"} folder.
     *
     * <p>
     * This method doesn't care about "published" WC paths (like
     * "@vaadin/vaadin-grid" and so on). See the
     * {@link #importedFileExists(String)} method implementation.
     *
     * @return a file on FS if it exists and it's inside a frontend folder or in
     *         node_modules/@vaadin/flow-frontend/, otherwise returns
     *         {@code null}
     */
    private File getImportedFrontendFile(String jsImport) {
        // file is in /frontend
        File file = getFile(frontendDir, jsImport);
        if (file.exists()) {
            return file;
        }
        // file is a flow resource e.g.
        // /node_modules/@vaadin/flow-frontend/gridConnector.js
        file = getFile(new File(getNodeModulesDir(), NODE_MODULES),
                FLOW_NPM_PACKAGE_NAME, jsImport);
        return file.exists() ? file : null;
    }

    private File getNodeModulesDir() {
        return new File(npmDir, NODE_MODULES);
    }

    private File getFile(File base, String... path) {
        return new File(base, String.join("/", path));
    }

    private boolean isFile(File base, String... path) {
        return getFile(base, path).isFile();
    }

    private boolean addCssLines(Collection<String> lines, CssData cssData,
            int i) {
        String cssFile = resolveResource(cssData.getValue(), false);
        boolean found = importedFileExists(cssFile);
        String cssImport = toValidBrowserImport(cssFile);
        String include = cssData.getInclude() != null
                ? " include=\"" + cssData.getInclude() + "\""
                : "";

        if (cssData.getThemefor() != null) {
            addLines(lines, String.format(CSS_THEME_FOR_TPL, i, cssImport, i,
                    cssData.getThemefor(), include, i));
        } else if (cssData.getId() != null) {
            addLines(lines, String.format(CSS_MODULE_TPL, i, cssImport,
                    cssData.getId(), include, i));
        } else {
            addLines(lines,
                    String.format(CSS_BASIC_TPL, i, cssImport, include, i));
        }
        return found;
    }

    private String notFoundMessage(Set<String> files, String prefix,
            String suffix) {
        return String.format("%n%n  %s%n      - %s%n  %s%n%n", prefix,
                String.join("\n      - ", files), suffix);
    }

    private boolean hasMetaInfResource(String resource) {
        return getResource(RESOURCES_FRONTEND_DEFAULT + "/" + resource) != null
                || getResource(COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT + "/"
                        + resource) != null;
    }

    private String toValidBrowserImport(String jsImport) {
        if (jsImport.startsWith(NodeUpdater.GENERATED_PREFIX)) {
            return generatedResourcePathIntoRelativePath(jsImport);
        } else if (isFile(frontendDir, jsImport)) {
            if (!jsImport.startsWith("./")) {
                getLogger().warn(
                        "Use the './' prefix for files in the '{}' folder: '{}', please update your annotations.",
                        frontendDir, jsImport);
            }
            return WEBPACK_PREFIX_ALIAS + jsImport.replaceFirst("^\\./", "");
        }
        return jsImport;
    }

    private void visitImportsRecursively(Path filePath, String path,
            AbstractTheme theme, Collection<String> imports,
            Set<String> visitedImports) throws IOException {
        String content = Files.readAllLines(filePath, StandardCharsets.UTF_8)
                .stream().collect(Collectors.joining("\n"));
        ImportExtractor extractor = new ImportExtractor(content);
        List<String> importedPaths = extractor.getImportedPaths();
        for (String importedPath : importedPaths) {
            // try to resolve path relatively to original filePath (inside user
            // frontend folder)
            String resolvedPath = resolve(importedPath, filePath, path);
            File file = getImportedFrontendFile(resolvedPath);
            if (file == null && !importedPath.startsWith("./")) {
                // In case such file doesn't exist it may be external: inside
                // node_modules folder
                file = getFile(getNodeModulesDir(), resolvedPath);
                resolvedPath = importedPath;
            }
            if (file == null) {
                // don't do anything if such file doesn't exist at all
                continue;
            }
            if (resolvedPath.contains(theme.getBaseUrl())) {
                String translatedPath = theme.translateUrl(resolvedPath);
                if (!visitedImports.contains(translatedPath)
                        && importedFileExists(translatedPath)) {
                    visitedImports.add(translatedPath);
                    imports.add(normalizeImportPath(translatedPath));
                }
            }
            handleImports(resolvedPath, theme, imports, visitedImports);
        }
    }

    private void handleImports(String path, AbstractTheme theme,
            Collection<String> imports, Set<String> visitedImports) {
        if (visitedImports.contains(path)) {
            return;
        }
        File file = getImportedFrontendFile(path);
        if (file == null) {
            return;
        }
        Path filePath = file.toPath();
        visitedImports.add(filePath.normalize().toString().replace("\\", "/"));
        try {
            visitImportsRecursively(filePath, path, theme, imports,
                    visitedImports);
        } catch (IOException exception) {
            getLogger().warn(
                    "Could not read file {}. Skipping "
                            + "applying theme for its imports",
                    file.getPath(), exception);
        }
    }

    /**
     * Resolves {@code importedPath} declared in the {@code moduleFile} whose
     * path (used in the app) is {@code path}.
     *
     * @param importedPath
     *            the path to resolve
     * @param moduleFile
     *            the path to file which contains the import
     * @param path
     *            the path which is used in the app for the {@code moduleFile}
     * @return resolved path to use in the application
     */
    private String resolve(String importedPath, Path moduleFile, String path) {
        String pathPrefix = moduleFile.toString();
        pathPrefix = pathPrefix.substring(0,
                pathPrefix.length() - path.length());
        String resolvedPath = moduleFile.getParent().resolve(importedPath)
                .toString();
        if (resolvedPath.startsWith(pathPrefix)) {
            resolvedPath = resolvedPath.substring(pathPrefix.length());
        }
        return resolvedPath;
    }

    private String normalizeImportPath(String path) {
        String importPath = toValidBrowserImport(path);
        File file = new File(importPath);
        return file.toPath().normalize().toString().replace("\\", "/");
    }

    private static String generatedResourcePathIntoRelativePath(String path) {
        return path.replace(NodeUpdater.GENERATED_PREFIX, "./");
    }

}
