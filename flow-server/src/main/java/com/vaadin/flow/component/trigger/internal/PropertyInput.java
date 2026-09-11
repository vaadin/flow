/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component.trigger.internal;

import java.io.Serializable;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Reads a JavaScript property from a target component's root element at the
 * moment a trigger fires.
 * <p>
 * Common targets and properties:
 * <ul>
 * <li>{@code TextField.value} →
 * {@code new PropertyInput<>(textField, "value", String.class)}
 * <li>{@code Checkbox.checked} →
 * {@code new PropertyInput<>(checkbox, "checked", Boolean.class)}
 * </ul>
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value produced
 * @since 25.2
 */
public class PropertyInput<T> extends Action.Input<T> {

    private final Element target;
    private final String propertyName;

    /**
     * Creates a property input that reads the given JS property from the given
     * target component.
     *
     * @param target
     *            the component to read from, not {@code null}
     * @param propertyName
     *            the JS property name, not {@code null}
     * @param valueType
     *            runtime type of the produced value, not {@code null}
     */
    public PropertyInput(Component target, String propertyName,
            Class<T> valueType) {
        this.target = Objects.requireNonNull(target).getElement();
        this.propertyName = Objects.requireNonNull(propertyName);
        Objects.requireNonNull(valueType);
    }

    @Override
    public JsFunction toJs(Trigger trigger) {
        // Both target (Element) and propertyName (String) are JsFunction
        // captures — JsFunction's wire encoding handles Element-to-DOM-ref
        // mapping and JSON-quotes the property name.
        return JsFunction.of("return $0[$1]", target, propertyName);
    }

    @Override
    public JsonNode evaluate(@Nullable JsonNode eventData) {
        // Reads the value synced to the server-side property map, which stands
        // in for the client-side DOM property the JS would read.
        Serializable raw = target.getPropertyRaw(propertyName);
        return raw == null ? JacksonUtils.nullNode()
                : JacksonUtils.writeValue(raw);
    }
}
