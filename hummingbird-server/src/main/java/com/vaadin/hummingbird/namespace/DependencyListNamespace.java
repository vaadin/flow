/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.namespace;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.ui.Dependency;
import com.vaadin.ui.Dependency.Type;

import elemental.json.Json;
import elemental.json.JsonObject;

public class DependencyListNamespace extends ListNamespace<JsonObject> {

    public static final String KEY_URL = "url";
    public static final String KEY_TYPE = "type";
    public static final String TYPE_STYLESHEET = "css";
    public static final String TYPE_JAVASCRIPT = "js";

    public DependencyListNamespace(StateNode node) {
        super(node, false);
    }

    public void add(Dependency dependency) {
        JsonObject jsonObject = Json.createObject();
        jsonObject.put(KEY_URL, dependency.getUrl());
        if (dependency.getType() == Type.JAVASCRIPT) {
            jsonObject.put(KEY_TYPE, TYPE_JAVASCRIPT);
        } else if (dependency.getType() == Type.STYLESHEET) {
            jsonObject.put(KEY_TYPE, TYPE_STYLESHEET);
        } else {
            throw new IllegalArgumentException(
                    "Unknown type: " + dependency.getType());
        }

        super.add(jsonObject);
    }

}
