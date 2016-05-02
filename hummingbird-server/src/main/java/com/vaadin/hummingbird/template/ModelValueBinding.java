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

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelMap;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A template binding that produces dynamic value based on a model.
 * <p>
 * Binding is defined by the type (may be "property", "attribute", etc.), key
 * (property name, attribute name, etc.) and default value (defined in a
 * template) for the key.
 * 
 * @author Vaadin Ltd
 *
 */
public class ModelValueBinding extends AbstractTemplateBinding {

    /**
     * Type identifier used for property bindings in JSON messages.
     */
    public static final String PROPERTY = "property";

    public static final String DEFAULT_VALUE = "default_value";

    private final String type;

    private final String key;

    private final String value;

    /**
     * Creates a binding with the given {@code type}, {@code key} and default
     * {@code value}.
     * <p>
     * Value for the {@code key} is stored inside a {@link StateNode} directly
     * (via features) so only {@code key} is used to retrieve a dynamic value
     * from the node. The {@code value} defines default value which is used if
     * model doesn't provide dynamic value (the value has not been set
     * explicitly).
     *
     * @param type
     *            the type of the binding, not null
     * @param key
     *            the key of the binding, not null
     * @param vlaue
     *            the default value of the binding
     */
    public ModelValueBinding(String type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    @Override
    public String getValue(StateNode node) {
        ModelMap feature = node.getFeature(ModelMap.class);
        if (feature.hasValue(key)) {
            return feature.getValue(key);
        } else {
            return value;
        }
    }

    @Override
    public JsonValue toJson() {
        JsonObject object = makeJsonObject(type, key);
        object.put(DEFAULT_VALUE, value);
        return object;
    }

}
