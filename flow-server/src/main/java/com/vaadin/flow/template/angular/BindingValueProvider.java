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

import java.io.Serializable;

import com.vaadin.flow.internal.StateNode;

import elemental.json.JsonValue;

/**
 * Representation of a static or dynamic value derived from a template. A
 * binding can be used e.g. as the value of an element attribute or as the text
 * in a text node.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public interface BindingValueProvider extends Serializable {

    String TYPE_PROPERTY = "type";

    String VALUE_PROPERTY = "value";

    /**
     * Produces a value for the given state node.
     * <p>
     * The type of the value is {@link String}, {@link Double}, {@link Boolean}
     * or {@link JsonValue}.
     *
     * @param node
     *            the state node for which to produce a value, not
     *            <code>null</code>
     * @return the binding value
     */
    Object getValue(StateNode node);

    /**
     * Produces a value for the given state node as a string, or the provided
     * default value if the produced value is <code>null</code>.
     *
     * @param node
     *            the state node for which to produce a value, not
     *            <code>null</code>
     * @param defaultValue
     *            the default value to use if the produced value is
     *            <code>null</code>
     * @return the binding value
     */
    default String getValue(StateNode node, String defaultValue) {
        Object value = getValue(node);
        if (value == null) {
            return defaultValue;
        } else {
            // Need to format values the same way as JS does to avoid
            // differences between the pre-rendered and the live version
            if (value instanceof Double) {
                return JavaScriptNumberFormatter.toString((double) value);
            }
            return value.toString();
        }
    }

    /**
     * Encodes this binding as JSON.
     *
     * @return the encoded JSONvalue
     */
    JsonValue toJson();

}
