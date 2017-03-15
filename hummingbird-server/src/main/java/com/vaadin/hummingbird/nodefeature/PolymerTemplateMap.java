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

package com.vaadin.hummingbird.nodefeature;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.vaadin.hummingbird.ConstantPoolKey;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.shared.NodeFeatures;
import com.vaadin.hummingbird.template.angular.BindingValueProvider;
import com.vaadin.hummingbird.template.model.ModelDescriptor;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Map for polymer template.
 *
 * @author Vaadin Ltd
 */
public class PolymerTemplateMap extends NodeMap {
    private ModelDescriptor<?> modelDescriptor;
    private HashMap<String, BindingValueProvider> modelBindings = new HashMap<>();

    /**
     * Creates a new template map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public PolymerTemplateMap(StateNode node) {
        super(node);
    }

    /**
     * Sets the descriptor of the model type used by this template.
     *
     * @param modelDescriptor
     *            the model descriptor to set, not <code>null</code>
     */
    public void setModelDescriptor(ModelDescriptor<?> modelDescriptor) {
        assert modelDescriptor != null;
        assert !contains(NodeFeatures.MODEL_DESCRIPTOR);

        this.modelDescriptor = modelDescriptor;
        put(NodeFeatures.MODEL_DESCRIPTOR,
                new ConstantPoolKey(modelDescriptor.toJson()));
    }

    /**
     * Gets the descriptor of the model type used by this template.
     *
     * @return the model descriptor, or <code>null</code> if it has not yet been
     *         set
     */
    public ModelDescriptor<?> getModelDescriptor() {
        return modelDescriptor;
    }

    /**
     * Sets model bindings.
     *
     * @param modelBindings model bindings, not {@code null}
     */
    public void setModelBindings(
            Map<String, BindingValueProvider> modelBindings) {
        if (modelBindings == null) {
            throw new IllegalArgumentException("modelBindings should not be null");
        }

        this.modelBindings = new HashMap<>(modelBindings);
        bindingsToJson(modelBindings).ifPresent(
                json -> put(NodeFeatures.POLYMER_MODEL_BINDINGS, json));
    }

    private static Optional<JsonObject> bindingsToJson(
            Map<String, BindingValueProvider> bindings) {
        return mapToJson(bindings, BindingValueProvider::toJson);
    }

    private static <T> Optional<JsonObject> mapToJson(Map<String, T> map,
            Function<T, JsonValue> toJson) {
        if (map.isEmpty()) {
            return Optional.empty();
        } else {
            JsonObject json = Json.createObject();

            map.forEach((name, value) -> json.put(name, toJson.apply(value)));

            return Optional.of(json);
        }
    }

    /**
     * Gets model bindings.
     *
     * @return model bindings
     */
    public Map<String, BindingValueProvider> getModelBindings() {
        return modelBindings;
    }
}
