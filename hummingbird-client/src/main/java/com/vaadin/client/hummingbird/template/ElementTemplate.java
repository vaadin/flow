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
package com.vaadin.client.hummingbird.template;

import com.vaadin.hummingbird.template.ElementTemplateNode;

import elemental.json.JsonObject;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Client-side representation of an {@link ElementTemplateNode} received from
 * the server. The properties are based on the output of
 * {@link ElementTemplateNode#populateJson(JsonObject)}.
 *
 * @since
 * @author Vaadin Ltd
 */
@JsType(isNative = true)
public interface ElementTemplate extends Template {
    /**
     * Gets the tag name of the described element.
     *
     * @return the tag name
     */
    @JsProperty
    String getTag();

    /**
     * Gets a map of property bindings, as a JSON object.
     *
     * @see Binding
     * @return a JSON object mapping property names to bindings.
     */
    @JsProperty
    JsonObject getProperties();
}
