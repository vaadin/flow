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

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

/**
 * Reads a JavaScript property from a target component's root element at the
 * moment a trigger fires.
 * <p>
 * Common targets and properties:
 * <ul>
 * <li>{@code TextField.value} →
 * {@code new PropertyArgument<>(textField, "value", String.class)}
 * <li>{@code Checkbox.checked} →
 * {@code new PropertyArgument<>(checkbox, "checked", Boolean.class)}
 * </ul>
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value produced
 */
public class PropertyArgument<T> extends AbstractArgument<T> {

    public static final String TYPE_ID = "flow:property";

    private final Element target;
    private final String propertyName;

    /**
     * Creates a property argument that reads the given JS property from the
     * given target component.
     *
     * @param target
     *            the component to read from, not {@code null}
     * @param propertyName
     *            the JS property name, not {@code null}
     * @param valueType
     *            runtime type of the produced value, not {@code null}
     */
    public PropertyArgument(Component target, String propertyName,
            Class<T> valueType) {
        super(TYPE_ID, valueType);
        this.target = Objects.requireNonNull(target).getElement();
        this.propertyName = Objects.requireNonNull(propertyName);
    }

    /**
     * @return the target element
     */
    public Element getTarget() {
        return target;
    }

    /**
     * @return the property name being read
     */
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public void buildClientConfig(ConfigContext context) {
        context.put("property", propertyName);
        context.put("element", context.referenceElement(target));
    }
}
