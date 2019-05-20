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

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Creates the <code>package.json</code> if missing.
 */
public class TaskCreatePackageJson extends NodeUpdater {

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param npmFolder
     *            folder with the `package.json` file.
     * @param generatedPath
     *            folder where flow generated files will be placed.
     */
    TaskCreatePackageJson(File npmFolder, File generatedPath) {
        super(null, null, npmFolder, generatedPath);
    }

    @Override
    public void execute() {
        try {
            modified = false;
            JsonObject mainContent = getMainPackageJson();
            if (mainContent == null) {
                mainContent = Json.createObject();
            }
            modified = updateMainDefaultDependencies(mainContent);
            if (modified) {
                writeMainPackageFile(mainContent);
            }
            JsonObject customContent = getAppPackageJson();
            if (customContent == null) {
                customContent = Json.createObject();
                updateAppDefaultDependencies(customContent);
                writeAppPackageFile(customContent);
                modified = true;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
