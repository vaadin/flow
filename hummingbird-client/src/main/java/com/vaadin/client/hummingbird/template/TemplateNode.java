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

import com.vaadin.client.hummingbird.collection.JsArray;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Client-side representation of a generic
 * {@link com.vaadin.hummingbird.template.TemplateNode} received from the
 * server. The properties are based on the output of
 * {@link com.vaadin.hummingbird.template.TemplateNode#toJson(java.util.function.Consumer)}
 * on the server.
 *
 * @since
 * @author Vaadin Ltd
 */
@JsType(isNative = true)
public interface TemplateNode {
    /**
     * Gets the template type. The possible type values are defined as constants
     * named <code>TYPE</code> in subclasses of
     * {@link com.vaadin.hummingbird.template.TemplateNode} on the server.
     *
     * @return the template type
     */
    @JsProperty
    String getType();

    /**
     * Gets an array of child template ids. The corresponding template instances
     * can be found using {@link TemplateRegistry#get(int)}.
     *
     * @return and array of child template ids
     */
    @JsProperty
    JsArray<Double> getChildren();
}
