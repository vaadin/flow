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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.plugin.common.AnnotationValuesExtractor;
import com.vaadin.flow.plugin.common.FlowPluginFileUtils;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Goal that updates package.json file with @NpmPackage annotations defined in
 * the classpath.
 */
@Mojo(name = "update-npm-dependencies", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class UpdateNpmDependenciesMojo extends AbstractMojo {

    private static final String VALUE = "value";

    public static final String PACKAGE_JSON = "package.json";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The folder where `package.json` file is located. Default is current dir.
     */
    @Parameter(defaultValue = "./")
    private String npmFolder;

    /**
     * Enable or disable legacy components annotated only with
     * {@link HtmlImport}.
     */
    @Parameter(defaultValue = "true")
    private boolean convertHtml;

    private Log log = getLog();

    @Override
    public void execute() {
        URL[] projectClassPathUrls = getProjectClassPathUrls(project);

        log.info("Looking for npm packages...");

        Map<Class<?>, Set<String>> classes = new HashMap<>();

        AnnotationValuesExtractor annotationValuesExtractor = new AnnotationValuesExtractor(projectClassPathUrls);

        Map<Class<?>, Set<String>> classesWithNpmPackage = annotationValuesExtractor
                .getAnnotatedClasses(NpmPackage.class, VALUE);

        classes.putAll(classesWithNpmPackage);

        if (convertHtml) {
            Map<Class<?>, Set<String>> classesWithHtmlImport = annotationValuesExtractor
                    .getAnnotatedClasses(HtmlImport.class, VALUE);

            Map<Class<?>, Set<String>> classesWithJsModule = annotationValuesExtractor
                    .getAnnotatedClasses(JsModule.class, VALUE);

            // Remove classes with HtmlImport that already have npm annotations
            classesWithHtmlImport = classesWithHtmlImport.entrySet().stream()
                    .filter(entry -> !classesWithNpmPackage.containsKey(entry.getKey())
                            && !classesWithJsModule.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(Entry::getKey, entry -> getHtmlImportNpmPackages(entry.getValue())));

            classes.putAll(classesWithHtmlImport);
        }

        try {
            JsonObject currentDeps = parsePackage();

            Set<String> dependencies = new HashSet<>();
            classes.entrySet().stream().forEach(entry -> entry.getValue().forEach(s -> {
                // exclude local dependencies (those starting with `.` or `/`
                if (s.matches("[^./].*") && !s.matches("(?i)[a-z].*\\.js$") && !currentDeps.hasKey(s)) {
                    dependencies.add(s);
                }
            }));

            if (createPackageJsonFile()) {
                dependencies.add("@webcomponents/webcomponentsjs");
            }

            if (dependencies.isEmpty()) {
                log.info("No npm packages to update");
            } else {
                updateDependencies(dependencies.stream().sorted().collect(Collectors.toList()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean createPackageJsonFile() throws IOException {
        String packageFile = npmFolder + "/" + PACKAGE_JSON;
        if (!FileUtils.fileExists(packageFile)) {
            log.info("Creating a default " + packageFile);
            FileUtils.fileWrite(packageFile, "{}");
            return true;
        }
        return false;
    }

    private void updateDependencies(List<String> dependencies) throws IOException {
        List<String> command = new ArrayList<>(5 + dependencies.size());
        command.add("npm");
        command.add("install");
        command.add("--save");
        command.add("--package-lock-only");
        command.add("--no-package-lock");
        command.addAll(dependencies);

        log.info("Updating npm dependencies\n " + command.stream().collect(Collectors.joining(" ")));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(new File(npmFolder));

        Process process = builder.start();
        logStream(process.getInputStream());
        logStream(process.getErrorStream());
        if (process.exitValue() != 0) {
            getLog().error(
                    ">>> Dependency ERROR. Check that all required dependencies are deployed in npm repositories.");
        }
    }

    private JsonObject parsePackage() throws IOException {
        String packageFile = npmFolder + "/" + PACKAGE_JSON;
        if (FileUtils.fileExists(packageFile)) {
            JsonObject o = Json.parse(FileUtils.fileRead(packageFile));
            if (o.hasKey("dependencies")) {
                return o.getObject("dependencies");
            }
        }
        return Json.createObject();
    }

    private void logStream(InputStream input) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty() && !line.contains("npm WARN")) {
                    log.info(line);
                }
            }

        } catch (IOException e) {
            log.error(e);
        }
    }

    static Set<String> getHtmlImportJsModules(Set<String> htmlImports) {
        return htmlImports.stream().map(UpdateNpmDependenciesMojo::getHtmlImportJsModule).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    static Set<String> getHtmlImportNpmPackages(Set<String> htmlImports) {
        return htmlImports.stream().map(UpdateNpmDependenciesMojo::getHtmlImportNpmPackage).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    static String getHtmlImportJsModule(String htmlImport) {
        String module = htmlImport // @formatter:off
                .replaceFirst("^.*bower_components/(vaadin-[^/]*/.*)\\.html$", "@vaadin/$1.js")
                .replaceFirst("^.*bower_components/((iron|paper)-[^/]*/.*)\\.html$", "@polymer/$1.js")
                .replaceFirst("^frontend://(.*)$", "./$1")
                .replaceFirst("\\.html$", ".js")
                .replaceFirst("^([a-z].*\\.js)$", "./$1")
                ; // @formatter:on
        return Objects.equals(module, htmlImport) ? null : module;
    }

    static String getHtmlImportNpmPackage(String htmlImport) {
        String module = htmlImport // @formatter:off
                .replaceFirst("^.*bower_components/(vaadin-[^/]*)/.*\\.html$", "@vaadin/$1")
                .replaceFirst("^.*bower_components/((iron|paper)-[^/]*)/.*\\.html$", "@polymer/$1")
                .replaceFirst("^frontend://(.*)$", "./$1")
                .replaceFirst("\\.html$", ".js")
                .replaceFirst("^([a-z].*\\.js)$", "./$1")
                ; // @formatter:on
        return Objects.equals(module, htmlImport) ? null : module;
    }

    static URL[] getProjectClassPathUrls(MavenProject project) {
        final List<String> runtimeClasspathElements;
        try {
            runtimeClasspathElements = project.getRuntimeClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException(
                    String.format("Failed to retrieve runtime classpath elements from project '%s'", project), e);
        }
        return runtimeClasspathElements.stream().map(File::new).map(FlowPluginFileUtils::convertToUrl)
                .toArray(URL[]::new);
    }
}
