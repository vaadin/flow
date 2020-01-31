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
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

import static elemental.json.impl.JsonUtil.stringify;

/**
 * Copies platform versions.json file and converts to simplified format.
 *
 * @author Vaadin Ltd
 *
 */
class ConvertVersionsJson {

    private static final String JS_VERSION = "jsVersion";
    private static final String NPM_NAME = "npmName";
    private final JsonObject convertedObject;

    ConvertVersionsJson(String versions) {
        convertedObject = Json.createObject();
        JsonObject object = Json.parse(versions);

        collectDependencies(object);
    }

    void convert(File file) throws IOException {
        FileUtils.write(file, stringify(convertedObject, 2) + "\n",
                StandardCharsets.UTF_8);
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
        assert obj.hasKey(JS_VERSION);
        String npmName = obj.getString(NPM_NAME);
        String version = obj.getString(JS_VERSION);
        convertedObject.put(npmName, version);

    }
}
