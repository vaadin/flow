/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.client;

import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Test helper for creating Dependency JSON objects. Since Dependency no longer
 * has toJson() method, this helper provides convenient methods for tests to
 * create JSON representations.
 */
public class DependencyTestHelper {

    /**
     * Converts a Dependency object to its JSON representation.
     *
     * @param dependency
     *            the dependency to convert
     * @return JSON representation of the dependency
     */
    public static JsonObject dependencyToJson(Dependency dependency) {
        JsonObject json = Json.createObject();
        json.put(Dependency.KEY_URL, dependency.getUrl());
        json.put(Dependency.KEY_TYPE, dependency.getType().name());
        json.put(Dependency.KEY_LOAD_MODE, dependency.getLoadMode().name());
        if (dependency.getId() != null) {
            json.put(Dependency.KEY_ID, dependency.getId());
        }
        return json;
    }

    /**
     * Creates a JSON representation of a dependency with the given parameters.
     * Convenience method that creates a Dependency object and converts it to
     * JSON.
     *
     * @param type
     *            the dependency type
     * @param url
     *            the dependency URL
     * @param loadMode
     *            the load mode
     * @return JSON representation of the dependency
     */
    public static JsonObject createDependencyJson(Dependency.Type type,
            String url, LoadMode loadMode) {
        return dependencyToJson(new Dependency(type, url, loadMode));
    }

    /**
     * Creates a JSON representation of a dependency with the given parameters
     * and ID. Convenience method that creates a Dependency object and converts
     * it to JSON.
     *
     * @param type
     *            the dependency type
     * @param url
     *            the dependency URL
     * @param loadMode
     *            the load mode
     * @param id
     *            the dependency ID
     * @return JSON representation of the dependency
     */
    public static JsonObject createDependencyJson(Dependency.Type type,
            String url, LoadMode loadMode, String id) {
        return dependencyToJson(new Dependency(type, url, loadMode, id));
    }
}
