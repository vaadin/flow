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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.vaadin.flow.component.dependency.NpmPackage;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Updates <code>package.json</code> by visiting {@link NpmPackage} annotations
 * found in the classpath. It also visits classes annotated with
 * {@link NpmPackage}
 */
public class TaskUpdatePackages extends NodeUpdater {

    private static List<String> PRO_PACKAGES = Arrays.asList(
            "@vaadin/vaadin-board",
            "@vaadin/vaadin-charts",
            "@vaadin/vaadin-confirm-dialog",
            "@vaadin/vaadin-cookie-consent",
            "@vaadin/vaadin-crud",
            "@vaadin/vaadin-grid-pro",
            "@vaadin/vaadin-rich-text-editor");

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
     */
    TaskUpdatePackages(ClassFinder finder,
            FrontendDependencies frontendDependencies, File npmFolder,
            File generatedPath) {
        super(finder, frontendDependencies, npmFolder, generatedPath, false);
    }

    @Override
    public void execute() {
        try {
            Map<String, String> deps = frontDeps.getPackages();
            JsonObject packageJson = getAppPackageJson();
            if (packageJson == null) {
                packageJson = Json.createObject();
            }
            modified = updatePackageJsonDependencies(packageJson, deps);
            if (modified) {
                writeAppPackageFile(packageJson);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean updatePackageJsonDependencies(JsonObject packageJson,
            Map<String, String> deps) {
        boolean added = false;
        boolean hasVaadin = false;
        boolean hasPro = false;
        
        // Add application dependencies
        for(Entry<String, String> e : deps.entrySet()) {
            String pkg = e.getKey();

            // Check whether the package will require shrinkwrap resolution
            hasPro = hasPro || PRO_PACKAGES.contains(pkg);
            hasVaadin = hasPro || hasVaadin || pkg.startsWith("@vaadin");

            // Add the dependency 
            added = addDependency(packageJson, DEPENDENCIES, pkg, e.getValue()) || added;
        }
        // Remove obsolete dependencies
        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        if (dependencies != null) {
            for (String key : dependencies.keys()) {
                if (!deps.containsKey(key)) {
                    dependencies.remove(key);
                }
            }
        }
        // Add the appropriate shrinkwrap based on which packages we have
        if (hasVaadin) {
            String dep = hasPro ? "vaadin-shrinkwrap" : "vaadin-core-shrinkwrap";
            added = addDependency(packageJson, DEPENDENCIES, "@vaadin/"+ dep, "v14.0.0-alpha3") || added;
        }
        return added;
    }

}
