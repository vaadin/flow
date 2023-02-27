/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_D_TS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;

/**
 * An updater that it's run when the servlet context is initialised in dev-mode
 * or when flow-maven-plugin goals are run in order to update Flow imports file
 * and {@value FrontendUtils#JAR_RESOURCES_FOLDER} contents by visiting all
 * classes with {@link JsModule} and {@link Theme} annotations.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskUpdateImports extends NodeUpdater {

    private static final String THEME_LINE_TPL = "%saddCssBlock('%s', true);";
    private static final String THEME_VARIANT_TPL = "document.documentElement.setAttribute('%s', '%s');";
    // Trim and remove new lines.
    private static final Pattern NEW_LINE_TRIM = Pattern
            .compile("(?m)(^\\s+|\\s?\n)");
    private final FrontendDependenciesScanner fallbackScanner;

    private class UpdateMainImportsFile extends AbstractUpdateImports {
        private static final String EXPORT_MODULES_DEF = "export declare const addCssBlock: (block: string, before?: boolean) => void;";

        private final File generatedFlowImports;
        private final File generatedFlowDefinitions;
        private final File fallBackImports;
        private final ClassFinder finder;

        UpdateMainImportsFile(ClassFinder classFinder, File fallBackImports,
                Options options) {
            super(options);
            generatedFlowImports = new File(options.getGeneratedFolder(),
                    IMPORTS_NAME);
            generatedFlowDefinitions = new File(options.getGeneratedFolder(),
                    IMPORTS_D_TS_NAME);
            finder = classFinder;
            this.fallBackImports = fallBackImports;

        }

        @Override
        protected void writeImportLines(List<String> lines) {
            if (fallBackImports != null) { // @formatter:off
                lines.add("let thisScript;");
                lines.add(
                        "const elements = document.getElementsByTagName('script');");
                lines.add("for (let i = 0; i < elements.length; i++) {");
                lines.add(" const script = elements[i];");
                lines.add(
                        " if (script.getAttribute('type')=='module' && script.getAttribute('data-app-id') && !script['vaadin-bundle']) {");
                lines.add("  thisScript = script;");
                lines.add("  break;");
                lines.add(" }");
                lines.add("}");
                lines.add("if (!thisScript) {");
                lines.add(
                        " throw new Error('Could not find the bundle script to identify the application id');");
                lines.add("}");
                lines.add("thisScript['vaadin-bundle'] = true;");
                lines.add(
                        "if (!window.Vaadin.Flow.fallbacks) { window.Vaadin.Flow.fallbacks={}; }");
                lines.add("const fallbacks = window.Vaadin.Flow.fallbacks;");
                lines.add(
                        "fallbacks[thisScript.getAttribute('data-app-id')] = {}");
                lines.add(
                        "fallbacks[thisScript.getAttribute('data-app-id')].loadFallback = function loadFallback() {");
                lines.add(" return import('./" + fallBackImports.getName()
                        + "');");
                lines.add("}");
            } // @formatter:on
            try {
                updateImportsFile(generatedFlowImports, lines);
                updateImportsFile(generatedFlowDefinitions,
                        getDefinitionLines());
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
                boolean hasApplicationTheme = themeDef != null
                        && !"".equals(themeDef.getName());
                // There is no application theme in use, write theme includes
                // here. Otherwise they are written by the theme
                if (!theme.getHeaderInlineContents().isEmpty()) {
                    lines.add("");
                    if (hasApplicationTheme) {
                        lines.add("// Handled in the application theme");
                    }
                    theme.getHeaderInlineContents()
                            .forEach(html -> addLines(lines,
                                    String.format(THEME_LINE_TPL,
                                            hasApplicationTheme ? "// " : "",
                                            NEW_LINE_TRIM.matcher(html)
                                                    .replaceAll(""))));
                }
                if (themeDef != null) {
                    theme.getHtmlAttributes(themeDef.getVariant())
                            .forEach((key, value) -> addLines(lines, String
                                    .format(THEME_VARIANT_TPL, key, value)));
                }
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
            return NodeUpdater.getGeneratedModules(options.getGeneratedFolder(),
                    exclude);
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

        @Override
        protected String getImportsNotFoundMessage() {
            return getAbsentPackagesMessage();
        }

        protected List<String> getDefinitionLines() {
            List<String> lines = new ArrayList<>();
            addLines(lines, EXPORT_MODULES_DEF);
            return lines;
        }
    }

    private class UpdateFallBackImportsFile extends AbstractUpdateImports {
        private final File generatedFallBack;
        private final ClassFinder finder;

        UpdateFallBackImportsFile(ClassFinder classFinder, Options options) {
            super(options);
            generatedFallBack = new File(options.getGeneratedFolder(),
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
            return filter(set.stream()).collect(Collectors.toList());
        }

        @Override
        protected Set<String> getScripts() {
            LinkedHashSet<String> set = new LinkedHashSet<>(
                    fallbackScanner.getScripts());
            set.removeAll(frontDeps.getScripts());
            return filter(set.stream()).collect(Collectors.toSet());
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

        @Override
        protected String getImportsNotFoundMessage() {
            return getAbsentPackagesMessage();
        }

        @Override
        protected String getThemeIdPrefix() {
            return "fallback_" + super.getThemeIdPrefix();
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
     * @param tokenFileData
     *            object to fill with token file data, may be {@code null}
     * @param enablePnpm
     *            if {@code true} then pnpm is used instead of npm, otherwise
     *            npm is used
     * @param buildDir
     *            the used build directory
     */
    TaskUpdateImports(ClassFinder finder,
            FrontendDependenciesScanner frontendDepScanner,
            SerializableFunction<ClassFinder, FrontendDependenciesScanner> fallBackScannerProvider,
            Options options) {
        super(finder, frontendDepScanner, options);
        fallbackScanner = fallBackScannerProvider.apply(finder);
    }

    @Override
    public void execute() {
        File fallBack = null;
        if (fallbackScanner != null) {
            UpdateFallBackImportsFile fallBackUpdate = new UpdateFallBackImportsFile(
                    finder, options);
            fallBackUpdate.run();
            fallBack = fallBackUpdate.getGeneratedFallbackFile();
            updateBuildFile(fallBackUpdate);
        }

        UpdateMainImportsFile mainUpdate = new UpdateMainImportsFile(finder,
                fallBack, options);
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
            log().debug("Theme definition is discovered by the fallback "
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
        boolean tokenFileExists = options.getTokenFile() != null
                && options.getTokenFile().exists();
        if (!tokenFileExists) {
            log().warn(
                    "Token file is not available. Fallback chunk data won't be written.");
        }
        try {
            if (tokenFileExists) {
                String json = FileUtils.readFileToString(options.getTokenFile(),
                        StandardCharsets.UTF_8);
                JsonObject buildInfo = json.isEmpty() ? Json.createObject()
                        : JsonUtil.parse(json);
                populateFallbackData(buildInfo, updater);
                FileUtils.write(options.getTokenFile(),
                        JsonUtil.stringify(buildInfo, 2),
                        StandardCharsets.UTF_8);
            }

        } catch (IOException e) {
            log().warn("Unable to read token file", e);
        }
        if (options.getTokenFileData() != null) {
            populateFallbackData(options.getTokenFileData(), updater);
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

    private Stream<String> filter(Stream<String> modules) {
        if (options.isProductionMode()) {
            return modules.filter(
                    module -> CvdlProducts.includeInFallbackBundle(module,
                            options.getNodeModulesFolder()));
        }
        return modules;
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

    private String getAbsentPackagesMessage() {
        String lockFile = options.isEnablePnpm() ? "pnpm-lock.yaml"
                : Constants.PACKAGE_LOCK_JSON;
        String command = options.isEnablePnpm() ? "pnpm" : "npm";
        String note = "";
        if (options.isEnablePnpm()) {
            note = "\nMake sure first that `pnpm` command is installed, otherwise you should install it using npm: `npm add -g pnpm@"
                    + FrontendTools.DEFAULT_PNPM_VERSION + "`";
        }
        return String.format(
                "If the build fails, check that npm packages are installed.\n\n"
                        + "  To fix the build remove `%s` and `node_modules` directory to reset modules.\n"
                        + "  In addition you may run `%s install` to fix `node_modules` tree structure.%s",
                lockFile, command, note);
    }

}
