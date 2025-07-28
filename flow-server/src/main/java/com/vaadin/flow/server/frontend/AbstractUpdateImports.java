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
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ChunkInfo;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.server.frontend.scanner.EntryPointType;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.theme.AbstractTheme;

import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND_FOLDER_ALIAS;

/**
 * Common logic for generate import file JS content.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 *
 */
abstract class AbstractUpdateImports implements Runnable {

    private static final String CSS_PREPARE = "function addCssBlock(block) {\n"
            + " const tpl = document.createElement('template');\n"
            + " tpl.innerHTML = block;\n"
            + " document.head.appendChild(tpl.content);\n" + "}";
    private static final String IMPORT_INJECT = "import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';\n";
    private static final String IMPORT_WC_INJECT = "import { injectGlobalWebcomponentCss } from 'Frontend/generated/jar-resources/theme-util.js';\n";

    private static final String CSS_IMPORT = "import $cssFromFile_%d from '%s';%n";
    private static final String CSS_IMPORT_AND_MAKE_LIT_CSS = CSS_IMPORT
            + "const $css_%1$d = typeof $cssFromFile_%1$d  === 'string' ? unsafeCSS($cssFromFile_%1$d) : $cssFromFile_%1$d;";
    private static final String CSS_PRE = CSS_IMPORT_AND_MAKE_LIT_CSS + "%n"
            + "addCssBlock(`";
    private static final String CSS_POST = "`);";
    private static final String CSS_BASIC_TPL = CSS_PRE
            + "<style%s>${$css_%1$d}</style>" + CSS_POST;
    private static final String INJECT_CSS = CSS_IMPORT
            + "%ninjectGlobalCss($cssFromFile_%1$d.toString(), 'CSSImport end', document);%n";
    private static final Pattern INJECT_CSS_PATTERN = Pattern
            .compile("^\\s*injectGlobalCss\\(([^,]+),.*$");
    private static final String INJECT_WC_CSS = "injectGlobalWebcomponentCss(%s);";

    private static final String THEMABLE_MIXIN_IMPORT = "import { css, unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin';";
    private static final String REGISTER_STYLES_FOR_TEMPLATE = CSS_IMPORT_AND_MAKE_LIT_CSS
            + "%n" + "registerStyles('%s', $css_%1$d%s);";
    static final String RESET_FOCUS_JS = "() => {\n"
            + " let ae=document.activeElement;\n"
            + " while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;\n"
            + " return !ae || ae.blur() || ae.focus() || true;\n" + "}";
    private static final String IMPORT_TEMPLATE = "import '%s';";
    private static final Pattern STARTING_DOT_SLASH = Pattern.compile("^\\./+");
    private static final Pattern VAADIN_LUMO_GLOBAL_IMPORT = Pattern
            .compile(".*@vaadin/vaadin-lumo-styles/.*-global.js.*");
    final Options options;

    private final UnaryOperator<String> themeToLocalPathConverter;

    private final Map<Path, List<String>> resolvedImportPathsCache = new HashMap<>();

    private FrontendDependenciesScanner scanner;

    private ClassFinder classFinder;

    final File generatedFlowImports;
    final File generatedFlowWebComponentImports;
    private final File generatedFlowDefinitions;
    private File chunkFolder;

    private final GeneratedFilesSupport generatedFilesSupport;

    AbstractUpdateImports(Options options,
            FrontendDependenciesScanner scanner) {
        this(options, scanner, new GeneratedFilesSupport());
    }

    AbstractUpdateImports(Options options, FrontendDependenciesScanner scanner,
            GeneratedFilesSupport generatedFilesSupport) {
        this.generatedFilesSupport = generatedFilesSupport;
        this.options = options;
        this.scanner = scanner;
        this.classFinder = options.getClassFinder();
        this.themeToLocalPathConverter = createThemeToLocalPathConverter(
                scanner.getTheme());

        generatedFlowImports = FrontendUtils
                .getFlowGeneratedImports(options.getFrontendDirectory());
        generatedFlowDefinitions = new File(
                generatedFlowImports.getParentFile(),
                FrontendUtils.IMPORTS_D_TS_NAME);

        generatedFlowWebComponentImports = FrontendUtils
                .getFlowGeneratedWebComponentsImports(
                        options.getFrontendDirectory());
        this.chunkFolder = new File(generatedFlowImports.getParentFile(),
                "chunks");

    }

