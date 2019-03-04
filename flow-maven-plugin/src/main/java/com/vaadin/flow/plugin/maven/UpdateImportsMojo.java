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

import static com.vaadin.flow.plugin.maven.UpdateNpmDependenciesMojo.getHtmlImportNpmPackages;
import static com.vaadin.flow.plugin.maven.UpdateNpmDependenciesMojo.getProjectClassPathUrls;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.plugin.common.AnnotationValuesExtractor;

/**
 * Goal that updates flow-imports.js file with @JsModule and @HtmlImport
 * annotations defined in the classpath.
 */
@Mojo(name = "update-imports", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class UpdateImportsMojo extends AbstractMojo {

    static final String IMPORTS_FILE_PARAMETER = "vaadin.imports.file";
    static final String IMPORTS_FILE_DEFAULT = "src/main/webapp/frontend/main.js";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() {
        URL[] projectClassPathUrls = getProjectClassPathUrls(project);

        getLog().info("Looking for imports ...");

        AnnotationValuesExtractor annotationValuesExtractor = new AnnotationValuesExtractor(projectClassPathUrls);

        Map<Class<?>, Set<String>> classesWithHtmlImport = annotationValuesExtractor
                .getAnnotatedClasses(HtmlImport.class, "value");

        Map<Class<?>, Set<String>> classesWithJsModule = annotationValuesExtractor.getAnnotatedClasses(JsModule.class,
                "value");

        classesWithHtmlImport = classesWithHtmlImport.entrySet().stream()
                .filter(entry -> !classesWithJsModule.containsKey(entry.getKey())).collect(
                        Collectors.toMap(entry -> entry.getKey(), entry -> getHtmlImportNpmPackages(entry.getValue())));

        Map<Class<?>, Set<String>> classes = new HashMap<>(classesWithJsModule);
        classes.putAll(classesWithHtmlImport);

        Set<String> jsModules = new HashSet<String>();
        classes.entrySet().stream().forEach(entry -> entry.getValue().forEach(s -> jsModules.add(s)));

        String content = jsModules.stream().map(s -> "import '" + s + "';").collect(Collectors.joining("\n"));
        try {
            saveFile(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveFile(String content) throws IOException {
        File out = new File(System.getProperty(IMPORTS_FILE_PARAMETER, IMPORTS_FILE_DEFAULT));
        if (out.getParentFile() != null && !out.getParentFile().exists()) {
            getLog().info("creating Folder: " + out.getParentFile().getAbsolutePath());
            out.getParentFile().mkdirs();
        }
        getLog().info("Saving imports to file: " + out.getAbsolutePath());

        out.createNewFile();
        Files.write(Paths.get(out.toURI()), content.getBytes());
    }
}
