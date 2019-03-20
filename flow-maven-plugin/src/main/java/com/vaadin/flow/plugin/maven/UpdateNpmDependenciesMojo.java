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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.exec.OS;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.FileUtils;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.plugin.common.AnnotationValuesExtractor;
import com.vaadin.flow.plugin.common.JarContentsManager;
import com.vaadin.flow.server.Constants;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.plugin.production.ProductionModeCopyStep.NON_WEB_JAR_RESOURCE_PATH;

/**
 * Goal that updates package.json file with @NpmPackage annotations defined in
 * the classpath.
 */
@Mojo(name = "update-npm-dependencies", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class UpdateNpmDependenciesMojo extends AbstractNpmMojo {

    private static final String VALUE = "value";

    public static final String PACKAGE_JSON = "package.json";
    public static final String WEBPACK_CONFIG = "webpack.config.js";

    /**
     * Enable or disable legacy components annotated only with
     * {@link HtmlImport}.
     */
    @Parameter(defaultValue = "true")
    private boolean convertHtml;

    /**
     * Copy the `webapp.config.js` from the specified URL if missing.
     * Default is the template provided by this plugin.
     * Leave it blank to disable the feature.
     */
    @Parameter(defaultValue = WEBPACK_CONFIG)
    private String webpackTemplate;

    private final Log log = getLog();

    private AnnotationValuesExtractor annotationValuesExtractor;
    private JarContentsManager jarContentsManager;

    @Override
    public void execute() {

        // Do nothing when bower mode
        if (Boolean.getBoolean("vaadin." + Constants.SERVLET_PARAMETER_BOWER_MODE)) {
            getLog().info("Skipped `update-npm-dependencies` goal because `vaadin.bowerMode` is set.");
            return;
        }

        log.info("Looking for npm package dependencies in the java class-path ...");

        if (annotationValuesExtractor == null) {
            URL[] projectClassPathUrls = getProjectClassPathUrls(project);
            annotationValuesExtractor = new AnnotationValuesExtractor(projectClassPathUrls);
        }

        try {
            Map<Class<?>, Set<String>> classes = annotationValuesExtractor.getAnnotatedClasses(NpmPackage.class, VALUE);
            classes.putAll(classesWithHtmlImport(classes));

            JsonObject packageJson = getPackageJson();

            updatePackageJsonDependencies(packageJson, classes);

            updatePackageJsonDevDependencies(packageJson);

            createWebpackConfig();

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        log.info("Generating the Flow package...");

        if (jarContentsManager == null) {
            jarContentsManager = new JarContentsManager();
        }

        try {
            if (flowPackageDirectory.isDirectory()) {
                FileUtils.cleanDirectory(flowPackageDirectory);
            } else {
                FileUtils.forceMkdir(flowPackageDirectory);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        project.getArtifacts().stream()
            .filter(artifact -> "jar".equals(artifact.getType()))
            .map(Artifact::getFile)
            .filter(File::isFile)
            .forEach(jar -> jarContentsManager.copyFilesFromJarTrimmingBasePath(
                jar, NON_WEB_JAR_RESOURCE_PATH, flowPackageDirectory));
    }

    private void createWebpackConfig() throws IOException {
        if (webpackTemplate == null || webpackTemplate.trim().isEmpty()) {
            return;
        }

        String configFile = npmFolder + "/" + WEBPACK_CONFIG;

        if (FileUtils.fileExists(configFile)) {
            log.info("No changes to " + configFile);
        } else {
            URL resource = this.getClass().getClassLoader().getResource(webpackTemplate);
            if (resource == null) {
                resource = new URL(webpackTemplate);
            }
            FileUtils.copyURLToFile(resource, new File(configFile));
            log.info("Created " + WEBPACK_CONFIG + " from " + resource);
        }
    }

    private Map<Class<?>, Set<String>> classesWithHtmlImport(Map<Class<?>, Set<String>> classesWithNpmPackage) {
        if (convertHtml) {
            Map<Class<?>, Set<String>> classesWithHtmlImport = annotationValuesExtractor
                    .getAnnotatedClasses(HtmlImport.class, VALUE);

            Map<Class<?>, Set<String>> classesWithJsModule = annotationValuesExtractor
                    .getAnnotatedClasses(JsModule.class, VALUE);

            // Remove classes with HtmlImport that already have npm annotations
            return classesWithHtmlImport.entrySet().stream()
                    .filter(entry -> !classesWithNpmPackage.containsKey(entry.getKey())
                            && !classesWithJsModule.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(Entry::getKey, entry -> getHtmlImportNpmPackages(entry.getValue())));
        }
        return Collections.emptyMap();
    }

    private void updatePackageJsonDependencies(JsonObject packageJson, Map<Class<?>, Set<String>> classes) throws IOException {
        JsonObject currentDeps = packageJson.getObject("dependencies");

        Set<String> dependencies = new HashSet<>();
        classes.values().stream().flatMap(Collection::stream).forEach(s -> {
            // exclude local dependencies (those starting with `.` or `/`
            if (s.matches("[^./].*") && !s.matches("(?i)[a-z].*\\.js$") && !currentDeps.hasKey(s)) {
                dependencies.add(s);
            }
        });

        if (!currentDeps.hasKey("@webcomponents/webcomponentsjs")) {
            dependencies.add("@webcomponents/webcomponentsjs");
        }

        if (dependencies.isEmpty()) {
            log.info("No npm packages to update");
        } else {
            updateDependencies(
                    dependencies.stream().sorted().collect(Collectors.toList()),
                    "--save");
        }
    }

    private void updatePackageJsonDevDependencies(JsonObject packageJson) throws IOException {
        JsonObject currentDeps = packageJson.getObject("devDependencies");

        Set<String> dependencies = new HashSet<>();
        dependencies.add("webpack");
        dependencies.add("webpack-cli");
        dependencies.add("webpack-dev-server");
        dependencies.add("webpack-babel-multi-target-plugin");
        dependencies.add("copy-webpack-plugin");

        dependencies.removeAll(Arrays.asList(currentDeps.keys()));

        if (dependencies.isEmpty()) {
            log.info("No npm dev packages to update");
        } else {
            updateDependencies(
                    dependencies.stream().sorted().collect(Collectors.toList()),
                    "--save-dev");
        }

    }

    private void updateDependencies(List<String> dependencies, String... npmInstallArgs) throws IOException {
        List<String> command = new ArrayList<>(5 + dependencies.size());
        if (OS.isFamilyWindows()) {
            command.add("npm.cmd");
        } else {
            command.add("npm");
        }
        command.add("--no-package-lock");
        command.add("install");
        command.addAll(Arrays.asList(npmInstallArgs));
        command.addAll(dependencies);

        log.info("Updating package.json and installing npm dependencies ...\n " + String.join(" ", command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(npmFolder);

        Process process = builder.start();
        logStream(process.getInputStream());
        logStream(process.getErrorStream());
        try {
            // At this point some linux & node.js (CI) seems not to have finished.
            // destroying the process and sleeping helps to get green builds
            process.destroy();
            if (process.isAlive()) {
                getLog().warn("npm process still alive, sleeping 500ms");
                Thread.sleep(500);
            }
            if (process.exitValue() != 0) {
                getLog().error(
                        ">>> Dependency ERROR. Check that all required dependencies are deployed in npm repositories.");
            }
            getLog().info("package.json updated and npm dependencies installed. ");
        } catch (Exception e) {
            getLog().error(e);
        }
    }

    private JsonObject getPackageJson() throws IOException {

        JsonObject packageJson;

        String packageFile = npmFolder + "/" + PACKAGE_JSON;
        if (FileUtils.fileExists(packageFile)) {
            packageJson = Json.parse(FileUtils.fileRead(packageFile));

        } else {
            log.info("Creating a default " + packageFile);
            FileUtils.fileWrite(packageFile, "{}");
            packageJson = Json.createObject();
        }

        ensureMissingObject(packageJson,"dependencies");
        ensureMissingObject(packageJson,"devDependencies");

        return packageJson;
    }

    private void ensureMissingObject(JsonObject packageJson, String name) {
        if (!packageJson.hasKey(name)) {
            packageJson.put(name, Json.createObject());
        }
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
}
