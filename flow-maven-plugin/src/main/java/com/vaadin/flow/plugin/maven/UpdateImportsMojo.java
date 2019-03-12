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

import static com.vaadin.flow.plugin.maven.UpdateNpmDependenciesMojo.getHtmlImportJsModules;
import static com.vaadin.flow.plugin.maven.UpdateNpmDependenciesMojo.getProjectClassPathUrls;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.FileUtils;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.plugin.common.AnnotationValuesExtractor;
import com.vaadin.flow.plugin.common.FlowPluginFileUtils;
import com.vaadin.flow.theme.ThemeDefinition;


/**
 * Goal that updates flow-imports.js file with @JsModule and @HtmlImport
 * annotations defined in the classpath.
 */
@Mojo(name = "update-imports", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class UpdateImportsMojo extends AbstractMojo {

    private static final String VALUE = "value";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Name of the JavaScript file to update.
     */
    @Parameter(defaultValue = "webapp/frontend/main.js")
    private String jsFile;

    /**
     * Enable or disable legacy components annotated only with
     * {@link HtmlImport}.
     */
    @Parameter(defaultValue = "true")
    private boolean convertHtml;

    @Override
    public void execute() {

        URL[] projectClassPathUrls = getProjectClassPathUrls(project);

        getLog().info("Looking for imports ...");

        AnnotationValuesExtractor annotationValuesExtractor = new AnnotationValuesExtractor(projectClassPathUrls);

        ThemeDefinition themeDefinition = getThemeDefinition(annotationValuesExtractor);

        Map<Class<?>, Set<String>> classesWithJsModule = annotationValuesExtractor.getAnnotatedClasses(JsModule.class,
                VALUE);

        final Set<String> themeModules = annotationValuesExtractor.getClassAnnotationValues(themeDefinition.getTheme(),
                JsModule.class, VALUE);

        Map<Class<?>, Set<String>> classes = new HashMap<>(classesWithJsModule);

        if (convertHtml) {
            Map<Class<?>, Set<String>> classesWithHtmlImport = annotationValuesExtractor
                    .getAnnotatedClasses(HtmlImport.class, VALUE);

            if (themeModules.isEmpty()) {
                themeModules.addAll(getHtmlImportJsModules(annotationValuesExtractor
                        .getClassAnnotationValues(themeDefinition.getTheme(), HtmlImport.class, VALUE)));
            }

            classesWithHtmlImport = classesWithHtmlImport.entrySet().stream()
                    .filter(entry -> !classesWithJsModule.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(Entry::getKey, entry -> getHtmlImportJsModules(entry.getValue())));

            classes.putAll(classesWithHtmlImport);
        }

        try {
            List<String> current = FileUtils.fileExists(jsFile) ? FileUtils.loadFile(new File(jsFile))
                    : Collections.emptyList();

            // It's not possible to cast to AbstractTheme here as a type since it's load by the different classloader.
            // Thus we use reflection here to invoke those methods in the different context.
            Object themeInstance = annotationValuesExtractor
                    .loadClassInProjectClassLoader(themeDefinition.getTheme().getCanonicalName()).newInstance();

            Map<String, String> htmlAttributes = annotationValuesExtractor.doInvokeMethod(themeInstance,
                    "getHtmlAttributes", themeDefinition.getVariant());
            List<String> headerContents = annotationValuesExtractor.doInvokeMethod(themeInstance, "getHeaderInlineContents");

            String baseUrl = annotationValuesExtractor.doInvokeMethod(themeInstance, "getBaseUrl");

            Set<String> jsModules = new HashSet<>();
            classes.entrySet().stream().forEach(entry -> entry.getValue().forEach(fileName -> jsModules.add(
                    // add `./` prefix to everything starting with letters
                    fileName.replaceFirst("(?i)^([a-z])", "./$1"))));

            List<String> themeConfig = new ArrayList<>();
            if (!headerContents.isEmpty()) {
                themeConfig.add("const div = document.createElement('div');");
                headerContents.forEach(html -> {
                    themeConfig.add("div.innerHTML = '" + html.replaceAll("(?m)(^\\s+|\\s?\n)", "") + "';");
                    themeConfig.add("document.head.insertBefore(div.firstElementChild, document.head.firstChild);");
                });
            }
            htmlAttributes.entrySet().forEach(
                    e -> themeConfig.add("document.body.setAttribute('" + e.getKey() + "', '" + e.getValue() + "');"));

            Stream<String> themeStream = themeModules.stream().map(s -> "import '" + s + "';");

            Stream<String> importStream = jsModules.stream().sorted(Comparator.reverseOrder())
                    // Do not repeat modules already defined in theme
                    .filter(fileName -> !themeModules.contains(fileName))
                    .map(fileName -> {
                        // TODO(manolo): disabled because not all files have corresponding themed one.
                        // eg vaadin-upload/src/vaadin-upload.js and vaadin-upload/theme/lumo/vaadin-upload.js exist
                        // but vaadin-upload/src/vaadin-upload-file.js does not.
                        // ticket: https://github.com/vaadin/flow/issues/5244
                        if (fileName.matches(".*(vaadin-[^/]+)/" + baseUrl + "\\1\\.(js|html)")) {
                            fileName = annotationValuesExtractor.doInvokeMethod(themeInstance, "translateUrl",
                                    fileName);
                        }
                        return "import '" + fileName + "';";
                    });

            List<String> concat = Stream.concat(Stream.concat(themeConfig.stream(), themeStream), importStream)
                    .collect(Collectors.toList());

            if (concat.equals(current)) {
                getLog().info("No js modules to update");
            } else {
                String content = concat.stream().collect(Collectors.joining("\n"));
                replaceJsFile(content + "\n");
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ThemeDefinition getThemeDefinition(AnnotationValuesExtractor annotationValuesExtractor) {
        Map<ThemeDefinition, Class<?>> themes = annotationValuesExtractor.getThemeDefinitions();

        if (themes.size() > 1) {
            getLog().warn(
                    "Found multiple themes for the application, vaadin-flow would only consider the first one\n"
                            + themes.entrySet().stream()
                                    .map(e -> "   theme:" + e.getKey().getTheme().getName() + " "
                                            + e.getKey().getVariant() + " in class: " + e.getValue().getName())
                                    .collect(Collectors.joining("\n")));
        }

        ThemeDefinition themeDef = themes.keySet().iterator().next();
        getLog().info("Using theme " + themeDef.getTheme().getName()
                + (themeDef.getVariant().isEmpty() ? "" : (" variant: " + themeDef.getVariant())));
        return themeDef;
    }

    private void replaceJsFile(String content) throws IOException {
        File out = new File(jsFile);
        if (out.getParentFile() != null && !out.getParentFile().exists()) {
            getLog().info("creating Folder: " + out.getParentFile().getAbsolutePath());
            FlowPluginFileUtils.forceMkdir(out.getParentFile());
        }
        getLog().info("Updating JS imports to file: " + out.getAbsolutePath());

        if (out.canWrite() || out.createNewFile()) {
            Files.write(Paths.get(out.toURI()), content.getBytes("UTF-8"));
        }

    }
}
