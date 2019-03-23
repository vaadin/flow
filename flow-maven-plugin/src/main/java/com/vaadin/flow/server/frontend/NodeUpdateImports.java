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
public class NodeUpdateImports extends NodeUpdater {
    private static final String VALUE = "value";
    private static final String MAIN_JS = "frontend/main.js";

    /**
     * Name of the JavaScript file to update.
     */
    @Parameter
    private String jsFile;

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

        if (jsFile == null || jsFile.isEmpty()) {
            jsFile = project.getBasedir() + "/" + MAIN_JS;
        }
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
            throw new IllegalStateException(e);
        }
    }

    private List<String> getMainJsContent(Set<String> modules) throws InstantiationException, IllegalAccessException  {
        List<String> lines = new ArrayList<>();
        if (themeDefinition == null) {
            modules.forEach(module -> lines.add("import '" + module + "';"));
        } else {
            // It's not possible to cast to AbstractTheme as a type here since it's loaded by different classloader.
            // Thus we use reflection to invoke those methods in the different context.
            Object theme = annotationValuesExtractor
                    .loadClassInProjectClassLoader(themeDefinition.getTheme().getCanonicalName()).newInstance();

            Map<String, String> htmlAttributes = annotationValuesExtractor.doInvokeMethod(theme,
                    "getHtmlAttributes", themeDefinition.getVariant());
            List<String> headerContents = annotationValuesExtractor.doInvokeMethod(theme,
                    "getHeaderInlineContents");
            String baseUrl = annotationValuesExtractor.doInvokeMethod(theme, "getBaseUrl");

            if (!headerContents.isEmpty()) {
                lines.add("const div = document.createElement('div');");
                headerContents.forEach(html -> {
                    lines.add("div.innerHTML = '" + html.replaceAll("(?m)(^\\s+|\\s?\n)", "") + "';");
                    lines.add("document.head.insertBefore(div.firstElementChild, document.head.firstChild);");
                });
            }
            htmlAttributes.forEach((key, value) -> lines.add("document.body.setAttribute('" + key + "', '" + value + "');"));

            modules.forEach(module -> {
                // to-do(manolo): disabled for certain files because not all files have corresponding themed one.
                // e.g. vaadin-upload/src/vaadin-upload.js and vaadin-upload/theme/lumo/vaadin-upload.js exist
                // but vaadin-upload/src/vaadin-upload-file.js does not.
                // ticket: https://github.com/vaadin/flow/issues/5244
                if (module.matches(".*(vaadin-[^/]+)/" + baseUrl + "\\1\\.(js|html)")) {
                    module = annotationValuesExtractor.doInvokeMethod(theme, "translateUrl",
                            module);
                }
                lines.add("import '" + module + "';");
            });
        }
        return lines;
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
        List<String> oldContent = FileUtils.loadFile(new File(jsFile));
        if (newContent.equals(oldContent)) {
            log.info("No js modules to update");
        } else {
            File out = new File(jsFile);
            FlowPluginFileUtils.forceMkdir(out.getParentFile());
            FileUtils.fileWrite(out, "UTF-8", String.join("\n", newContent));
            log.info("Updated " + jsFile);
        }
    }
}
