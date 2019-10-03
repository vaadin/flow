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
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_D_TS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_PREFIX_ALIAS;

/**
 * An updater that it's run when the servlet context is initialised in dev-mode
 * or when flow-maven-plugin goals are run in order to update Flow imports file
 * and <code>node_module/@vaadin/flow-frontend</code> contents by visiting all
 * classes with {@link JsModule} {@link HtmlImport} and {@link Theme}
 * annotations.
 *
 * @since 2.0
 */
public class TaskUpdateImports extends NodeUpdater {

    private final File generatedFlowImports;
    private final File generatedFlowDefinitions;
    private final File frontendDirectory;
    private static final String IMPORT_TEMPLATE = "import '%s';";

    private static final String THEME_LINE_TPL ="addCssBlock('%s', 1);";
    private static final String THEME_VARIANT_TPL = "document.body.setAttribute('%s', '%s');";

    private static final String EXPORT_FUNCTIONS = "export const addCssBlock = function(block, before = false) {\n" +
            " const tpl = document.createElement('template');\n" +
            " tpl.innerHTML = block;\n" +
            " document.head[before ? 'insertBefore' : 'appendChild'](tpl.content, document.head.firstChild);\n" +
            "};";

    private static final String EXPORT_DEFINITIONS = "export function addCssBlock(block:string, before?:boolean): void;";

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

    // Trim and remove new lines.
    private static final Pattern NEW_LINE_TRIM = Pattern
            .compile("(?m)(^\\s+|\\s?\n)");

