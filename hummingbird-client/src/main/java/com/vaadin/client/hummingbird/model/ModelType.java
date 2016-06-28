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

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Represents a specific model type.
 *
 * @author Vaadin Ltd
 */
public abstract class ModelType {
    /**
     * Gets an model type implementation based on a JSON definition.
     *
     * @param type
     *            a JSON of the type, not <code>null</code>
     * @return a model type definition, not <code>null</code>
     */
    public static ModelType fromJson(JsonValue type) {
        assert type != null;

        switch (type.getType()) {
        case STRING:
            return PrimitiveModelType.get(type.asString());
        case OBJECT:
            return new BeanModelType((JsonObject) type);
        case ARRAY:
            throw new IllegalArgumentException(
                    "Not yet supporting JS proxies for arrays");
        default:
            throw new IllegalArgumentException(
                    "Cannot support type descriptor for " + type.getType());
        }
    }

    public abstract Object getJsRepresentation(Object value);
}
