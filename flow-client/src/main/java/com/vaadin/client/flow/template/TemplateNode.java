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
package com.vaadin.client.flow.template;

import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.dom.DomElement;
import com.vaadin.flow.shared.JsonConstants;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Client-side representation of a generic
 * {@link com.vaadin.flow.template.angular.TemplateNode} received from the
 * server. The properties are based on the output of
 * {@link com.vaadin.flow.template.angular.TemplateNode#toJson(java.util.function.Consumer)}
 * on the server.
 *
 * @author Vaadin Ltd
 */
@JsType(isNative = true)
public interface TemplateNode {
    /**
     * Gets the template type. The possible type values are defined as constants
     * named <code>TYPE</code> in subclasses of
     * {@link com.vaadin.flow.template.angular.TemplateNode} on the server.
     *
     * @return the template type
     */
    @JsProperty
    String getType();

    /**
     * Gets an array of child template ids. The corresponding template instances
     * can be found using {@link TemplateRegistry#get(int)}.
     * <p>
     * The name childrenIds is used instead of children because of colliding
     * with {@link DomElement} API.
     *
     * @return and array of child template ids
     */
    @JsProperty(name = JsonConstants.CHILD_TEMPLATE_KEY)
    JsArray<Double> getChildrenIds();

    /**
     * Gets the id that this template node has in its {@link TemplateRegistry}.
     *
     * @return the template node id, or <code>null</code> if this node has not
     *         been registered
     */
    @JsProperty
    Double getId();

    /**
     * Sets the id of this template node. The id is set by
     * {@link TemplateRegistry#register(int, TemplateNode)}.
     *
     * @param id
     *            the id of the template node, not <code>null</code>
     */
    @JsProperty
    void setId(Double id);
}
