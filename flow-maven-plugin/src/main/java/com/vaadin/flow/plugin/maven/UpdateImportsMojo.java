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
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.plugin.common.AnnotationValuesExtractor;
import com.vaadin.flow.plugin.common.FlowPluginFileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import static com.vaadin.flow.plugin.maven.UpdateNpmDependenciesMojo.getHtmlImportNpmPackages;
import static com.vaadin.flow.plugin.maven.UpdateNpmDependenciesMojo.getProjectClassPathUrls;

/**
 * Goal that updates flow-imports.js file with @JsModule and @HtmlImport
 * annotations defined in the classpath.
 */
@Mojo(name = "update-imports", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class UpdateImportsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Name of the JavaScript file to update.
     */
    @Parameter(defaultValue = "src/main/webapp/frontend/main.js")
    private String jsFile;

    /**
     * Enable or disable legacy components annotated only with
     * {@link HtmlImport}.
     */
    @Parameter(defaultValue = "true")
    private boolean convertHtml;

    private final List<String> lumoJsFiles = Arrays.asList(new String[] {
            "@vaadin/vaadin-lumo-styles/color.js",
            "@vaadin/vaadin-lumo-styles/typography.js",
            "@vaadin/vaadin-lumo-styles/sizing.js",
            "@vaadin/vaadin-lumo-styles/spacing.js",
            "@vaadin/vaadin-lumo-styles/style.js",
            "@vaadin/vaadin-lumo-styles/icons.js"
    });

    @Override
    public void execute() {
        URL[] projectClassPathUrls = getProjectClassPathUrls(project);

        getLog().info("Looking for imports ...");

        AnnotationValuesExtractor annotationValuesExtractor = new AnnotationValuesExtractor(projectClassPathUrls);

        Map<Class<?>, Set<String>> classesWithJsModule = annotationValuesExtractor.getAnnotatedClasses(JsModule.class,
                "value");

        Map<Class<?>, Set<String>> classes = new HashMap<>(classesWithJsModule);

        if (convertHtml) {
            Map<Class<?>, Set<String>> classesWithHtmlImport = annotationValuesExtractor
                    .getAnnotatedClasses(HtmlImport.class, "value");

            classesWithHtmlImport = classesWithHtmlImport.entrySet().stream()
                    .filter(entry -> !classesWithJsModule.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(Entry::getKey, entry -> getHtmlImportNpmPackages(entry.getValue())));

            classes.putAll(classesWithHtmlImport);
        }

        try {
            Pattern pattern = Pattern.compile("^import\\s*'(.*)'\\s*;$");

            List<String> current = FileUtils.fileExists(jsFile)
                    ? FileUtils.loadFile(new File(jsFile))
                    : Collections.emptyList();
            Set<String> existingJsModules = current.stream().map(
                    jsImport -> pattern.matcher(jsImport).replaceFirst("$1"))
                    .collect(Collectors.toSet());

            Set<String> jsModules = new HashSet<>();

            // Add Lumo files manually.
            jsModules.addAll(lumoJsFiles);

            classes.entrySet().stream().forEach(entry -> entry.getValue().forEach(fileName -> {
                // add `./` prefix to everything starting with letters
                fileName = fileName.replaceFirst("(?i)^([a-z])", "./$1");
                jsModules.add(fileName);
            }));

            if (existingJsModules.equals(jsModules)) {
                getLog().info("No js modules to update");

            } else {
                String content = jsModules.stream().sorted(Comparator.reverseOrder()).map(s -> "import '" + s + "';")
                        .collect(Collectors.joining("\n"));
                replaceJsFile(content + "\n");
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
