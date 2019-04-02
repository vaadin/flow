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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * An updater that it's run when the servlet context is initialised in dev-mode
 * or when flow-maven-plugin goals are run in order to update
 * <code>main.js</code> and <code>node_module/@vaadin/flow-frontend</code>
 * contents by visiting all classes with {@link JsModule} {@link HtmlImport} and
 * {@link Theme} annotations.
 */
public class NodeUpdateImports extends NodeUpdater {
    /**
     * File to be updated with imports, javascript, and theme annotations.
     * It is also the entry-point for webpack.
     */
    public static final String MAIN_JS = "frontend/main.js";
    private static final String MAIN_JS_PARAM = "vaadin.frontend.jsFile";

    private static final String LUMO = "com.vaadin.flow.theme.lumo.Lumo";
    private static final String VALUE = "value";

    private final File jsFile;

    private final ThemeDefinition themeDefinition;

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param extractor
     *            a reusable annotation extractor
     * @param jsFile
     *            name of the JS file to update with the imports
     * @param npmFolder
     *            folder with the `package.json` file
     * @param nodeModulesPath
     *            the path to the {@literal node_modules} directory of the project
     * @param convertHtml
     *            true to enable polymer-2 annotated classes to be considered
     */
    public NodeUpdateImports(AnnotationValuesExtractor extractor, File jsFile, File npmFolder,
                             File nodeModulesPath, boolean convertHtml) {
        this.annotationValuesExtractor = extractor;
        this.npmFolder = npmFolder;
        this.nodeModulesPath = nodeModulesPath;
        this.jsFile = jsFile;
        this.convertHtml = convertHtml;
        this.themeDefinition = getThemeDefinition(annotationValuesExtractor);
    }

    /**
     * Create an instance of the updater given the reusable extractor, the rest
     * of the configurable parameters will be set to their default values.
     *
     * @param extractor
     *            a reusable annotation extractor
     */
    public NodeUpdateImports(AnnotationValuesExtractor extractor) {
        this(extractor, new File(System.getProperty(MAIN_JS_PARAM, MAIN_JS)),
                new File("."), new File("./node_modules/"), true);
    }

    @Override
    public void execute() {
        // Using LinkedHashSet to maintain theme imports sorted at top
        Set<String> modules = new LinkedHashSet<>();
        modules.addAll(getThemeModules());
        modules.addAll(getJsModules());
        modules.addAll(getJavaScriptFiles());

        try {
            installFlowModules();
            updateMainJsFile(getMainJsContent(modules));
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to update the Flow imports file '%s'", jsFile), e);
        }
    }

    private List<String> getMainJsContent(Set<String> modules) throws InstantiationException, IllegalAccessException {
        List<String> lines = new ArrayList<>();
        Object theme = null;
        if (themeDefinition != null) {
            // It's not possible to cast to AbstractTheme as a type here since
            // it's loaded by different classloader.
            // Thus we use reflection to invoke those methods in the different
            // context.
            theme = annotationValuesExtractor
                    .loadClassInProjectClassLoader(themeDefinition.getTheme().getName()).newInstance();

            Map<String, String> htmlAttributes = annotationValuesExtractor.doInvokeMethod(theme, "getHtmlAttributes",
                    themeDefinition.getVariant());
            List<String> headerContents = annotationValuesExtractor.doInvokeMethod(theme, "getHeaderInlineContents");

            if (!headerContents.isEmpty()) {
                lines.add("const div = document.createElement('div');");
                headerContents.forEach(html -> {
                    lines.add("div.innerHTML = '" + html.replaceAll("(?m)(^\\s+|\\s?\n)", "") + "';");
                    lines.add("document.head.insertBefore(div.firstElementChild, document.head.firstChild);");
                });
            }
            htmlAttributes
                    .forEach((key, value) -> lines.add("document.body.setAttribute('" + key + "', '" + value + "');"));
        }

        lines.addAll(modulesToImports(modules, theme));
        return lines;
    }

