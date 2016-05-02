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
 * A template binding that produces dynamic value for some key.
 * <p>
 * The key can represent e.g. property/text value/attribute.
 *
 * @author Vaadin Ltd
 */
public class ModelValueBinding extends AbstractTemplateBinding {

    /**
     * Type identifier used for text bindings in JSON messages.
     */
    public static final String TEXT = "text";

    private final String key;
    private final String type;

    /**
     * Creates a binding with the given key.
     * <p>
     * Value for the {@code key} is stored inside a {@link StateNode} directly
     * (via features) so only {@code key} is required to be able to get a value.
     * <p>
     * The {@code type} is a unique identifier for the binding which is used on
     * the client side how to handle the binding instance.
     *
     * @param type
     *            the type of the binding
     * @param key
     *            the key of the binding
     */
    public ModelValueBinding(String type, String key) {
        this.type = type;
        this.key = key;
    }

    @Override
    public String getValue(StateNode node) {
        return node.getFeature(ModelMap.class).getValue(key);
    }

    @Override
    public JsonValue toJson() {
        return makeJsonObject(type, key);
    }

}
