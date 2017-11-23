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

import com.vaadin.flow.JsonCodec;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Abstract binding value provider implementation which provides method to
 * produce initial JsonObject for {@link #toJson()} method.
 *
 * @author Vaadin Ltd
 *
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public abstract class AbstractBindingValueProvider
        implements BindingValueProvider {

    /**
     * Constructs initial {@link JsonObject} instance for {@link #toJson()}
     * method.
     * <p>
     * Two properties are set into json object:
     * <ul>
     * <li>TemplateBinding.TYPE which represents the unique type of the binding
     * <li>TemplateBinding.VALUE_PROPERTY which represents a value of the
     * binding
     * </ul>
     * <p>
     * The properties correspond to the client side generic Binding class which
     * has two methods
     *
     * <pre>
     * <code>
     *  String getType();
     *  String getValue();
     *  </code>
     * </pre>
     *
     * @param type
     *            the binding type
     * @param value
     *            TemplateBinding.VALUE_PROPERTY property value
     * @return the json object
     */
    protected JsonObject makeJsonObject(String type, Object value) {
        JsonObject json = Json.createObject();

        json.put(TYPE_PROPERTY, type);
        json.put(VALUE_PROPERTY, JsonCodec.encodeWithoutTypeInfo(value));

        return json;
    }

}
