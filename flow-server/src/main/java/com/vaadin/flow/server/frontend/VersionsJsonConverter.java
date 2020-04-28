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

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;

/**
 * Converts platform versions file to internal format which doesn't contain
 * extra information.
 * <p>
 * The result contains all framework dependencies as keys and their versions as
 * value.
 *
 * @author Vaadin Ltd
 */
class VersionsJsonConverter {

    private static final String JS_VERSION = "jsVersion";
    private static final String NPM_NAME = "npmName";
    private static final String NPM_VERSION = "npmVersion";
    private final JsonObject convertedObject;
    private final JsonObject userManagedDependencies;

    VersionsJsonConverter(JsonObject platformVersions, JsonObject packageJson) {
        convertedObject = Json.createObject();

        collectDependencies(platformVersions);
        userManagedDependencies = collectUserManagedDependencies(packageJson);
    }

    /**
     * Collect framework managed versions to enforce that the user hasn't
     * changed.
     *
     * @return managed versions json
     */
    JsonObject getManagedVersions() {
        JsonObject json = Json.createObject();
        for(String key : convertedObject.keys()) {
            if (!userManagedDependencies.hasKey(key)) {
                json.put(key, convertedObject.getString(key));
            }
        }
        return json;
    }

    private void collectDependencies(JsonObject obj) {
        for (String key : obj.keys()) {
            JsonValue value = obj.get(key);
            if (!(value instanceof JsonObject)) {
                continue;
            }
            JsonObject json = (JsonObject) value;
            if (json.hasKey(NPM_NAME)) {
                addDependency(json);
            } else {
                collectDependencies(json);
            }
        }
    }

    private void addDependency(JsonObject obj) {
        assert obj.hasKey(NPM_NAME);
        String npmName = obj.getString(NPM_NAME);
        if (obj.hasKey(NPM_VERSION)) {
            convertedObject.put(npmName, obj.getString(NPM_VERSION));
        } else if (obj.hasKey(JS_VERSION)) {
            convertedObject.put(npmName, obj.getString(JS_VERSION));
        } else {
            throw new IllegalStateException("Vaadin code versions file "
                    + "contains unexpected data: dependency '" + npmName
                    + "' has"
                    + " no 'npmVersion'/'jsVersion' . "
                    + "Please report a bug in https://github.com/vaadin/platform/issues/new");
        }
    }

    /**
     * Collect all dependencies that the user has changed that do not match the
     * flow managed dependency versions.
     *
     * @param packageJson
     *         package.json Json object
     * @return collection of user managed dependencies
     */
    private JsonObject collectUserManagedDependencies(JsonObject packageJson) {
        JsonObject json = Json.createObject();
        JsonObject vaadinDep;
        if(packageJson.hasKey(VAADIN_DEP_KEY) && packageJson.getObject(VAADIN_DEP_KEY).hasKey(DEPENDENCIES)) {
            vaadinDep = packageJson.getObject(VAADIN_DEP_KEY).getObject(DEPENDENCIES);
        } else {
            vaadinDep = Json.createObject();
        }

        if(packageJson.hasKey(DEPENDENCIES)) {
            JsonObject dependencies = packageJson.getObject(DEPENDENCIES);

            for (String key : dependencies.keys()) {
                if (isUserChanged(key, vaadinDep, dependencies)) {
                    json.put(key, dependencies.getString(key));
                }
            }
        }

        return json;
    }

    private boolean isUserChanged(String key, JsonObject vaadinDep,
            JsonObject dependencies) {
        if (vaadinDep.hasKey(key)) {
            FrontendVersion vaadin = new FrontendVersion(
                    vaadinDep.getString(key));
            FrontendVersion dep = new FrontendVersion(
                    dependencies.getString(key));
            return !vaadin.isEqualTo(dep);
        }
        // User changed if not in vaadin dependency
        return true;
    }
}