    // Used to recognize and sort FRONTEND/ imports in the final
    // generated-flow-imports.js
    private static final Pattern FRONTEND_IMPORT_LINE = Pattern.compile(
            String.format(IMPORT_TEMPLATE, WEBPACK_PREFIX_ALIAS + "\\S*"));

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
        this.generatedFlowDefinitions = new File(generatedPath, IMPORTS_D_TS_NAME);
    }

    @Override
    public void execute() {
        Set<String> modules = new LinkedHashSet<>();
        modules.addAll(resolveModules(frontDeps.getModules(), true));
        modules.addAll(resolveModules(frontDeps.getScripts(), false));

        modules.addAll(getGeneratedModules(generatedFolder,
                Collections.singleton(generatedFlowImports.getName())));

        modules.removeIf(UrlUtil::isExternal);

        try {
            updateFile(generatedFlowImports, getMainJsContent(modules));
            updateFile(generatedFlowDefinitions,
                    Arrays.asList(EXPORT_DEFINITIONS.split("\r?\n")));
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("Failed to update the Flow imports file '%s'",
                            generatedFlowImports),
                    e);
        }
    }

    private List<String> getMainJsContent(Set<String> modules) {
        List<String> lines = new ArrayList<>();

        lines.addAll(getExportFunctions());
        lines.addAll(getThemeLines());
        lines.addAll(getCssLines());

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

        return lines;
    }

    private Collection<String> getExportFunctions() {
        return Arrays.asList(EXPORT_FUNCTIONS.split("\r?\n"));
    }

    private Collection<String> getThemeLines() {
        Collection<String> lines = new ArrayList<>();
        AbstractTheme theme = frontDeps.getTheme();
        ThemeDefinition themeDef = frontDeps.getThemeDefinition();
        if (theme != null) {
            lines.add("");
            if (!theme.getHeaderInlineContents().isEmpty()) {
                theme.getHeaderInlineContents().forEach(
                        html -> addLines(lines, String.format(THEME_LINE_TPL,
                                NEW_LINE_TRIM.matcher(html).replaceAll(""))));
            }
            theme.getHtmlAttributes(themeDef.getVariant())
                    .forEach((key, value) -> addLines(lines,
                            String.format(THEME_VARIANT_TPL, key, value)));
            lines.add("");
        }
        return lines;
    }

    private void addLines(Collection<String> lines, String content) {
        lines.addAll(Arrays.asList(content.split("\r?\n")));
    }

    private Collection<String> getCssLines() {
        Collection<String> lines = new ArrayList<>();
        Set<CssData> css = frontDeps.getCss();
        if (!css.isEmpty()) {
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
                        frontendDirectory.getPath());
                String suffix = String.format(
                        "Check that they exist or are installed. If you use a custom directory "
                                + "for your resource files instead of the default `frontend` folder "
                                + "then make sure it's correctly configured (e.g. set '%s' property)",
                        FrontendUtils.PARAM_FRONTEND_DIR);
                throw new IllegalStateException(
                        notFoundMessage(cssNotFound, prefix, suffix));
            }
            lines.add("");
        }
        return lines;
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

    private Collection<String> getModuleLines(Set<String> modules) {
        Set<String> resourceNotFound = new HashSet<>();
        Set<String> npmNotFound = new HashSet<>();
        Set<String> visited = new HashSet<>();
        AbstractTheme theme = frontDeps.getTheme();
        Collection<String> lines = new ArrayList<>();

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
                lines.add(String.format(IMPORT_TEMPLATE,
                        toValidBrowserImport(localModulePath)));
            } else if (importedFileExists(translatedModulePath)) {
                lines.add(String.format(IMPORT_TEMPLATE,
                        toValidBrowserImport(translatedModulePath)));
            } else if (importedFileExists(originalModulePath)) {
                lines.add(String.format(IMPORT_TEMPLATE,
                        toValidBrowserImport(originalModulePath)));
            } else if (originalModulePath.startsWith("./")) {
                resourceNotFound.add(originalModulePath);
            } else {
                npmNotFound.add(originalModulePath);
                lines.add(String.format(IMPORT_TEMPLATE, originalModulePath));
            }

            if (theme != null) {
                handleImports(originalModulePath, theme, lines, visited);
            }
        }

        if (!resourceNotFound.isEmpty()) {
            String prefix =  "Failed to find the following files: ";
            String suffix = String.format(
                    "%n  Locations searched were:"
                            + "%n      - `%s` in this project"
                            + "%n      - `%s` in included JARs"
                            + "%n%n  Please, double check that those files exist. If you use a custom directory "
                            + "for your resource files instead of default "
                            + "`frontend` folder then make sure you it's correctly configured "
                            + "(e.g. set '%s' property)",
                    frontendDirectory.getPath(),
                    Constants.RESOURCES_FRONTEND_DEFAULT,
                    FrontendUtils.PARAM_FRONTEND_DIR);
            throw new IllegalStateException(
                    notFoundMessage(resourceNotFound, prefix, suffix));
        }

        if (!npmNotFound.isEmpty() && log().isInfoEnabled()) {
            log().info(notFoundMessage(npmNotFound,
                    "Failed to find the following imports in the `node_modules` tree:",
                    "If the build fails, check that npm packages are installed.\n\n"
                    + "  To fix the build remove `node_modules` directory to reset modules.\n"
                    + "  In addition you may run `npm install` to fix `node_modules` tree structure."));
        }
        return lines;
    }

    private String notFoundMessage(Set<String> files, String prefix,
            String suffix) {
        return String.format("%n%n  %s%n      - %s%n  %s%n%n", prefix,
                String.join("\n      - ", files), suffix);
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
            LoggerFactory.getLogger(TaskUpdateImports.class)
                    .warn("Could not read file {}. Skipping "
                            + "applying theme for its imports", file.getPath(),
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
            // try to resolve path relatively to original filePath (inside user
            // frontend folder)
            String resolvedPath = resolve(importedPath, filePath, path);
            File file = getImportedFrontendFile(resolvedPath);
            if (file == null && !importedPath.startsWith("./")) {
                // In case such file doesn't exist it may be external: inside
                // node_modules folder
                file = getFile(nodeModulesFolder, resolvedPath);
                resolvedPath = importedPath;
            }
            if (file == null) {
                // don't do anything if such file doesn't exist at all
                continue;
            }
            if (resolvedPath.contains(theme.getBaseUrl())) {
                String translatedPath = theme.translateUrl(resolvedPath);
                if (importedFileExists(translatedPath)) {
                    imports.add(String.format(IMPORT_TEMPLATE,
                            normalizeImportPath(translatedPath)));
                }
            }
            handleImports(resolvedPath, theme, imports, visitedImports);
        }
    }

    private String normalizeImportPath(String path) {
        String importPath = toValidBrowserImport(path);
        File file = new File(importPath);
        return file.toPath().normalize().toString().replace("\\", "/");
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

    private boolean frontendFileExists(String jsImport) {
        File file = getFile(frontendDirectory, jsImport);
        return file.exists();
    }

    private boolean importedFileExists(String importName) {
        File file = getImportedFrontendFile(importName);
        if (file != null) {
            return true;
        }

        // full path import e.g
        // /node_modules/@vaadin/vaadin-grid/vaadin-grid-column.js
        boolean found = isFile(nodeModulesFolder, importName);
        if (importName.toLowerCase().endsWith(".css")) {
            return found;
        }
        // omitted the .js extension e.g.
        // /node_modules/@vaadin/vaadin-grid/vaadin-grid-column
        found = found || isFile(nodeModulesFolder, importName + ".js");
        // has a package.json file e.g. /node_modules/package-name/package.json
        found = found || isFile(nodeModulesFolder, importName, PACKAGE_JSON);
        // file was generated by flow
        found = found || isFile(generatedFolder,
                generatedResourcePathIntoRelativePath(importName));

        return found;
    }

    /**
     * Returns a file for the {@code jsImport} path ONLY if it's either in the
     * {@code "frontend"} folder or
     * {@code "node_modules/@vaadin/flow-frontend/") folder.
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
                        "Use the './' prefix for files in the '{}' folder: '{}', please update your annotations.",
                        frontendDirectory, jsImport);
            }
            return WEBPACK_PREFIX_ALIAS + jsImport.replaceFirst("^\\./", "");
        }
        return jsImport;
    }


    private void updateFile(File file, List<String> newContent) throws IOException {
        List<String> oldContent = generatedFlowImports.exists()
                ? FileUtils.readLines(generatedFlowImports, "UTF-8")
                : null;

        if (newContent.equals(oldContent)) {
            log().info("No changes for {}", file);
        } else {
            FileUtils.forceMkdir(file.getParentFile());
            FileUtils.writeStringToFile(file, String.join("\n", newContent),
                    "UTF-8");
            log().info("Updated {}", file);
        }
    }

    private static String generatedResourcePathIntoRelativePath(String path) {
        return path.replace(GENERATED_PREFIX, "./");
    }
}
