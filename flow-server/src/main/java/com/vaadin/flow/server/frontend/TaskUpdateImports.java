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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_PREFIX_ALIAS;

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
        Set<String> modules = new HashSet<>(
                getJavascriptJsModules(frontDeps.getModules()));
        modules.addAll(getJavascriptJsModules(frontDeps.getScripts()));

        modules.addAll(getGeneratedModules(generatedFolder,
                Collections.singleton(generatedFlowImports.getName())));

        // filter out external URLs (including "://")
        modules = modules.stream().filter(module -> !module.contains("://"))
                .collect(Collectors.toSet());

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

        lines.addAll(modulesToImports(modules, theme));

        return lines;
    }

    private List<String> modulesToImports(Set<String> modules,
            AbstractTheme theme) {
        List<String> imports = new ArrayList<>(modules.size());
        Set<String> resourceNotFound = new HashSet<>();
        Set<String> npmNotFound = new HashSet<>();

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

        return imports;
    }

    private boolean importedFileExists(String jsImport) {
        // file is in /frontend
        boolean found = isFile(frontendDirectory, jsImport);
        // file is a flow resource e.g.
        // /node_modules/@vaadin/flow-frontend/gridConnector.js
        found = found
                || isFile(nodeModulesFolder, FLOW_NPM_PACKAGE_NAME, jsImport);
        // full path import e.g
        // /node_modules/@vaadin/vaadin-grid/vaadin-grid-column.js
        found = found || isFile(nodeModulesFolder, jsImport);
        // omitted the .js extension e.g.
        // /node_modules/@vaadin/vaadin-grid/vaadin-grid-column
        found = found || isFile(nodeModulesFolder, jsImport + ".js");
        // has a package.json file e.g. /node_modules/package-name/package.json
        found = found || isFile(nodeModulesFolder, jsImport, PACKAGE_JSON);
        // file was generated by flow
        return found || isFile(generatedFolder,
                generatedResourcePathIntoRelativePath(jsImport));
    }

    private boolean isFile(File base, String... path) {
        return new File(base, String.join("/", path)).isFile();
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
