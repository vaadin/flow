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

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_PREFIX_ALIAS;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * An updater that it's run when the servlet context is initialised in dev-mode
 * or when flow-maven-plugin goals are run in order to update Flow imports file
 * and <code>node_module/@vaadin/flow-frontend</code> contents by visiting all
 * classes with {@link JsModule} {@link HtmlImport} and {@link Theme}
 * annotations.
 */
public class TaskUpdateImports extends NodeUpdater {

    private final File generatedFlowImports;
    private final File frontendDirectory;
    private static final String IMPORT = "import '%s';";

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param finder
     *            a reusable class finder
     * @param frontendDependencies
     *            a reusable frontend dependencies
     * @param npmFolder
     *            folder with the `package.json` file
     * @param generatedPath
     *            folder where flow generated files will be placed.
     * @param frontendDirectory
     *            a directory with project's frontend files
     */
    TaskUpdateImports(ClassFinder finder,
            FrontendDependencies frontendDependencies, File npmFolder,
            File generatedPath, File frontendDirectory) {
        super(finder, frontendDependencies, npmFolder, generatedPath);
        this.frontendDirectory = frontendDirectory;
        this.generatedFlowImports = new File(generatedPath, IMPORTS_NAME);
    }

    @Override
    public void execute() {
        Set<String> modules = new HashSet<>();
        modules.addAll(resolveModules(frontDeps.getModules(), true));
        modules.addAll(resolveModules(frontDeps.getScripts(), false));

        modules.addAll(getGeneratedModules(generatedFolder,
                Collections.singleton(generatedFlowImports.getName())));

        modules = sortModules(modules);
        try {
            updateMainJsFile(getMainJsContent(modules));
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("Failed to update the Flow imports file '%s'",
                            generatedFlowImports),
                    e);
        }
    }

    private Set<String> sortModules(Set<String> modules) {
        return modules.stream().sorted(Comparator.reverseOrder())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> getMainJsContent(Set<String> modules) {
        List<String> lines = new ArrayList<>();
        AbstractTheme theme = frontDeps.getTheme();
        ThemeDefinition themeDef = frontDeps.getThemeDefinition();
        if (theme != null) {
            if (!theme.getHeaderInlineContents().isEmpty()) {
                lines.add("const div = document.createElement('div');");
                theme.getHeaderInlineContents().forEach(html -> {
                    lines.add("div.innerHTML = '"
                            + html.replaceAll("(?m)(^\\s+|\\s?\n)", "") + "';");
                    lines.add(
                            "document.head.insertBefore(div.firstElementChild, document.head.firstChild);");
                });
            }
            theme.getHtmlAttributes(themeDef.getVariant()).forEach(
                    (key, value) -> lines.add("document.body.setAttribute('"
                            + key + "', '" + value + "');"));
        }

        Collection<String> imports = new ArrayList<>(modules.size());
        modulesToImports(modules, theme, imports);
        lines.addAll(imports);

        return lines;
    }

    private void modulesToImports(Set<String> modules, AbstractTheme theme,
            Collection<String> imports) {
        Set<String> resourceNotFound = new HashSet<>();
        Set<String> npmNotFound = new HashSet<>();
        Set<String> visited = new HashSet<>();

        for (String originalModulePath : modules) {
            String translatedModulePath = originalModulePath;
            if (theme != null
                    && translatedModulePath.contains(theme.getBaseUrl())) {
                translatedModulePath = theme.translateUrl(translatedModulePath);
            }
            if (importedFileExists(translatedModulePath)) {
                imports.add(String.format(IMPORT,
                        toValidBrowserImport(translatedModulePath)));
            } else if (importedFileExists(originalModulePath)) {
                imports.add(String.format(IMPORT,
                        toValidBrowserImport(originalModulePath)));
            } else if (originalModulePath.startsWith("./")) {
                resourceNotFound.add(originalModulePath);
            } else {
                npmNotFound.add(originalModulePath);
                imports.add(String.format(IMPORT, originalModulePath));
            }

            if (theme != null) {
                handleImports(originalModulePath, theme, imports, visited);
            }
        }

        if (!resourceNotFound.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder(
                    "\n\n  Failed to resolve the following files either:"
                            + "\n   · in the `/frontend` sources folder"
                            + "\n   · or as a `META-INF/resources/frontend` resource in some JAR. \n       ➜ ");
            errorMessage.append(String.join("\n       ➜ ", resourceNotFound));
            errorMessage.append(
                    "\n  Please, double check that those files exist.\n");
            throw new IllegalStateException(errorMessage.toString());
        }

        if (!npmNotFound.isEmpty()) {
            String message = "\n\n  Failed to find the following imports in the `node_modules` tree:\n      ➜ "
                    + String.join("\n       ➜ ", npmNotFound)
                    + "\n  If the build fails, check that npm packages are installed.\n";
            log().info(message);
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
        visitedImports.add(filePath.normalize().toString());
        try {
            visitImportsRecursively(filePath, path, theme, imports,
                    visitedImports);
        } catch (IOException exception) {
            LoggerFactory.getLogger(TaskUpdateImports.class)
                    .warn("Could not read file {}. Skipping "
                            + "applyig theme for its imports", file.getPath(),
                            exception);
        }
    }

    private void visitImportsRecursively(Path filePath, String path,
            AbstractTheme theme, Collection<String> imports,
            Set<String> visitedImports) throws IOException {
        String content = Files.readAllLines(filePath, StandardCharsets.UTF_8)
                .stream().collect(Collectors.joining("\n"));
        ImportExtractor extractor = new ImportExtractor(content);
        List<String> importedPaths = extractor.getImportedPaths();
        for (String importedPath : importedPaths) {
            String resolvedPath = resolve(importedPath, filePath, path);
            if (resolvedPath.contains(theme.getBaseUrl())) {
                String translatedPath = theme.translateUrl(resolvedPath);
                if (importedFileExists(translatedPath)) {
                    imports.add(String.format(IMPORT,
                            normalizeImportPath(translatedPath)));
                }
            }
            handleImports(resolvedPath, theme, imports, visitedImports);
        }
    }

    private String normalizeImportPath(String path) {
        String importPath = toValidBrowserImport(path);
        File file = new File(importPath);
        return file.toPath().normalize().toString();
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

    private boolean importedFileExists(String jsImport) {
        File file = getImportedFrontendFile(jsImport);
        if (file != null) {
            return true;
        }

        // full path import e.g
        // /node_modules/@vaadin/vaadin-grid/vaadin-grid-column.js
        boolean found = isFile(nodeModulesFolder, jsImport);
        // omitted the .js extension e.g.
        // /node_modules/@vaadin/vaadin-grid/vaadin-grid-column
        found = found || isFile(nodeModulesFolder, jsImport + ".js");
        // has a package.json file e.g. /node_modules/package-name/package.json
        found = found || isFile(nodeModulesFolder, jsImport, PACKAGE_JSON);
        // file was generated by flow
        return found || isFile(generatedFolder,
                generatedResourcePathIntoRelativePath(jsImport));
    }

    /**
     * Returns a file for the {@code jsImport} path ONLY if it's either in the
     * frontend folder or {@code "node_modules/@vaadin/flow-frontend/") folder.
     *
    <p>
     * This method doesn't care about "published" WC paths (like "@vaadin/vaadin-grid" and so on).
     * See the {@link #importedFileExists(String)} method implementation.
     *
     * @return a file on FS if it exists and it's inside a frontend folder or in
     * node_modules/@vaadin/flow-frontend/, otherwise returns {@code null}
     */
    private File getImportedFrontendFile(String jsImport) {
        // file is in /frontend
        File file = getFile(frontendDirectory, jsImport);
        if (file.exists()) {
            return file;
        }
        // file is a flow resource e.g.
        // /node_modules/@vaadin/flow-frontend/gridConnector.js
        file = getFile(nodeModulesFolder, FLOW_NPM_PACKAGE_NAME, jsImport);
        return file.exists() ? file : null;
    }

    private File getFile(File base, String... path) {
        return new File(base, String.join("/", path));
    }

    private boolean isFile(File base, String... path) {
        return getFile(base, path).isFile();
    }

    private String toValidBrowserImport(String jsImport) {
        if (jsImport.startsWith(GENERATED_PREFIX)) {
            return generatedResourcePathIntoRelativePath(jsImport);
        } else if (isFile(frontendDirectory, jsImport)) {
            if (!jsImport.startsWith("./")) {
                log().warn(
                        "Use the './' prefix for files in the 'frontend' folder: '{}', please update your annotations.",
                        jsImport);
            }
            return WEBPACK_PREFIX_ALIAS + jsImport.replaceFirst("^\\./", "");
        }
        return jsImport;
    }

    private void updateMainJsFile(List<String> newContent) throws IOException {
        List<String> oldContent = generatedFlowImports.exists()
                ? FileUtils.readLines(generatedFlowImports, "UTF-8")
                : null;
        if (newContent.equals(oldContent)) {
            log().info("No js modules to update");
        } else {
            FileUtils.forceMkdir(generatedFlowImports.getParentFile());
            FileUtils.writeStringToFile(generatedFlowImports,
                    String.join("\n", newContent), "UTF-8");
            log().info("Updated {}", generatedFlowImports);
        }
    }

    private static String generatedResourcePathIntoRelativePath(String path) {
        return path.replace(GENERATED_PREFIX, "./");
    }
}
