/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.client.flow.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.util.JsObject;
import com.vaadin.client.flow.util.JsObject.PropertyDescriptor;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Model type definition for beans. A bean type contains a set of named
 * properties that each has a defined type. A bean is represented as a
 * JavaScript object with properties triggered to access the corresponding
 * property in a model map instance.
 *
 * @author Vaadin Ltd
 */
public class BeanModelType extends ModelType {

    private JsonObject properties;

    /**
     * Creates a new model type based on a JSON type description.
     *
     * @param properties
     *            the JSON object defining the type, not <code>null</code>
     */
    public BeanModelType(JsonObject properties) {
        this.properties = properties;
    }

    private static native PropertyDescriptor createPropertyDescriptor(
            NodeMap map, String name, JsonValue type)
    /*-{
        return {
          enumerable: true,
          get: $entry(function() {
            return @BeanModelType::get(*)(map, name, type);
          })
        }
    }-*/;

    private static Object get(NodeMap map, String name, JsonValue jsonType) {
        Object value = map.getProperty(name).getValue();

        ModelType type = ModelType.fromJson(jsonType);

        return type.getJsRepresentation(value);
    }

    /**
     * Creates a JS proxy instance for accessing the given model map based on
     * the properties in this type.
     *
     * @param model
     *            the model map to read property values from
     * @return a JS proxy for the model map
     */
    public Object createProxy(NodeMap model) {
        Object proxy = JavaScriptObject.createObject();

        for (String name : properties.keys()) {
            JsonValue propertyTypeJson = properties.get(name);

            JsObject.defineProperty(proxy, name,
                    createPropertyDescriptor(model, name, propertyTypeJson));
        }

        return proxy;
    }

    @Override
    public Object getJsRepresentation(Object value) {
        if (value == null) {
            return null;
        } else {
            assert value instanceof StateNode;
            StateNode node = (StateNode) value;

            assert node.hasFeature(NodeFeatures.TEMPLATE_MODELMAP);
            return createProxy(node.getMap(NodeFeatures.TEMPLATE_MODELMAP));
        }
    }
}
