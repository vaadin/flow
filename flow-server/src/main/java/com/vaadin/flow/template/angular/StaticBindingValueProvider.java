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

import elemental.json.JsonValue;

/**
 * A template binding value provider that always produces the same value.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class StaticBindingValueProvider extends AbstractBindingValueProvider {
    /**
     * Type identifier used for static bindings in JSON messages.
     */
    public static final String TYPE = "static";

    private final String value;

    /**
     * Creates a binding value provider with the given value.
     *
     * @param value
     *            the value of the binding
     */
    public StaticBindingValueProvider(String value) {
        this.value = value;
    }

    @Override
    public String getValue(StateNode node) {
        return value;
    }

    @Override
    public JsonValue toJson() {
        return makeJsonObject(TYPE, value);
    }
}