    @Override
    public void run() {
        getLogger().debug("Start updating imports file and chunk files.");
        long start = System.nanoTime();

        Map<ChunkInfo, List<CssData>> css = scanner.getCss();
        Map<ChunkInfo, List<String>> javascript = getMergedJavascript();

        Map<File, List<String>> output = process(css, javascript);
        writeOutput(output);
        writeWebComponentImports(
                filterWebComponentImports(output.get(generatedFlowImports)));

        getLogger().debug("Imports and chunks update took {} ms.",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    private Map<ChunkInfo, List<String>> getMergedJavascript() {
        getLogger().debug("Start collecting scanned JS modules and scripts.");
        long start = System.nanoTime();

        Map<ChunkInfo, List<String>> javascript;
        Map<ChunkInfo, List<String>> modules = scanner.getModules();
        Map<ChunkInfo, List<String>> scripts = scanner.getScripts();

        if (options.isProductionMode()) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Found {} modules, and {} scripts.",
                        modules.size(), scripts.size());
            }
            javascript = mergeJavascript(modules, scripts);
        } else {
            Map<ChunkInfo, List<String>> modulesDevelopment = scanner
                    .getModulesDevelopment();
            Map<ChunkInfo, List<String>> scriptsDevelopment = scanner
                    .getScriptsDevelopment();

            if (getLogger().isDebugEnabled()) {
                getLogger().debug(
                        "Found {} modules, {} scripts, {} dev-mode modules and {} dev-mode scripts.",
                        modules.size(), scripts.size(),
                        modulesDevelopment.size(), scriptsDevelopment.size());
            }

            javascript = mergeJavascript(modules, modulesDevelopment, scripts,
                    scriptsDevelopment);
        }

