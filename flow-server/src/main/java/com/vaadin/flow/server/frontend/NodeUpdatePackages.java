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
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import static com.vaadin.flow.server.frontend.FrontendUtils.getBaseDir;


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
    private final File webpackOutputDirectory;

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param extractor
     *            a reusable annotation extractor
     * @param webpackOutputDirectory
     *            the directory to set for webpack to output its build results
     * @param webpackTemplate
     *            name of the webpack resource to be used as template when
     *            creating the <code>webpack.config.js</code> file
     * @param npmFolder
     *            folder with the `package.json` file
     * @param nodeModulesPath
     *            the path to the {@literal node_modules} directory of the
     *            project
     * @param convertHtml
     *            whether to convert html imports or not during the package
     *            updates
     */
    public NodeUpdatePackages(AnnotationValuesExtractor extractor,
            File webpackOutputDirectory, String webpackTemplate, File npmFolder,
            File nodeModulesPath, boolean convertHtml) {
        this.annotationValuesExtractor = extractor;
        this.npmFolder = npmFolder;
        this.nodeModulesPath = nodeModulesPath;
        this.webpackOutputDirectory = webpackOutputDirectory;
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
        this(extractor, new File(getBaseDir(), "src/main/webapp"), WEBPACK_CONFIG, new File(getBaseDir()),
                new File(getBaseDir(), "node_modules"), true);
    }

    @Override
    public void execute() {
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
            log().info("{} already exists.", configFile);
        } else {
            URL resource = this.getClass().getClassLoader()
                    .getResource(webpackTemplate);
            if (resource == null) {
                resource = new URL(webpackTemplate);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    resource.openStream(), StandardCharsets.UTF_8))) {
                List<String> webpackConfigLines = br.lines()
                    .map(line -> line.replace("{{OUTPUT_DIRECTORY}}", webpackOutputDirectory.getPath()))
                        .collect(Collectors.toList());
                Files.write(configFile.toPath(), webpackConfigLines);
                log().info("Created {} from {}", WEBPACK_CONFIG, resource);
            }
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

    private void updatePackageJsonDependencies(JsonObject packageJson, Map<Class<?>, Set<String>> classes) {
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

    private void updatePackageJsonDevDependencies(JsonObject packageJson) {
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

    private void updateDependencies(List<String> dependencies,
            String... npmInstallArgs) {
        ProcessBuilder builder = new ProcessBuilder(
                getNpmCommand(dependencies, npmInstallArgs));
        builder.directory(npmFolder);
        if (DevModeHandler.UNIX_OS) {
            builder.environment().put("PATH",
                    builder.environment().get("PATH") + ":/usr/local/bin");
        }
        if (log().isInfoEnabled()) {
            log().info(
                "Updating package.json and installing npm dependencies ...\n {}",
                String.join(" ", builder.command()));
        }

        Process process = null;
        try {
            process = builder.inheritIO().start();
            int errorCode = process.waitFor();
            if (errorCode != 0) {
                log().error(
                        ">>> Dependency ERROR. Check that all required dependencies are deployed in npm repositories.");
            } else {
                log().info(
                        "package.json updated and npm dependencies installed. ");
            }
        } catch (InterruptedException | IOException e) {
            log().error("Error running npm", e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    private List<String> getNpmCommand(List<String> dependencies,
            String... npmInstallArgs) {
        List<String> command = new ArrayList<>(5 + dependencies.size());
        command.add(DevModeHandler.UNIX_OS ? "npm" : "npm.cmd");
        command.add("--no-package-lock");
        command.add("install");
        command.addAll(Arrays.asList(npmInstallArgs));
        command.addAll(dependencies);
        return command;
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
}
