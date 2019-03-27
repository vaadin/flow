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

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.server.DevModeHandler;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;


/**
 * Updates <code>package.json</code> by visiting {@link NpmPackage} annotations found in
 * the classpath. It also visits classes annotated with {@link NpmPackage}
 */
public class NodeUpdatePackages extends NodeUpdater {

    /**
     * The name of the webpack config file.
     */
    public static final String WEBPACK_CONFIG = "webpack.config.js";

    private static final String VALUE = "value";
    private final String webpackTemplate;

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param extractor
     *            a reusable annotation extractor
     * @param webpackTemplate
     *            name of the webpack resource to be used as template when
     *            creating the <code>webpack.config.js</code> file
     * @param npmFolder
     *            folder with the `package.json` file
     * @param nodeModulesPath
     *            the path to the {@literal node_modules} directory of the project
     * @param convertHtml
     *            true to enable polymer-2 annotated classes to be considered
     */
    public NodeUpdatePackages(AnnotationValuesExtractor extractor, String webpackTemplate, File npmFolder,
            File nodeModulesPath, boolean convertHtml) {
        this.annotationValuesExtractor = extractor;
        this.npmFolder = npmFolder;
        this.nodeModulesPath = nodeModulesPath;
        this.webpackTemplate = webpackTemplate;
        this.convertHtml = convertHtml;
    }

    /**
     * Create an instance of the updater given the reusable extractor, the rest
     * of the configurable parameters will be set to their default values.
     *
     * @param extractor
     *            a reusable annotation extractor
     */
    public NodeUpdatePackages(AnnotationValuesExtractor extractor) {
        this(extractor, WEBPACK_CONFIG, new File("."),
                new File("./node_modules/"), true);
    }

    @Override
    public void execute() {
        log().info("Looking for npm package dependencies in the java class-path ...");

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
    }

    private void createWebpackConfig() throws IOException {
        if (webpackTemplate == null || webpackTemplate.trim().isEmpty()) {
            return;
        }

        File configFile = new File(npmFolder, WEBPACK_CONFIG);

        if (configFile.exists()) {
            log().info("No changes to {}", configFile);
        } else {
            URL resource = this.getClass().getClassLoader().getResource(webpackTemplate);
            if (resource == null) {
                resource = new URL(webpackTemplate);
            }
            FileUtils.copyURLToFile(resource, configFile);
            log().info("Created {} from {}", WEBPACK_CONFIG, resource);
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

    private void updatePackageJsonDependencies(JsonObject packageJson, Map<Class<?>, Set<String>> classes)
            throws IOException {
        JsonObject currentDeps = packageJson.getObject("dependencies");

        Set<String> dependencies = new HashSet<>();
        classes.values().stream().flatMap(Collection::stream).forEach(s -> {
            // exclude local dependencies (those starting with `.` or `/`
            if (s.matches("[^./].*") && !s.matches("(?i)[a-z].*\\.js$") && !currentDeps.hasKey(s)
                    && !s.startsWith(FLOW_PACKAGE)) {
                dependencies.add(s);
            }
        });

        if (!currentDeps.hasKey("@webcomponents/webcomponentsjs")) {
            dependencies.add("@webcomponents/webcomponentsjs");
        }

        if (dependencies.isEmpty()) {
            log().info("No npm packages to update");
        } else {
            updateDependencies(dependencies.stream().sorted().collect(Collectors.toList()), "--save");
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
            log().info("No npm dev packages to update");
        } else {
            updateDependencies(dependencies.stream().sorted().collect(Collectors.toList()), "--save-dev");
        }
    }

    private void updateDependencies(List<String> dependencies, String... npmInstallArgs) throws IOException {
        List<String> command = new ArrayList<>(5 + dependencies.size());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(npmFolder);

        if (!DevModeHandler.UNIX_OS) {
            command.add("npm.cmd");
        } else {
            builder.environment().put("PATH", builder.environment().get("PATH") + ":/usr/local/bin");
            command.add("npm");
        }

        command.add("--no-package-lock");
        command.add("install");
        command.addAll(Arrays.asList(npmInstallArgs));
        command.addAll(dependencies);

        String cmd = String.join(" ", command);
        log().info("Updating package.json and installing npm dependencies ...\n {}", cmd);

        Process process = builder.start();
        logStream(process.getInputStream());
        logStream(process.getErrorStream());
        try {
            // At this point some linux & node.js (CI) seems not to have finished.
            // destroying the process and sleeping helps to get green builds
            process.destroy();
            if (process.isAlive()) {
                log().warn("npm process still alive, sleeping 500ms");
                Thread.sleep(500);
            }
            if (process.exitValue() != 0) {
                log().error(
                        ">>> Dependency ERROR. Check that all required dependencies are deployed in npm repositories.");
            }
            log().info("package.json updated and npm dependencies installed. ");
        } catch (Exception e) {
            log().error("Error destroying webpack process", e);
        }
    }

    private JsonObject getPackageJson() throws IOException {
        JsonObject packageJson;
        File packageFile = new File(npmFolder, PACKAGE_JSON);

        if (packageFile.exists()) {
            packageJson = Json.parse(FileUtils.readFileToString(packageFile, "UTF-8"));
        } else {
            log().info("Creating a default {}", packageFile);
            FileUtils.writeStringToFile(packageFile, "{}", "UTF-8");
            packageJson = Json.createObject();
        }
        ensureMissingObject(packageJson, "dependencies");
        ensureMissingObject(packageJson, "devDependencies");
        return packageJson;
    }

    private void ensureMissingObject(JsonObject packageJson, String name) {
        if (!packageJson.hasKey(name)) {
            packageJson.put(name, Json.createObject());
        }
    }

    private void logStream(InputStream input) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty() && !line.contains("npm WARN")) {
                    log().info(line);
                }
            }
        } catch (IOException e) {
            log().error("Error when reading from npm stdin/stderr", e);
        }
    }
}
