/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.component.html;

import java.util.Optional;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasOrderedComponents;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;

/**
 * Component representing a <code>&lt;object&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@Tag(Tag.OBJECT)
public class HtmlObject extends HtmlContainer implements
        ClickNotifier<HtmlObject>, HasOrderedComponents, Focusable<HtmlObject> {

    private static final PropertyDescriptor<String, String> dataDescriptor = PropertyDescriptors
            .attributeWithDefault("data", "");

    private static final PropertyDescriptor<String, Optional<String>> typeDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("type", "");

    /**
     * Creates a new <code>&lt;object&gt;</code> component.
     */
    public HtmlObject() {
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data and
     * type attribute values.
     *
     * @see #setData(String)
     * @see #setType(String)
     *
     *
     * @param data
     *            a data attribute value
     * @param type
     *            a type attribute value
     */
    public HtmlObject(String data, String type) {
        setData(data);
        setType(type);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data, type
     * attribute values and and "param" components.
     *
     * @see #setData(String)
     * @see #setType(String)
     * @see #add(Component...)
     *
     *
     * @param data
     *            a data attribute value
     * @param type
     *            a type attribute value
     * @param params
     *            parameter components
     */
    public HtmlObject(String data, String type, Param... params) {
        setData(data);
        setType(type);
        add(params);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data
     * resource and type value.
     *
     * @see #setData(AbstractStreamResource)
     * @see #setType(String)
     *
     * @param data
     *            the resource value, not null
     * @param type
     *            a type attribute value
     */
    public HtmlObject(AbstractStreamResource data, String type) {
        setData(data);
        setType(type);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data
     * resource, type value and "param" components.
     *
     * @see #setData(String)
     * @see #setType(String)
     * @see #add(Component...)
     *
     *
     * @param data
     *            a data attribute value
     * @param type
     *            a type attribute value
     * @param params
     *            parameter components
     */
    public HtmlObject(AbstractStreamResource data, String type,
            Param... params) {
        setData(data);
        setType(type);
        add(params);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data and
     * "param" components.
     *
     * @see #setData(String)
     * @see #add(Component...)
     *
     *
     * @param data
     *            a data attribute value
     * @param params
     *            parameter components
     */
    public HtmlObject(String data, Param... params) {
        setData(data);
        add(params);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data
     * resource value.
     *
     * @see #setData(AbstractStreamResource)
     *
     * @param data
     *            the resource value, not {@code null}
     */
    public HtmlObject(AbstractStreamResource data) {
        setData(data);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data
     * resource value and "param" components.
     *
     * @see #setData(AbstractStreamResource)
     * @see #add(Component...)
     *
     * @param data
     *            the resource value, not {@code null}
     * @param params
     *            parameter components
     */
    public HtmlObject(AbstractStreamResource data, Param... params) {
        setData(data);
        add(params);
    }

    /**
     * Sets the "data" attribute value.
     *
     * @param data
     *            a "data" attribute value
     */
    public void setData(String data) {
        set(dataDescriptor, data);
    }

    /**
     * Sets the {@link StreamResource} URL as "data" attribute value .
     *
     * @param data
     *            a "data" attribute value,, not {@code null}
     */
    public void setData(AbstractStreamResource data) {
        getElement().setAttribute("data", data);
    }

    /**
     * Gets the "data" attribute value.
     *
     * @return the "data" attribute value
     *
     * @see #setData(String)
     * @see #setData(AbstractStreamResource)
     */
    public String getData() {
        return get(dataDescriptor);
    }

    /**
     * Sets the "type" attribute value.
     *
     * @param type
     *            a "type" attribute value
     */
    public void setType(String type) {
        set(typeDescriptor, type);
    }

    /**
     * Gets the "type" attribute value.
     *
     * @see #setType(String)
     *
     * @return the "type" attribute value
     */
    public Optional<String> getType() {
        return get(typeDescriptor);
    }

}
