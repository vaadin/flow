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
package com.vaadin.hummingbird.template;

import com.vaadin.hummingbird.JsonCodec;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * @author Vaadin Ltd
 *
 */
public abstract class AbstractTemplateBinding implements TemplateBinding {

    public JsonObject makeJsonObject(String type, String value) {
        JsonObject json = Json.createObject();

        json.put(TYPE_PROPERTY, type);
        json.put(VALUE_PROPERTY, JsonCodec.encodeWithoutTypeInfo(value));

        return json;
    }

}
