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
import java.util.Map;
import java.util.Map.Entry;

import com.vaadin.flow.component.dependency.NpmPackage;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;

/**
 * Updates <code>package.json</code> by visiting {@link NpmPackage} annotations found in
 * the classpath. It also visits classes annotated with {@link NpmPackage}
 */
public class TaskUpdatePackages extends NodeUpdater {

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param finder
     *            a reusable class finder
     * @param frontendDependencies
     *            a reusable frontend dependencies
     * @param npmFolder
     *            folder with the `package.json` file
     * @param generatedPath
     *            folder where flow generated files will be placed.
     * @param convertHtml
     *            whether to convert html imports or not during the package
     *            updates
     */
    TaskUpdatePackages(ClassFinder finder,
            FrontendDependencies frontendDependencies, File npmFolder,
            File generatedPath, boolean convertHtml) {
        super(finder, frontendDependencies, npmFolder, generatedPath, convertHtml);
    }

    @Override
    public void execute() {
        try {
            JsonObject packageJson = getPackageJson();
            if (packageJson == null) {
                throw new IllegalStateException("Unable to read '"  + PACKAGE_JSON + "' file in: " + npmFolder) ;
            }

            Map<String, String> deps = frontDeps.getPackages();
            if (convertHtml) {
                addHtmlImportPackages(deps);
            }

            modified = updatePackageJsonDependencies(packageJson, deps);
            modified = updateDefaultDependencies(packageJson) || modified ;

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
        for(Entry<String, String> e : deps.entrySet()) {
            added = addDependency(packageJson, DEPENDENCIES, e.getKey(), e.getValue()) || added;
        }
        return added;
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
