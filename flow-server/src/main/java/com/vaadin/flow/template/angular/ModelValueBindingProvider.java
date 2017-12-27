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
package com.vaadin.flow.template.angular;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ModelMap;

import elemental.json.JsonValue;

/**
 * A template binding value provider that produces dynamic value based on a
 * model.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class ModelValueBindingProvider extends AbstractBindingValueProvider {

    /**
     * Type identifier used for model data bindings in JSON messages.
     */
    public static final String TYPE = "model";

    private final String key;

    /**
     * Creates a binding value provider with the given {@code key}.
     * <p>
     * Value for the {@code key} is stored inside a {@link StateNode} directly
     * (via features) so only {@code key} is used to retrieve a dynamic value
     * from the node.
     *
     * @param key
     *            the key of the binding, not null
     */
    public ModelValueBindingProvider(String key) {
        assert key != null;
        this.key = key;
    }

    @Override
    public Object getValue(StateNode node) {
        ModelMap map = ModelMap.get(node);
        int dotLocation = key.lastIndexOf('.');
        if (dotLocation != -1) {
            String modelPath = key.substring(0, dotLocation);
            String modelKey = key.substring(dotLocation + 1);
            return map.resolveModelMap(modelPath).getValue(modelKey);
        } else {
            return map.getValue(key);
        }
    }

    @Override
    public JsonValue toJson() {
        return makeJsonObject(TYPE, key);
    }

}
