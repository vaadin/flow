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
package com.vaadin.flow.plugin.maven;

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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.utils.io.FileUtils;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.plugin.common.AnnotationValuesExtractor;
import com.vaadin.flow.plugin.common.FlowPluginFileUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.plugin.common.AnnotationValuesExtractor.LUMO;


/**
 * Goal that updates flow-imports.js file with @JsModule and @HtmlImport
 * annotations defined in the classpath.
 */
@Mojo(name = "update-imports", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class UpdateImportsMojo extends AbstractNpmMojo {
    private static final String VALUE = "value";

    /**
     * A Flow JavaScript file with all project's imports to update.
     */
    @Parameter(defaultValue = "${project.basedir}/frontend/main.js")
    private File jsFile;

    /**
     * Enable or disable legacy components annotated only with
     * {@link HtmlImport}.
     */
    @Parameter(defaultValue = "true")
    private boolean convertHtml;

    private AnnotationValuesExtractor annotationValuesExtractor;
    private ThemeDefinition themeDefinition;

    @Override
    public void execute() {

        // Do nothing when bower mode
        if (Boolean.getBoolean("vaadin." + Constants.SERVLET_PARAMETER_BOWER_MODE)) {
            log.info("Skipped `update-imports` goal because `vaadin.bowerMode` is set.");
            return;
        }

        log.info("Looking for imports in the java class-path ...");
        if (annotationValuesExtractor == null) {
            annotationValuesExtractor = new AnnotationValuesExtractor(getProjectClassPathUrls(project));
        }
        if (themeDefinition == null) {
            themeDefinition = getThemeDefinition(annotationValuesExtractor);
        }

        // Using LinkedHashSet to maintain theme imports sorted at top
        Set<String> modules = new LinkedHashSet<>();
        modules.addAll(getThemeModules());
        modules.addAll(getJsModules());
        modules.addAll(getJavaScriptFiles());

        try {
            updateMainJsFile(getMainJsContent(modules));
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to update the Flow imports file '%s'", jsFile), e);
        }
    }

    private List<String> getMainJsContent(Set<String> modules) throws InstantiationException, IllegalAccessException  {
        List<String> lines = new ArrayList<>();

        Object theme = null;
        if (themeDefinition != null) {
            // It's not possible to cast to AbstractTheme as a type here since it's loaded by different classloader.
            // Thus we use reflection to invoke those methods in the different context.
            theme = annotationValuesExtractor
                    .loadClassInProjectClassLoader(themeDefinition.getTheme().getCanonicalName()).newInstance();

            Map<String, String> htmlAttributes = annotationValuesExtractor.doInvokeMethod(theme,
                    "getHtmlAttributes", themeDefinition.getVariant());
            List<String> headerContents = annotationValuesExtractor.doInvokeMethod(theme,
                    "getHeaderInlineContents");
            if (!headerContents.isEmpty()) {
                lines.add("const div = document.createElement('div');");
                headerContents.forEach(html -> {
                    lines.add("div.innerHTML = '" + html.replaceAll("(?m)(^\\s+|\\s?\n)", "") + "';");
                    lines.add("document.head.insertBefore(div.firstElementChild, document.head.firstChild);");
                });
            }
            htmlAttributes.forEach((key, value) -> lines.add("document.body.setAttribute('" + key + "', '" + value + "');"));
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
                // to-do(manolo): disabled for certain files because not all files have corresponding themed one.
                // e.g. vaadin-upload/src/vaadin-upload.js and vaadin-upload/theme/lumo/vaadin-upload.js exist
                // but vaadin-upload/src/vaadin-upload-file.js does not.
                // ticket: https://github.com/vaadin/flow/issues/5244
                if (translatedModulePath.matches(".*(vaadin-[^/]+)/" + baseUrl + "\\1\\.(js|html)")) {
                    translatedModulePath = annotationValuesExtractor.doInvokeMethod(theme, "translateUrl",
                        translatedModulePath);
                }
            }
            if (importedFileExists(translatedModulePath)) {
                imports.add("import '" + translatedModulePath + "';");
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

        return classes.values().stream().flatMap(Collection::stream)
                .map(jsModule ->
                // add `./` prefix to names starting with letters
                jsModule.replaceFirst("(?i)^([a-z])", "./$1"))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Collection<? extends String> getJavaScriptFiles() {
        return annotationValuesExtractor.getAnnotatedClasses(JavaScript.class, VALUE).values().stream()
            .flatMap(Collection::stream)
            .map(this::resolveInFlowFrontendDirectory)
            .map(javaScriptPath ->
                // add `./` prefix to names starting with letters
                javaScriptPath.replaceFirst("(?i)^([a-z])", "./$1"))
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toCollection(LinkedHashSet::new));
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
        Map<ThemeDefinition, Class<?>> themes = annotationValuesExtractor.getThemeDefinitions();

        if (themes.isEmpty()) {
           log.warn("No theme found for the app nor " + LUMO + " class found in the classpath");
           return null;
        }

        if (themes.size() > 1) {
            log.warn(
                    "Found multiple themes for the application, vaadin-flow would only consider the first one\n"
                            + themes.entrySet().stream()
                                    .map(e -> "   theme:" + e.getKey().getTheme().getName() + " "
                                            + e.getKey().getVariant() + " in class: " + e.getValue().getName())
                                    .collect(Collectors.joining("\n")));
        }


        ThemeDefinition themeDef = themes.keySet().iterator().next();
        log.info("Using theme " + themeDef.getTheme().getName()
                + (themeDef.getVariant().isEmpty() ? "" : (" variant: " + themeDef.getVariant())));
        return themeDef;
    }

    private void updateMainJsFile(List<String> newContent) throws IOException {
        List<String> oldContent = FileUtils.loadFile(jsFile);
        if (newContent.equals(oldContent)) {
            log.info("No js modules to update");
        } else {
            FlowPluginFileUtils.forceMkdir(jsFile.getParentFile());
            FileUtils.fileWrite(jsFile, "UTF-8", String.join("\n", newContent));
            log.info("Updated " + jsFile);
        }
    }
}