        getLogger().debug("JS modules and scripts collected in {} ms.",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));

        return javascript;
    }

    protected void writeOutput(Map<File, List<String>> outputFiles) {
        try {
            for (Entry<File, List<String>> output : outputFiles.entrySet()) {
                generatedFilesSupport.writeIfChanged(output.getKey(),
                        output.getValue());
            }
            if (chunkFolder.exists() && chunkFolder.isDirectory()) {
                for (File existingChunk : chunkFolder.listFiles()) {
                    if (!outputFiles.containsKey(existingChunk)) {
                        existingChunk.delete();
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to update the generated Flow imports", e);
        }
    }

    // Visible for test
    List<String> filterWebComponentImports(List<String> lines) {
        if (lines != null) {
            // Exclude Lumo global imports for exported web-component
            List<String> copy = new ArrayList<>(lines);
            copy.add(0, IMPORT_WC_INJECT);
            copy.removeIf(VAADIN_LUMO_GLOBAL_IMPORT.asPredicate());
            // Add global CSS imports with a per-webcomponent registration
            final ListIterator<String> li = copy.listIterator();
            while (li.hasNext()) {
                adaptCssInjectForWebComponent(li, li.next());
            }
            return copy;
        }
        return lines;
    }

    private void adaptCssInjectForWebComponent(ListIterator<String> iterator,
            String line) {
        Matcher matcher = INJECT_CSS_PATTERN.matcher(line);
        if (matcher.matches()) {
            // Remove from body and only add to shadowroot
            iterator.remove();
            iterator.add(String.format(INJECT_WC_CSS, matcher.group(1)));
        }
    }

    private void writeWebComponentImports(List<String> lines) {
        if (lines != null) {
            try {
                generatedFilesSupport.writeIfChanged(
                        generatedFlowWebComponentImports, lines);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to update the generated Flow imports for exported web component",
                        e);
            }
        }
    }

    /**
     * Processes what the scanner found and produces a set of files to write to
     * the generated folder.
     *
     * @param css
     *            the css data found by the scanner
     * @param javascript
     *            the javascript imports found by the scanner
     * @return a map from file nane to the lines for that file
     */
    private Map<File, List<String>> process(Map<ChunkInfo, List<CssData>> css,
            Map<ChunkInfo, List<String>> javascript) {
        getLogger().debug("Start sorting imports to lazy and eager.");
        long start = System.nanoTime();

        Map<File, List<String>> files = new HashMap<>();

        Map<ChunkInfo, List<String>> lazyJavascript = new LinkedHashMap<>();
        List<String> eagerJavascript = new ArrayList<>();
        Map<ChunkInfo, List<String>> lazyCss = new LinkedHashMap<>();
        List<CssData> eagerCssData = new ArrayList<>();
        for (Entry<ChunkInfo, List<String>> entry : javascript.entrySet()) {
            if (isLazyRoute(entry.getKey())) {
                lazyJavascript.put(entry.getKey(), entry.getValue());
            } else {
                eagerJavascript.addAll(entry.getValue());
            }
        }

        for (Entry<ChunkInfo, List<CssData>> entry : css.entrySet()) {
            boolean hasThemeFor = entry.getValue().stream()
                    .anyMatch(cssData -> cssData.getThemefor() != null);
            if (isLazyRoute(entry.getKey()) && !hasThemeFor) {
                List<String> cssLines = getCssLines(entry.getValue());
                if (!cssLines.isEmpty()) {
                    lazyCss.put(entry.getKey(), cssLines);
                }
            } else {
                eagerCssData.addAll(entry.getValue());
            }
        }

        getLogger().debug("Imports sorting took {} ms.",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));

        List<String> chunkLoader = new ArrayList<>();

        if (!lazyJavascript.isEmpty() || !lazyCss.isEmpty()) {
            getLogger().debug("Start generating lazy loaded chunks.");
            start = System.nanoTime();

            chunkLoader.add("");
            chunkLoader.add("const loadOnDemand = (key) => {");
            chunkLoader.add("  const pending = [];");
            Set<ChunkInfo> mergedChunkKeys = merge(lazyJavascript.keySet(),
                    lazyCss.keySet());
            Set<String> processedChunkHashes = new HashSet<>(
                    mergedChunkKeys.size());

            for (ChunkInfo chunkInfo : mergedChunkKeys) {
                List<String> chunkLines = new ArrayList<>();
                if (lazyJavascript.containsKey(chunkInfo)) {
                    chunkLines.addAll(
                            getModuleLines(lazyJavascript.get(chunkInfo)));
                }
                boolean hasLazyCss = lazyCss.containsKey(chunkInfo);
                if (hasLazyCss) {
                    chunkLines.add(IMPORT_INJECT);
                    chunkLines.add(THEMABLE_MIXIN_IMPORT);
                    chunkLines.addAll(lazyCss.get(chunkInfo));
                }

                if (chunkLines.isEmpty()) {
                    continue;
                }

                String chunkContentHash = BundleUtils.getChunkHash(chunkLines);

                String chunkFilename = "chunk-" + chunkContentHash + ".js";

                String ifClauses = chunkInfo.getDependencyTriggers().stream()
                        .map(BundleUtils::getChunkId)
                        .map(hash -> String.format("key === '%s'", hash))
                        .collect(Collectors.joining(" || "));
                chunkLoader.add(String.format("  if (%s) {", ifClauses));
                chunkLoader.add(String.format(
                        "    pending.push(import('./chunks/%s'));",
                        chunkFilename));
                chunkLoader.add("  }");

                boolean chunkNotExist = processedChunkHashes
                        .add(chunkContentHash);
                if (chunkNotExist) {
                    File chunkFile = new File(chunkFolder, chunkFilename);
                    files.put(chunkFile, chunkLines);
                }
            }

            chunkLoader.add("  return Promise.all(pending);");
            chunkLoader.add("}");
            chunkLoader.add("");

            getLogger().debug("Lazy chunks generation took {} ms.",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        } else {
            chunkLoader.add(
                    "const loadOnDemand = (key) => { return Promise.resolve(0); }");
        }

        List<String> mainLines = new ArrayList<>();

        // Convert eager CSS data to JS and deduplicate it
        List<String> mainCssLines = getCssLines(eagerCssData);
        if (!mainCssLines.isEmpty()) {
            mainLines.add(IMPORT_INJECT);
            mainLines.add(THEMABLE_MIXIN_IMPORT);
            mainLines.addAll(mainCssLines);
        }
        mainLines.addAll(getModuleLines(eagerJavascript));

        // Move all imports to the top
        List<String> copy = new ArrayList<>(mainLines);
        copy.removeIf(line -> !line.startsWith("import "));
        mainLines.removeIf(line -> line.startsWith("import "));
        mainLines.addAll(0, copy);

        mainLines.addAll(chunkLoader);
        mainLines.add("window.Vaadin = window.Vaadin || {};");
        mainLines.add("window.Vaadin.Flow = window.Vaadin.Flow || {};");
        mainLines.add("window.Vaadin.Flow.loadOnDemand = loadOnDemand;");
        mainLines.add("window.Vaadin.Flow.resetFocus = " + RESET_FOCUS_JS);

        files.put(generatedFlowImports, mainLines);
        files.put(generatedFlowDefinitions,
                Collections.singletonList("export {}"));

        return files;
    }

    private boolean isLazyRoute(ChunkInfo key) {
        if (key.getType() != EntryPointType.ROUTE) {
            return false;
        }

        return !key.isEager();
    }

    /**
     * Get a resource from the classpath.
     *
     * @param name
     *            class literal
     * @return the resource
     */
    private URL getResource(String name) {
        return classFinder.getResource(name);
    }

    /**
     * Get generated modules to import.
     *
     * @return generated modules
     */
    Collection<String> getGeneratedModules() {
        return NodeUpdater.getGeneratedModules(options.getFrontendDirectory());
    }

    /**
     * Get logger for this instance.
     *
     * @return a logger
     */
    protected abstract Logger getLogger();

    List<String> resolveModules(Collection<String> modules) {
        return modules.stream()
                .filter(module -> !module.startsWith(
                        ApplicationConstants.CONTEXT_PROTOCOL_PREFIX)
                        && !module.startsWith(
                                ApplicationConstants.BASE_PROTOCOL_PREFIX))
                .filter(module -> !UrlUtil.isExternal(module))
                .map(module -> resolveResource(module))
                .collect(Collectors.toList());
    }

    private Collection<? extends String> resolveGeneratedModules(
            Collection<String> generatedModules) {
        return generatedModules.stream()
                .map(module -> resolveGeneratedModule(module))
                .collect(Collectors.toList());
    }

    String resolveGeneratedModule(String module) {
        return FrontendUtils.FRONTEND_GENERATED_FLOW_IMPORT_PATH + module;
    }

    /**
     * Returns the JS code lines representing the CSS data provided as a
     * parameter.
     * <p>
     * Deduplicates the data so that each CSSData instance is loaded / injected
     * only once.
     *
     * @param css
     *            the CSS import data
     * @return the JS statements needed to import and apply the CSS data
     */
    protected List<String> getCssLines(List<CssData> css) {
        List<String> lines = new ArrayList<>();

        Set<String> cssNotFound = new HashSet<>();
        LinkedHashSet<CssData> allCss = new LinkedHashSet<>(css);
        int i = 0;
        for (CssData cssData : allCss) {
            if (!addCssLines(lines, cssData, i)) {
                cssNotFound.add(cssData.getValue());
            }
            i++;
        }

        if (!cssNotFound.isEmpty()) {
            String prefix = String.format(
                    "Failed to find the following css files in the `node_modules` or `%s` directory tree:",
                    options.getFrontendDirectory().getPath());

            String suffix;
            if (options.getTokenFile() == null
                    && !options.getFrontendDirectory().exists()) {
                suffix = "Unable to locate frontend resources and missing token file. "
                        + "Please run the `prepare-frontend` Vaadin plugin goal before deploying the application";
            } else {
                suffix = String.format(
                        "Check that they exist or are installed. If you use a custom directory "
                                + "for your resource files instead of the default `frontend` folder "
                                + "then make sure it's correctly configured (e.g. set '%s' property)",
                        FrontendUtils.PARAM_FRONTEND_DIR);
            }
            throw new IllegalStateException(
                    notFoundMessage(cssNotFound, prefix, suffix));
        }
        return lines;
    }

    protected String resolveResource(String importPath) {
        String resolved = importPath;
        if (!importPath.startsWith("@")) {

            // We only should check here those paths starting with './' when all
            // flow components have the './' prefix
            String resource = STARTING_DOT_SLASH.matcher(resolved)
                    .replaceFirst("");
            if (hasMetaInfResource(resource)) {
                if (!resolved.startsWith("./")) {
                    getLogger().warn(
                            "Use the './' prefix for files in JAR files: '{}', please update your component.",
                            importPath);
                }
                resolved = FrontendUtils.JAR_RESOURCES_IMPORT_FRONTEND_RELATIVE
                        + resource;
            }
        }
        return resolved;
    }

    protected void addLines(Collection<String> lines, String content) {
        lines.addAll(Arrays.asList(content.split("\\R")));
    }

    protected String getThemeIdPrefix() {
        return "flow_css_mod";
    }

    protected abstract String getImportsNotFoundMessage();

    @SafeVarargs
    private Map<ChunkInfo, List<String>> mergeJavascript(
            Map<ChunkInfo, List<String>>... javascripts) {
        Map<ChunkInfo, List<String>> result = new LinkedHashMap<>();
        Collection<? extends String> generated = resolveGeneratedModules(
                getGeneratedModules());
        for (Map<ChunkInfo, List<String>> javascript : javascripts) {

            for (Entry<ChunkInfo, List<String>> entry : javascript.entrySet()) {
                result.computeIfAbsent(entry.getKey(), e -> new ArrayList<>())
                        .addAll(resolveModules(entry.getValue()));
            }
        }

        result.computeIfAbsent(ChunkInfo.GLOBAL, e -> new ArrayList<>())
                .addAll(generated);
        return result;

    }

    protected <T> List<String> merge(Map<T, List<String>> css) {
        List<String> result = new ArrayList<>();
        css.forEach((key, value) -> result.addAll(value));
        return result;
    }

    private Set<ChunkInfo> merge(Set<ChunkInfo> set1, Set<ChunkInfo> set2) {
        Set<ChunkInfo> set = new HashSet<>(set1);
        set.addAll(set2);
        return set;
    }

    private Set<String> getUniqueEs6ImportPaths(Collection<String> modules) {
        Set<String> npmNotFound = new HashSet<>();
        Set<String> resourceNotFound = new HashSet<>();
        Set<String> es6ImportPaths = new LinkedHashSet<>();
        AbstractTheme theme = scanner.getTheme();

        Set<String> visited = new HashSet<>();

        for (String originalModulePath : modules) {
            String translatedModulePath = originalModulePath;
            String localModulePath = null;
            if (theme != null
                    && (originalModulePath.startsWith(theme.getBaseUrl())
                            || originalModulePath
                                    .startsWith("./" + theme.getBaseUrl()))) {
                translatedModulePath = theme.translateUrl(translatedModulePath);
                localModulePath = themeToLocalPathConverter
                        .apply(translatedModulePath);
            }

            if (localModulePath != null
                    && frontendFileExists(localModulePath)) {
                es6ImportPaths.add(toValidBrowserImport(localModulePath));
            } else if (isGeneratedFlowFile(translatedModulePath)) {
                es6ImportPaths.add(translatedModulePath);
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
            String suffix;
            if (options.getTokenFile() == null
                    && !options.getFrontendDirectory().exists()) {
                suffix = "Unable to locate frontend resources and missing token file. "
                        + "Please run the `prepare-frontend` Vaadin plugin goal before deploying the application";
            } else {
                suffix = String.format("%n  Locations searched were:"
                        + "%n      - `%s` in this project"
                        + "%n      - `%s` in included JARs"
                        + "%n      - `%s` in included JARs"
                        + "%n%n  Please, double check that those files exist. If you use a custom directory "
                        + "for your resource files instead of default "
                        + "`frontend` folder then make sure you it's correctly configured "
                        + "(e.g. set '%s' property)",
                        options.getFrontendDirectory().getPath(),
                        Constants.RESOURCES_FRONTEND_DEFAULT,
                        COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT,
                        FrontendUtils.PARAM_FRONTEND_DIR);
            }
            if (inMemoryCollection()) {
                es6ImportPaths.addAll(resourceNotFound);
                return es6ImportPaths;
            }
            throw new IllegalStateException(
                    notFoundMessage(resourceNotFound, prefix, suffix));
        }

        boolean needsNodeModules = options.isFrontendHotdeploy()
                || options.isBundleBuild();
        if (!npmNotFound.isEmpty() && getLogger().isInfoEnabled()
                && needsNodeModules) {
            getLogger().info(notFoundMessage(npmNotFound,
                    "Failed to find the following imports in the `node_modules` tree:",
                    getImportsNotFoundMessage()));
        }
        return es6ImportPaths;
    }

    private static UnaryOperator<String> createThemeToLocalPathConverter(
            AbstractTheme theme) {
        UnaryOperator<String> convertToLocalPath;
        if (theme != null) {
            // (#5964) Allows:
            // - custom @Theme with files placed in /frontend
            // - customize an already themed component
            // @vaadin/vaadin-grid/theme/lumo/vaadin-grid.js ->
            // theme/lumo/vaadin-grid.js
            String themePath = theme.getThemeUrl();
            Pattern themePattern = Pattern.compile("@.+" + themePath);
            convertToLocalPath = path -> themePattern.matcher(path)
                    .replaceFirst(themePath);
        } else {
            convertToLocalPath = UnaryOperator.identity();
        }
        return convertToLocalPath;
    }

    private boolean isGeneratedFlowFile(String localModulePath) {
        return localModulePath
                .startsWith(FrontendUtils.FRONTEND_GENERATED_FLOW_IMPORT_PATH);
    }

    /**
     * If in memory collection we are collecting for devBundle check.
     *
     * @return {@code true} if devBundle in memory collecting
     */
    protected boolean inMemoryCollection() {
        return false;
    }

    private List<String> getModuleLines(Collection<String> modules) {
        return getUniqueEs6ImportPaths(modules).stream()
                .map(path -> String.format(IMPORT_TEMPLATE, path))
                .collect(Collectors.toList());
    }

    private boolean frontendFileExists(String jsImport) {
        File file = getFile(options.getFrontendDirectory(), jsImport);
        return file.exists();
    }

    /**
     * Validate that the file {@code importName} can be found.
     *
     * @param importName
     *            name of the file
     * @return {@code true} if file is found
     */
    protected boolean importedFileExists(String importName) {
        File file = getImportedFrontendFile(importName);
        if (file != null) {
            return true;
        }

        // full path import e.g
        // /node_modules/@vaadin/vaadin-grid/vaadin-grid-column.js
        boolean found = isFile(options.getNodeModulesFolder(), importName);
        if (importName.toLowerCase().endsWith(".css")) {
            return found;
        }
        // omitted the .js extension e.g.
        // /node_modules/@vaadin/vaadin-grid/vaadin-grid-column
        found = found
                || isFile(options.getNodeModulesFolder(), importName + ".js");
        // has a package.json file e.g. /node_modules/package-name/package.json
        found = found || isFile(options.getNodeModulesFolder(), importName,
                PACKAGE_JSON);

        return found;
    }

    /**
     * Returns a file for the {@code jsImport} path ONLY if it's either in the
     * {@code frontend} or {@value FrontendUtils#JAR_RESOURCES_IMPORT} folder.
     * <p>
     * This method doesn't care about "published" WC paths (like
     * "@vaadin/vaadin-grid" and so on). See the
     * {@link #importedFileExists(String)} method implementation.
     *
     * @return a file on FS if it exists and it's inside the frontend folder or
     *         the jar resources folder, otherwise returns {@code null}
     */
    private File getImportedFrontendFile(String jsImport) {
        // file is in /frontend
        File file = getJsImportFile(options.getFrontendDirectory(), jsImport);
        if (file.exists()) {
            return file;
        }
        // file is a flow resource e.g.
        // Frontend/generated/addons/gridConnector.js
        file = getJsImportFile(getJarResourcesFolder(), jsImport);

        return file.exists() ? file : null;
    }

    private File getJarResourcesFolder() {
        return new File(options.getFrontendGeneratedFolder(),
                FrontendUtils.JAR_RESOURCES_FOLDER);
    }

    private File getJsImportFile(File base, String path) {
        File file = getFile(base, path);

        if (file.isDirectory()) {
            // import './foo' seems to be valid according to tools when 'foo' is
            // a directory. What is imported is 'foo/index.js'.
            file = new File(file, "index.js");
        }
        return file;
    }

    private File getFile(File base, String... path) {
        return new File(base, String.join("/", path));
    }

    private boolean isFile(File base, String... path) {
        return getFile(base, path).isFile();
    }

    private boolean isFileOrDirectory(File base, String... path) {
        File file = getFile(base, path);
        return file.isFile() || file.isDirectory();
    }

    /**
     * Adds CSS imports to the generated flow imports file based on the given
     * CssImport data.
     *
     * @param lines
     *            collection of generated file lines to add imports to
     * @param cssData
     *            CssImport data
     * @param i
     *            imported CSS counter
     * @return true if the imported CSS files does exist, false otherwise
     */
    protected boolean addCssLines(Collection<String> lines, CssData cssData,
            int i) {
        String cssFile = resolveResource(cssData.getValue());
        boolean found = importedFileExists(cssFile);
        String cssImport = toValidBrowserImport(cssFile);
        // Without this, Vite adds the CSS also to the document
        cssImport += "?inline";

        Map<String, String> optionalsMap = new LinkedHashMap<>();
        if (cssData.getInclude() != null) {
            optionalsMap.put("include", cssData.getInclude());
        }
        if (cssData.getId() != null && cssData.getThemefor() != null) {
            throw new IllegalStateException(
                    "provide either id or themeFor for @CssImport of resource "
                            + cssData.getValue() + ", not both");
        }
        if (cssData.getId() != null) {
            optionalsMap.put("moduleId", cssData.getId());
        } else if (cssData.getThemefor() != null) {
            optionalsMap.put("moduleId", getThemeIdPrefix() + "_" + i);
        }
        String optionals = "";
        if (!optionalsMap.isEmpty()) {
            optionals = ", " + optionalsMap.keySet().stream()
                    .map(k -> k + ": '" + optionalsMap.get(k) + "'")
                    .collect(Collectors.joining(", ", "{", "}"));
        }

        if (cssData.getThemefor() != null || cssData.getId() != null) {
            String themeFor = cssData.getThemefor() != null
                    ? cssData.getThemefor()
                    : "";
            addLines(lines, String.format(REGISTER_STYLES_FOR_TEMPLATE, i,
                    cssImport, themeFor, optionals));
        } else if (cssData.getInclude() != null) {
            if (!lines.contains("function addCssBlock(block) {")) {
                addLines(lines, CSS_PREPARE);
            }
            String include = cssData.getInclude() != null
                    ? " include=\"" + cssData.getInclude() + "\""
                    : "";
            addLines(lines,
                    String.format(CSS_BASIC_TPL, i, cssImport, include));
        } else {
            addLines(lines, String.format(INJECT_CSS, i, cssImport));
        }
        return found || !options.isBundleBuild();
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
        if (isFileOrDirectory(options.getFrontendDirectory(), jsImport)) {
            if (!jsImport.startsWith("./")) {
                getLogger().warn(
                        "Use the './' prefix for files in the '{}' folder: '{}', please update your annotations.",
                        options.getFrontendDirectory(), jsImport);
            }
            return FRONTEND_FOLDER_ALIAS
                    + STARTING_DOT_SLASH.matcher(jsImport).replaceFirst("");
        }
        return jsImport;
    }

    private void visitImportsRecursively(Path filePath, String path,
            AbstractTheme theme, Collection<String> imports,
            Set<String> visitedImports) throws IOException {

        if (!resolvedImportPathsCache.containsKey(filePath)) {
            String content = null;
            try (final Stream<String> contentStream = Files.lines(filePath,
                    StandardCharsets.UTF_8)) {
                content = contentStream.collect(Collectors.joining("\n"));
            } catch (UncheckedIOException ioe) {
                if (ioe.getCause() instanceof MalformedInputException) {
                    getLogger().trace(
                            "Failed to read file '{}' found from Es6 import statements. "
                                    + "This is probably due to it being a binary file, "
                                    + "in which case it doesn't matter as imports are only in js/ts files.",
                            filePath.toString(), ioe);
                    return;
                }
                throw ioe;
            }
            ImportExtractor extractor = new ImportExtractor(content);
            resolvedImportPathsCache.put(filePath,
                    extractor.getImportedPaths().stream().map(importedPath -> {
                        // try to resolve path relatively to original filePath
                        // (inside user
                        // frontend folder)
                        importedPath = StringUtil.stripSuffix(importedPath,
                                "?inline");
                        String resolvedPath = resolve(importedPath, filePath,
                                path);
                        File file = getImportedFrontendFile(resolvedPath);
                        if (file == null && !importedPath.startsWith("./")) {
                            // In case such file doesn't exist it may be
                            // external: inside node_modules folder
                            file = getFile(options.getNodeModulesFolder(),
                                    importedPath);
                            if (!file.exists()) {
                                file = null;
                            }
                            resolvedPath = importedPath;
                        }
                        if (file == null) {
                            // don't do anything if such file doesn't exist at
                            // all
                            return null;
                        }
                        return normalizePath(resolvedPath);
                    }).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        List<String> resolvedPaths = resolvedImportPathsCache.get(filePath);

        for (String resolvedPath : resolvedPaths) {
            if (resolvedPath.startsWith(theme.getBaseUrl())
                    || resolvedPath.startsWith("./" + theme.getBaseUrl())) {
                String translatedPath = theme.translateUrl(resolvedPath);
                if (!visitedImports.contains(translatedPath)
                        && importedFileExists(translatedPath)) {
                    visitedImports.add(translatedPath);
                    imports.add(normalizeImportPath(translatedPath));
                }
            } else {
                visitedImports.add(resolvedPath);
                imports.add(normalizeImportPath(resolvedPath));
            }
            handleImports(resolvedPath, theme, imports, visitedImports);
        }
    }

    void handleImports(String path, AbstractTheme theme,
            Collection<String> imports, Set<String> visitedImports) {
        if (visitedImports.contains(path)) {
            return;
        }
        File file = getImportedFrontendFile(path);
        if (file == null) {
            return;
        }
        Path filePath = file.toPath();
        String normalizedPath = filePath.normalize().toString().replace("\\",
                "/");
        if (!visitedImports.add(normalizedPath)) {
            return;
        }
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
        int pathLength = path.length();
        // path may have been resolved as `path/index.js`, if path points to a
        // directory
        if (!moduleFile.endsWith(path) && moduleFile.endsWith("index.js")) {
            pathLength += 9;
        }
        pathPrefix = pathPrefix.substring(0, pathPrefix.length() - pathLength);
        try {
            String resolvedPath = moduleFile.getParent().resolve(importedPath)
                    .toString();
            if (resolvedPath.startsWith(pathPrefix)) {
                resolvedPath = resolvedPath.substring(pathPrefix.length());
            }
            return resolvedPath;
        } catch (InvalidPathException ipe) {
            getLogger().error("Invalid import '{}' in file '{}'", importedPath,
                    moduleFile);
            getLogger().debug("Failed to resolve path.", ipe);
        }
        return importedPath;
    }

    private String normalizePath(String path) {
        File file = new File(path);
        return file.toPath().normalize().toString().replace("\\", "/");
    }

    private String normalizeImportPath(String path) {
        return toValidBrowserImport(normalizePath(path));
    }

}
