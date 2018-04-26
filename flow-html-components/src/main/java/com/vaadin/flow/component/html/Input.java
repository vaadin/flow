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
package com.vaadin.flow.component.html;

import java.util.Optional;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;

/**
 * Component representing an <code>&lt;input&gt;</code> element.
 *
 * @author Vaadin Ltd
 */
@Tag(Tag.INPUT)
public class Input extends AbstractSinglePropertyField<Input, String>
        implements Focusable<Input>, HasSize, HasStyle {

    private static final PropertyDescriptor<String, Optional<String>> placeholderDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("placeholder", "");

    private static final PropertyDescriptor<String, String> typeDescriptor = PropertyDescriptors
            .attributeWithDefault("type", "text");

    /**
     * Creates a new input without any specific type.
     */
    public Input() {
        super("value", "", false);

        setSynchronizedEvent("change");
    }

    /**
     * Sets the placeholder text that is shown if the input is empty.
     *
     * @param placeholder
     *            the placeholder text to set, or <code>null</code> to remove
     *            the placeholder
     */
    public void setPlaceholder(String placeholder) {
        set(placeholderDescriptor, placeholder);
    }

    /**
     * Gets the placeholder text.
     *
     * @see #setPlaceholder(String)
     *
     * @return an optional placeholder, or an empty optional if no placeholder
     *         has been set
     */
    public Optional<String> getPlaceholder() {
        return get(placeholderDescriptor);
    }

    /**
     * Sets the type of this input.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input">
     *      Overview of supported type values</a>
     *
     * @param type
     *            the type, not <code>null</code>
     */
    public void setType(String type) {
        set(typeDescriptor, type);
    }

    /**
     * Gets the type of this input.
     *
     * @return the input type, by default "text"
     */
    public String getType() {
        return get(typeDescriptor);
    }

}
