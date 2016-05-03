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

import elemental.json.JsonValue;

/**
 * A template binding that produces dynamic value based on a model.
 * <p>
 * Binding is defined by the type (may be "text", "property", "attribute", etc.)
 * and key (property name, attribute name, etc.)
 *
 * @author Vaadin Ltd
 */
public class ModelValueBinding extends AbstractTemplateBinding {

    /**
     * Type identifier used for text bindings in JSON messages.
     */
    public static final String TEXT = "text";

    /**
     * Type identifier used for property bindings in JSON messages.
     */
    public static final String PROPERTY = "property";

    private final String type;

    private final String key;

    /**
     * Creates a binding with the given {@code type} and {@code key}.
     * <p>
     * Value for the {@code key} is stored inside a {@link StateNode} directly
     * (via features) so only {@code key} is used to retrieve a dynamic value
     * from the node.
     *
     * @param type
     *            the type of the binding, not null
     * @param key
     *            the key of the binding, not null
     */
    public ModelValueBinding(String type, String key) {
        assert key != null;
        this.key = key;
        this.type = type;
    }

    @Override
    public Object getValue(StateNode node) {
        return node.getFeature(ModelMap.class).getValue(key);
    }

    @Override
    public JsonValue toJson() {
        return makeJsonObject(type, key);
    }

}
