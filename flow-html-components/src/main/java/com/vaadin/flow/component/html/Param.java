/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;param&gt;</code> element for
 * <code>&lt;param&gt;</code> element.
 *
 * @see HtmlObject
 *
 * @author Vaadin Ltd
 *
 */
@Tag(Tag.PARAM)
public class Param extends HtmlComponent {

    private static final PropertyDescriptor<String, String> nameDescriptor = PropertyDescriptors
            .attributeWithDefault("name", "");

    private static final PropertyDescriptor<String, Optional<String>> valueDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("value", "");

    /**
     * Creates a new <code>&lt;param&gt;</code> component.
     */
    public Param() {
    }

    /**
     * Creates a new <code>&lt;param&gt;</code> component with given name and
     * value.
     *
     * @see #setName(String)
     * @see #setValue(String)
     *
     * @param name
     *            a name attribute value
     * @param value
     *            a value attribute value
     */
    public Param(String name, String value) {
        setName(name);
        setValue(value);
    }

    /**
     * Sets a "value" attribute.
     *
     * @param value
     *            "value" attribute value
     */
    public void setValue(String value) {
        set(valueDescriptor, value);
    }

    /**
     * Sets a "name" attribute value.
     *
     * @param name
     *            a "name" attribute value
     */
    public void setName(String name) {
        set(nameDescriptor, name);
    }

    /**
     * Gets the "name" attribute value.
     *
     * @see #setName(String)
     *
     * @return the "name" attribute value
     */
    public String getName() {
        return get(nameDescriptor);
    }

    /**
     * Gets the "value" attribute.
     *
     * @see #setValue(String)
     *
     * @return the "value" attribute value
     */
    public Optional<String> getValue() {
        return get(valueDescriptor);
    }

}
