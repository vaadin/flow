/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;option&gt;</code> element.
 *
 * @since 2.0
 */
@Tag(Tag.OPTION)
public class Option extends HtmlComponent implements HasText {

    private static final PropertyDescriptor<String, String> valueDescriptor = PropertyDescriptors
            .propertyWithDefault("value", "");

    public Option() {
    }

    public Option(String value) {
        setValue(value);
        setText(value);
    }

    /**
     * Sets the model value of the option.
     *
     * @param value
     *            the model value
     */
    public void setValue(String value) {
        set(valueDescriptor, value);
    }

    /**
     * Gets the model value of the option.
     *
     * @return the model value
     */
    public String getValue() {
        return get(valueDescriptor);
    }

}