    private List<String> modulesToImports(Set<String> modules, Object theme) {
        List<String> imports = new ArrayList<>(modules.size());
        Map<String, String> unresolvedImports = new HashMap<>(modules.size());

        for (String originalModulePath : modules) {
            String translatedModulePath = originalModulePath;
            if (theme != null) {
                String baseUrl = annotationValuesExtractor.doInvokeMethod(theme, "getBaseUrl");
                if (translatedModulePath.matches(baseUrl)) {
                    translatedModulePath = annotationValuesExtractor.doInvokeMethod(theme, "translateUrl",
                        translatedModulePath);
                }
            }
            if (importedFileExists(translatedModulePath)) {
                imports.add("import '" + translatedModulePath + "';");

            } else if (importedFileExists(originalModulePath)) {
                imports.add("import '" + originalModulePath + "';");

            } else {
                unresolvedImports.put(originalModulePath, translatedModulePath);
            }
        }

        if (!unresolvedImports.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder(String.format(
                "Failed to resolve the following module imports neither in the node_modules directory '%s' " +
                    "nor in project files: ",
                nodeModulesPath)).append("\n");

            unresolvedImports
                .forEach((originalModulePath, translatedModulePath) -> {
                    errorMessage.append(
                        String.format("'%s'", translatedModulePath));
                    if (!Objects.equals(originalModulePath,
                        translatedModulePath)) {
                        errorMessage.append(String.format(
                            " (the import was translated by Flow from the path '%s')",
                            originalModulePath));
                    }
                    errorMessage.append("\n");
                });

            errorMessage.append("Double check that those files exist in the project structure.");

            throw new IllegalStateException(errorMessage.toString());
        }

        return imports;
    }

    private boolean importedFileExists(String jsImport) {
        if (jsImport.startsWith("./")) {
            return new File(jsFile.getParentFile(), jsImport).isFile();
        } else {
            return new File(nodeModulesPath, jsImport).isFile();
        }
    }

    private Set<String> getThemeModules() {
        if (themeDefinition == null) {
            return new HashSet<>();
        }
        Set<String> modules = annotationValuesExtractor.getClassAnnotationValues(themeDefinition.getTheme(),
                JsModule.class, VALUE);
        if (modules.isEmpty() && convertHtml) {
            modules = getHtmlImportJsModules(annotationValuesExtractor
                    .getClassAnnotationValues(themeDefinition.getTheme(), HtmlImport.class, VALUE));
        }
        return modules;
    }

    private Set<String> getJsModules() {
        Map<Class<?>, Set<String>> classes = new HashMap<>();
        addClassesWithJsModules(classes);
        addClassesWithHtmlImports(classes);

        return classes.values().stream().flatMap(Collection::stream).map(this::toValidBrowserImport)
                .sorted(Comparator.reverseOrder()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Collection<? extends String> getJavaScriptFiles() {
        return annotationValuesExtractor.getAnnotatedClasses(JavaScript.class, VALUE).values().stream()
                .flatMap(Collection::stream).map(this::resolveInFlowFrontendDirectory).map(this::toValidBrowserImport)
                .sorted(Comparator.reverseOrder()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String toValidBrowserImport(String s) {
        // add `./` prefix to names starting with letters
        return s.replaceFirst("(?i)^([a-z])", "./$1");
    }

    private void addClassesWithJsModules(Map<Class<?>, Set<String>> classes) {
        classes.putAll(annotationValuesExtractor.getAnnotatedClasses(JsModule.class, VALUE));
    }

    private void addClassesWithHtmlImports(Map<Class<?>, Set<String>> classes) {
        if (convertHtml) {
            Map<Class<?>, Set<String>> classesWithHtmlImport = annotationValuesExtractor
                    .getAnnotatedClasses(HtmlImport.class, VALUE);

            classesWithHtmlImport = classesWithHtmlImport.entrySet().stream()
                    .filter(entry -> !classes.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(Entry::getKey, entry -> getHtmlImportJsModules(entry.getValue())));
            classes.putAll(classesWithHtmlImport);
        }
    }

    private ThemeDefinition getThemeDefinition(AnnotationValuesExtractor annotationValuesExtractor) {
        Map<ThemeDefinition, Class<?>> themes = annotationValuesExtractor.getAppThemeOrDefault(LUMO);

        if (themes.isEmpty()) {
            log().warn("No theme found for the app nor {} class found in the classpath.", LUMO);
            return null;
        }

        if (themes.size() > 1 && log().isWarnEnabled()) {
            log().warn("Found multiple themes for the application, vaadin-flow would only consider the first one\n{}",
                    themes.entrySet().stream().map(e -> "   theme:" + e.getKey().getTheme().getName() + " "
                            + e.getKey().getVariant() + " in class: " + e.getValue().getName())
                            .collect(Collectors.joining("\n")));
        }

        ThemeDefinition themeDef = themes.keySet().iterator().next();
        log().info("Using theme {} {}", themeDef.getTheme().getName(),
                (themeDef.getVariant().isEmpty() ? "" : (" variant: " + themeDef.getVariant())));
        return themeDef;
    }

    private void updateMainJsFile(List<String> newContent) throws IOException {
        List<String> oldContent = jsFile.exists() ? FileUtils.readLines(jsFile, "UTF-8") : null;
        if (newContent.equals(oldContent)) {
            log().info("No js modules to update");
        } else {
            FileUtils.forceMkdir(jsFile.getParentFile());
            FileUtils.writeStringToFile(jsFile, String.join("\n", newContent), "UTF-8");
            log().info("Updated {}", jsFile);
        }
    }

}
