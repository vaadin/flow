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
package com.vaadin.client.hummingbird.model;

import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;

/**
 * Model type representing a primitive value.
 *
 * @author Vaadin Ltd
 */
public class PrimitiveModelType extends ModelType {

    private static JsMap<String, PrimitiveModelType> types = JsCollections
            .map();
    static {
        types.set("int", new PrimitiveModelType(Double.valueOf(0)));
        types.set("double", new PrimitiveModelType(Double.valueOf(0)));
        types.set("boolean", new PrimitiveModelType(Boolean.FALSE));
        types.set("Integer", new PrimitiveModelType(null));
        types.set("Double", new PrimitiveModelType(null));
        types.set("Boolean", new PrimitiveModelType(null));
        types.set("String", new PrimitiveModelType(null));
    }

    private final Object defaultValue;

    /**
     * Creates a primitive model type with the given default value.
     *
     * @param defaultValue
     *            the default value for this type
     */
    public PrimitiveModelType(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Gets the primitive model type with the given name.
     *
     * @param name
     *            the name of the type to get
     * @return the primitive model type
     */
    public static PrimitiveModelType get(String name) {
        assert types.has(name);

        return types.get(name);
    }

    @Override
    public Object getJsRepresentation(Object value) {
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }
}
