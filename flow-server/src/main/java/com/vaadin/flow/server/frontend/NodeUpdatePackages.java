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
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.component.dependency.NpmPackage;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Updates <code>package.json</code> by visiting {@link NpmPackage} annotations found in
 * the classpath. It also visits classes annotated with {@link NpmPackage}
 */
@NpmPackage(value = "@webcomponents/webcomponentsjs", version = "2.2.9")
@NpmPackage(value = "@polymer/polymer", version = "3.1.0")
public class NodeUpdatePackages extends NodeUpdater {

    private static final String DEV_DEPENDENCIES = "devDependencies";
    private static final String DEPENDENCIES = "dependencies";

    boolean modified = false;

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param finder
     *            a reusable class finder
     * @param npmFolder
     *            folder with the `package.json` file
     * @param nodeModulesPath
     *            the path to the {@literal node_modules} directory of the
     *            project
     * @param convertHtml
     *            whether to convert html imports or not during the package
     *            updates
     */
    public NodeUpdatePackages(ClassFinder finder, File npmFolder,
                              File nodeModulesPath, boolean convertHtml) {
        this(finder, null, npmFolder, nodeModulesPath, convertHtml);
    }

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param finder
     *            a reusable class finder
     * @param frontendDependencies
     *            a reusable frontend dependencies
     * @param npmFolder
     *            folder with the `package.json` file
     * @param nodeModulesPath
     *            the path to the {@literal node_modules} directory of the
     *            project
     * @param convertHtml
     *            whether to convert html imports or not during the package
     *            updates
     */
    public NodeUpdatePackages(ClassFinder finder,
            FrontendDependencies frontendDependencies, File npmFolder,
            File nodeModulesPath, boolean convertHtml) {
        super(finder, frontendDependencies, npmFolder, nodeModulesPath, convertHtml);
    }

    @Override
    public void execute() {
        try {
            JsonObject packageJson = getPackageJson();

            Map<String, String> deps = frontDeps.getPackages();
            if (convertHtml) {
                addHtmlImportPackages(deps);
            }

            modified = updatePackageJsonDependencies(packageJson, deps);
            modified = updatePackageJsonDevDependencies(packageJson) || modified ;

            if (modified) {
                writePackageFile(packageJson);
            } else {
                log().info("No packages to update");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void addHtmlImportPackages(Map<String, String> packages) throws IOException {
        JsonObject shrink = getShrinkwrapJson().getObject(DEPENDENCIES);
        for (String pakage : getHtmlImportNpmPackages(frontDeps.getImports())) {
            if (!packages.containsKey(pakage) && shrink.hasKey(pakage)) {
                packages.put(pakage, shrink.getObject(pakage).getString("version"));
            }
        }
    }

    private boolean updatePackageJsonDependencies(JsonObject packageJson, Map<String, String> deps) {
        boolean added = false;
        JsonObject json = packageJson.getObject(DEPENDENCIES);
        for(Entry<String, String> e : deps.entrySet()) {
            String version = "=" + e.getValue();
            if (!json.hasKey(e.getKey()) || !json.getString(e.getKey()).equals(version)) {
                json.put(e.getKey(), version);
                log().info("Added {}@{} dependency.", e.getKey(), version);
                added = true;
            }
        }
        return added;
    }

    private boolean updatePackageJsonDevDependencies(JsonObject packageJson) {
        boolean added = false;
        JsonObject json = packageJson.getObject(DEV_DEPENDENCIES);
        Map<String, String> devDependencies = new HashMap<>();
        devDependencies.put("webpack", "4.30.0");
        devDependencies.put("webpack-cli", "3.3.0");
        devDependencies.put("webpack-dev-server", "3.3.0");
        devDependencies.put("webpack-babel-multi-target-plugin", "2.1.0");
        devDependencies.put("copy-webpack-plugin", "5.0.3");
        for(Entry<String, String> entry: devDependencies.entrySet()) {
            if(!json.hasKey(entry.getKey())) {
                json.put(entry.getKey(), entry.getValue());
                log().info("Added {} dependency.", entry.getKey());
                added = true;
            }
        }
        return added;
    }

    private void writePackageFile(JsonObject packageJson) throws IOException {
        File packageFile = new File(npmFolder, PACKAGE_JSON);
        FileUtils.writeStringToFile(packageFile, packageJson.toJson(), UTF_8.name());
    }

    JsonObject getPackageJson() throws IOException {
        JsonObject packageJson;
        File packageFile = new File(npmFolder, PACKAGE_JSON);

        if (packageFile.exists()) {
            String fileContent = FileUtils.readFileToString(packageFile, UTF_8.name());
            packageJson = Json.parse(fileContent);
        } else {
            log().info("Creating a default {}", packageFile);
            FileUtils.writeStringToFile(packageFile, "{}", UTF_8.name());
            packageJson = Json.createObject();
        }
        ensureMissingObject(packageJson, DEPENDENCIES);
        ensureMissingObject(packageJson, DEV_DEPENDENCIES);
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
