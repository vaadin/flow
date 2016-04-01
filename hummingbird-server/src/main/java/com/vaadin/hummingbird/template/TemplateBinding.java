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

import java.io.Serializable;

import com.vaadin.hummingbird.StateNode;

import elemental.json.JsonValue;

/**
 * Representation of a static or dynamic value derived from a template. A
 * binding can be used e.g. as the value of an element attribute or as the text
 * in a text node.
 *
 * @since
 * @author Vaadin Ltd
 */
public interface TemplateBinding extends Serializable {
    /**
     * Produces a value for the given state node.
     *
     * @param node
     *            the state node for which to produce a value, not
     *            <code>null</code>
     * @return the binding value
     */
    String getValue(StateNode node);

    /**
     * Produces a string value for the given state node.
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
        String value = getValue(node);
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    /**
     * Encodes this binding as JSON.
     *
     * @return the encoded JSONvalue
     */
    JsonValue toJson();

}
