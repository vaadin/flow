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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;

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

    private static final String THEME_PREPARE = "const div = document.createElement('div');";
    private static final String THEME_LINE_TPL = "div.innerHTML = '%s';%n"
            + "document.head.insertBefore(div.firstElementChild, document.head.firstChild);";
    private static final String THEME_VARIANT_TPL = "document.body.setAttribute('%s', '%s');";
    // Trim and remove new lines.
    private static final Pattern NEW_LINE_TRIM = Pattern
            .compile("(?m)(^\\s+|\\s?\n)");

    private final File frontendDirectory;
    private final FrontendDependenciesScanner fallbackScanner;
    private final ClassFinder finder;
    private final File tokenFile;
    private final JsonObject tokenFileData;

    private class UpdateMainImportsFile extends AbstractUpdateImports {

        private final File generatedFlowImports;
        private final File fallBackImports;
        private final ClassFinder finder;

        UpdateMainImportsFile(ClassFinder classFinder, File frontendDirectory,
                File npmDirectory, File generatedDirectory,
                File fallBackImports) {
            super(frontendDirectory, npmDirectory, generatedDirectory);
            generatedFlowImports = new File(generatedDirectory, IMPORTS_NAME);
            finder = classFinder;
            this.fallBackImports = fallBackImports;
        }

        @Override
        protected void writeImportLines(List<String> lines) {
            if (fallBackImports != null) {
                lines.add(
                        "var scripts = document.getElementsByTagName('script');");
                lines.add("var thisScript;");
                lines.add(
                        "var elements = document.getElementsByTagName('script');");
                lines.add("for (var i = 0; i < elements.length; i++) {");
                lines.add("    var script = elements[i];");
                lines.add(
                        "    if (script.getAttribute('type')=='module' && script.getAttribute('data-app-id') && !script['vaadin-bundle']) {");
                lines.add("        thisScript = script;break;");
                lines.add("     }");
                lines.add("}");
                lines.add("if (!thisScript) {");
                lines.add(
                        "    throw new Error('Could not find the bundle script to identify the application id');");
                lines.add("}");
                lines.add("thisScript['vaadin-bundle'] = true;");
                lines.add(
                        "if (!window.Vaadin.Flow.fallbacks) { window.Vaadin.Flow.fallbacks={}; }");
                lines.add("var fallbacks = window.Vaadin.Flow.fallbacks;");
                lines.add(
                        "fallbacks[thisScript.getAttribute('data-app-id')] = {}");
                lines.add(
                        "fallbacks[thisScript.getAttribute('data-app-id')].loadFallback = function loadFallback(){");
                lines.add("   return import('./" + fallBackImports.getName()
                        + "');");
                lines.add("}");
            }
            try {
                updateImportsFile(generatedFlowImports, lines);
            } catch (IOException e) {
                throw new IllegalStateException(String.format(
                        "Failed to update the Flow imports file '%s'",
                        generatedFlowImports), e);
            }
        }

        @Override
        protected Collection<String> getThemeLines() {
            Collection<String> lines = new ArrayList<>();
            AbstractTheme theme = getTheme();
            ThemeDefinition themeDef = getThemeDefinition();
            if (theme != null) {
                if (!theme.getHeaderInlineContents().isEmpty()) {
                    lines.add(THEME_PREPARE);
                    theme.getHeaderInlineContents()
                            .forEach(html -> addLines(lines,
                                    String.format(THEME_LINE_TPL, NEW_LINE_TRIM
                                            .matcher(html).replaceAll(""))));
                }
                theme.getHtmlAttributes(themeDef.getVariant())
                        .forEach((key, value) -> addLines(lines,
                                String.format(THEME_VARIANT_TPL, key, value)));
                lines.add("");
            }
            return lines;
        }

        @Override
        protected List<String> getModules() {
            return frontDeps.getModules();
        }

        @Override
        protected Set<String> getScripts() {
            return frontDeps.getScripts();
        }

        @Override
        protected URL getResource(String name) {
            return finder.getResource(name);
        }

        @Override
        protected Collection<String> getGeneratedModules() {
            final Set<String> exclude = new HashSet<>(
                    Arrays.asList(generatedFlowImports.getName(),
                            FrontendUtils.FALLBACK_IMPORTS_NAME));
            return NodeUpdater.getGeneratedModules(generatedFolder, exclude);
        }

        @Override
        protected ThemeDefinition getThemeDefinition() {
            return TaskUpdateImports.this.getThemeDefinition();
        }

        @Override
        protected AbstractTheme getTheme() {
            return TaskUpdateImports.this.getTheme();
        }

        @Override
        protected Set<CssData> getCss() {
            return frontDeps.getCss();
        }

        @Override
        protected Logger getLogger() {
            return log();
        }
    }

    private class UpdateFallBackImportsFile extends AbstractUpdateImports {

        private final File generatedFallBack;
        private final ClassFinder finder;

        UpdateFallBackImportsFile(ClassFinder classFinder,
                File frontendDirectory, File npmDirectory,
                File generatedDirectory) {
            super(frontendDirectory, npmDirectory, generatedDirectory);
            generatedFallBack = new File(generatedDirectory,
                    FrontendUtils.FALLBACK_IMPORTS_NAME);
            finder = classFinder;
        }

        @Override
        protected void writeImportLines(List<String> lines) {
            try {
                updateImportsFile(generatedFallBack, lines);
            } catch (IOException e) {
                throw new IllegalStateException(String.format(
                        "Failed to update the Flow fallback imports file '%s'",
                        getGeneratedFallbackFile()), e);
            }
        }

        @Override
        protected Collection<String> getThemeLines() {
            return Collections.emptyList();
        }

        @Override
        protected List<String> getModules() {
            LinkedHashSet<String> set = new LinkedHashSet<>(
                    fallbackScanner.getModules());
            set.removeAll(frontDeps.getModules());
            return new ArrayList<String>(set);
        }

        @Override
        protected Set<String> getScripts() {
            LinkedHashSet<String> set = new LinkedHashSet<>(
                    fallbackScanner.getScripts());
            set.removeAll(frontDeps.getScripts());
            return set;
        }

        @Override
        protected URL getResource(String name) {
            return finder.getResource(name);
        }

        @Override
        protected Collection<String> getGeneratedModules() {
            return Collections.emptyList();
        }

        @Override
        protected ThemeDefinition getThemeDefinition() {
            return TaskUpdateImports.this.getThemeDefinition();
        }

        @Override
        protected AbstractTheme getTheme() {
            return TaskUpdateImports.this.getTheme();
        }

        @Override
        protected Set<CssData> getCss() {
            LinkedHashSet<CssData> set = new LinkedHashSet<>(
                    fallbackScanner.getCss());
            set.removeAll(frontDeps.getCss());
            return set;
        }

        @Override
        protected Logger getLogger() {
            return log();
        }

        File getGeneratedFallbackFile() {
            return generatedFallBack;
        }
    }

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param finder
     *            a reusable class finder
     * @param frontendDepScanner
     *            a reusable frontend dependencies scanner
     * @param fallBackScannerProvider
     *            fallback scanner provider, not {@code null}
     * @param npmFolder
     *            folder with the `package.json` file
     * @param generatedPath
     *            folder where flow generated files will be placed.
     * @param frontendDirectory
     *            a directory with project's frontend files
     * @param tokenFile
     *            the token (flow-build-info.json) path, may be {@code null}
     */
    TaskUpdateImports(ClassFinder finder,
            FrontendDependenciesScanner frontendDepScanner,
            SerializableFunction<ClassFinder, FrontendDependenciesScanner> fallBackScannerProvider,
            File npmFolder, File generatedPath, File frontendDirectory,
            File tokenFile) {
        this(finder, frontendDepScanner, fallBackScannerProvider, npmFolder,
                generatedPath, frontendDirectory, tokenFile, null);
    }

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param finder
     *            a reusable class finder
     * @param frontendDepScanner
     *            a reusable frontend dependencies scanner
     * @param fallBackScannerProvider
     *            fallback scanner provider, not {@code null}
     * @param npmFolder
     *            folder with the `package.json` file
     * @param generatedPath
     *            folder where flow generated files will be placed.
     * @param frontendDirectory
     *            a directory with project's frontend files
     * @param tokenFile
     *            the token (flow-build-info.json) path, may be {@code null}
     * @param tokenFileData
     *            object to fill with token file data, may be {@code null}
     */
    TaskUpdateImports(ClassFinder finder,
            FrontendDependenciesScanner frontendDepScanner,
            SerializableFunction<ClassFinder, FrontendDependenciesScanner> fallBackScannerProvider,
            File npmFolder, File generatedPath, File frontendDirectory,
            File tokenFile, JsonObject tokenFileData) {
        super(finder, frontendDepScanner, npmFolder, generatedPath);
        this.frontendDirectory = frontendDirectory;
        fallbackScanner = fallBackScannerProvider.apply(finder);
        this.finder = finder;
        this.tokenFile = tokenFile;
        this.tokenFileData = tokenFileData;
    }

    @Override
    public void execute() {
        File fallBack = null;
        if (fallbackScanner != null) {
            UpdateFallBackImportsFile fallBackUpdate = new UpdateFallBackImportsFile(
                    finder, frontendDirectory, npmFolder, generatedFolder);
            fallBackUpdate.run();
            fallBack = fallBackUpdate.getGeneratedFallbackFile();
            updateBuildFile(fallBackUpdate);
        }

        UpdateMainImportsFile mainUpdate = new UpdateMainImportsFile(finder,
                frontendDirectory, npmFolder, generatedFolder, fallBack);
        mainUpdate.run();
    }

    private ThemeDefinition getThemeDefinition() {
        ThemeDefinition def = frontDeps.getThemeDefinition();
        if (def != null) {
            return def;
        }
        if (fallbackScanner == null) {
            return null;
        }
        def = fallbackScanner.getThemeDefinition();
        if (def != null && log().isDebugEnabled()) {
            log().debug("Theme definition is discoverd by the fallback "
                    + "scanner and not discovered by the main scanner. Theme '{}' will be used",
                    def.getTheme().getName());
        }
        return def;
    }

    private AbstractTheme getTheme() {
        AbstractTheme theme = frontDeps.getTheme();
        if (theme != null) {
            return theme;
        }
        if (fallbackScanner == null) {
            return null;
        }
        return fallbackScanner.getTheme();
    }

    private void updateBuildFile(AbstractUpdateImports updater) {
        boolean tokenFileExists = tokenFile != null && tokenFile.exists();
        if (!tokenFileExists) {
            log().warn(
                    "Token file is not available. Fallback chunk data won't be written.");
        }
        try {
            if (tokenFileExists) {
                String json = FileUtils.readFileToString(tokenFile,
                        StandardCharsets.UTF_8);
                JsonObject buildInfo = json.isEmpty() ? Json.createObject()
                        : JsonUtil.parse(json);
                populateFallbackData(buildInfo, updater);
                FileUtils.write(tokenFile, JsonUtil.stringify(buildInfo, 2),
                        StandardCharsets.UTF_8);
            }

        } catch (IOException e) {
            log().warn("Unable to read token file", e);
        }
        if (tokenFileData != null) {
            populateFallbackData(tokenFileData, updater);
        }
    }

    private void populateFallbackData(JsonObject object,
            AbstractUpdateImports updater) {
        JsonObject fallback = Json.createObject();
        fallback.put(FrontendUtils.JS_MODULES, makeFallbackModules(updater));
        fallback.put(FrontendUtils.CSS_IMPORTS,
                makeFallbackCssImports(updater));

        JsonObject chunks = Json.createObject();
        chunks.put(FrontendUtils.FALLBACK, fallback);

        object.put(FrontendUtils.CHUNKS, chunks);
    }

    private JsonArray makeFallbackModules(AbstractUpdateImports updater) {
        JsonArray array = Json.createArray();
        List<String> modules = updater.getModules();
        Set<String> scripts = updater.getScripts();

        Iterator<String> modulesIterator = modules.iterator();
        Iterator<String> scriptsIterator = scripts.iterator();
        for (int i = 0; i < modules.size() + scripts.size(); i++) {
            if (i < modules.size()) {
                array.set(i, modulesIterator.next());
            } else {
                array.set(i, scriptsIterator.next());
            }

        }
        return array;
    }

    private JsonArray makeFallbackCssImports(AbstractUpdateImports updater) {
        JsonArray array = Json.createArray();
        Set<CssData> css = updater.getCss();
        Iterator<CssData> iterator = css.iterator();
        for (int i = 0; i < css.size(); i++) {
            array.set(i, makeCssJson(iterator.next()));
        }
        return array;
    }

    private JsonObject makeCssJson(CssData data) {
        JsonObject object = Json.createObject();
        if (data.getId() != null) {
            object.put("id", data.getId());
        }
        if (data.getInclude() != null) {
            object.put("include", data.getInclude());
        }
        if (data.getThemefor() != null) {
            object.put("themeFor", data.getThemefor());
        }
        if (data.getValue() != null) {
            object.put("value", data.getValue());
        }
        return object;
    }

}
