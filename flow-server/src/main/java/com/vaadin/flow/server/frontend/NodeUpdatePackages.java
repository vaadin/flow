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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.component.dependency.NpmPackage;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.getBaseDir;
import static com.vaadin.flow.server.frontend.NodeUpdateImports.FLOW_IMPORTS_FILE;
import static com.vaadin.flow.server.frontend.NodeUpdateImports.MAIN_JS_PARAM;


/**
 * Updates <code>package.json</code> by visiting {@link NpmPackage} annotations found in
 * the classpath. It also visits classes annotated with {@link NpmPackage}
 */

@NpmPackage(value = "@webcomponents/webcomponentsjs", version = "2.2.9")
public class NodeUpdatePackages extends NodeUpdater {

    private final String webpackTemplate;
    private final File webpackOutputDirectory;
    private final File generatedFlowImports;

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param finder
     *            a reusable class finder
     * @param webpackOutputDirectory
     *            the directory to set for webpack to output its build results
     * @param webpackTemplate
     *            name of the webpack resource to be used as template when
     *            creating the <code>webpack.config.js</code> file
     * @param generatedFlowImports
     *            name of the JS file to update with the Flow project imports
     * @param npmFolder
     *            folder with the `package.json` file
     * @param nodeModulesPath
     *            the path to the {@literal node_modules} directory of the
     *            project
     * @param convertHtml
     *            whether to convert html imports or not during the package
     *            updates
     */
    public NodeUpdatePackages(ClassFinder finder, File webpackOutputDirectory,
            String webpackTemplate, File generatedFlowImports, File npmFolder,
            File nodeModulesPath, boolean convertHtml) {
        super(finder, npmFolder, nodeModulesPath, convertHtml);
        this.generatedFlowImports = generatedFlowImports;
        this.webpackOutputDirectory = webpackOutputDirectory;
        this.webpackTemplate = webpackTemplate;
    }

    /**
     * Create an instance of the updater given the reusable extractor, the rest
     * of the configurable parameters will be set to their default values.
     *
     * @param finder
     *            a reusable class finder
     */
    public NodeUpdatePackages(ClassFinder finder) {
        this(finder, new File(getBaseDir(), "src/main/webapp"), WEBPACK_CONFIG,
                Paths.get(getBaseDir()).resolve("target")
                        .resolve(System.getProperty(MAIN_JS_PARAM,
                                FLOW_IMPORTS_FILE))
                        .toFile(),
                new File(getBaseDir()), new File(getBaseDir(), "node_modules"),
                true);
    }

    @Override
    public void execute() {
        try {
            JsonObject packageJson = getPackageJson();

            HashMap<String, String> deps = frontDeps.getPackages();
            if (convertHtml) {
                addHtmlImportPackages(deps);
            }

            boolean modified = updatePackageJsonDependencies(packageJson, deps);
            modified = updatePackageJsonDevDependencies(packageJson) || modified ;

            if (modified) {
                writePackageFile(packageJson);
                runNpmInstall();
            } else {
                log().info("No packages to update");
            }

            createWebpackConfig();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void addHtmlImportPackages(HashMap<String, String> packages) throws IOException {
        JsonObject shrink = getShrinkwrapJson().getObject("dependencies");
        for (String s : getHtmlImportNpmPackages(frontDeps.getImports())) {
            if (!packages.containsKey(s) && shrink.hasKey(s)) {
                packages.put(s, shrink.getObject(s).getString("version"));
            }
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
                        .map(line -> line.replace("{{GENERATED_FLOW_IMPORTS}}",
                                generatedFlowImports.getPath()
                                        .replaceAll("\\\\", "/")))
                        .collect(Collectors.toList());
                Files.write(configFile.toPath(), webpackConfigLines);
                log().info("Created {} from {}", WEBPACK_CONFIG, resource);
            }
        }
    }

    private boolean updatePackageJsonDependencies(JsonObject packageJson, HashMap<String, String> deps) {
        boolean modified = false;
        JsonObject json = packageJson.getObject("dependencies");

        for(Entry<String, String> e : deps.entrySet()) {
            String version = "=" + e.getValue();
            if (!json.hasKey(e.getKey()) || !json.getString(e.getKey()).equals(version)) {
                json.put(e.getKey(), version);
                log().info("Added {}@{} dependency.", e.getKey(), version);
                modified = true;
            }
        }

        return modified;
    }

    private boolean updatePackageJsonDevDependencies(JsonObject packageJson) {
        boolean modified = false;
        JsonObject json = packageJson.getObject("devDependencies");

        for (String pkg : Arrays.asList(
                "webpack",
                "webpack-cli",
                "webpack-dev-server",
                "webpack-babel-multi-target-plugin",
                "copy-webpack-plugin"
                )) {
            if (!json.hasKey(pkg)) {
                json.put(pkg, "latest");
                log().info("Added {} dependency.", pkg);
                modified = true;
            }
        }
        return modified;
    }

    private void runNpmInstall() {
        List<String> command = new ArrayList<>();
        command.addAll(FrontendUtils.getNpmExecutable());
        command.add("install");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(npmFolder);
        if (log().isInfoEnabled()) {
            log().info(
                "Running `npm install` ...");
        }
        executeNpmInstall(builder);
    }

    /**
     * Executes `npm install` after `package.json` has been updated.
     *
     * @param builder
     */
    // NOTE: public because needs to be stub in flow-maven-plugin
    public void executeNpmInstall(ProcessBuilder builder) {
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

    private void writePackageFile(JsonObject packageJson) throws IOException {
        File packageFile = new File(npmFolder, PACKAGE_JSON);
        FileUtils.writeStringToFile(packageFile, packageJson.toJson(), "UTF-8");
    }

    JsonObject getPackageJson() throws IOException {
        JsonObject packageJson;
        File packageFile = new File(npmFolder, PACKAGE_JSON);

        if (packageFile.exists()) {
            String s = FileUtils.readFileToString(packageFile, "UTF-8");
            packageJson = Json.parse(s);
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

    /**
     * Get latest vaadin-core-shrinkwrap file so as we can set correctly the
     * version of legacy elements marked with HtmlImport but not with NpmPackage
     * or JsImport.
     *
     * This is a temporary solution during alpha period until all
     * flow-components are updated and released
     *
     * @return
     * @throws IOException
     */
    private JsonObject getShrinkwrapJson() throws IOException {
        URL url = new URL("https://raw.githubusercontent.com/vaadin/vaadin-core-shrinkwrap/master/npm-shrinkwrap.json");
        String content = FrontendUtils.streamToString(url.openStream());
        return Json.parse(content);
    }
}
