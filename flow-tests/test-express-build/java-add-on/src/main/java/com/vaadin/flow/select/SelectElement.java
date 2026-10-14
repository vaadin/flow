/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.select;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

/**
 * Native select element for selecting items.
 */
@Tag("select")
public class SelectElement
        extends AbstractSinglePropertyField<SelectElement, String> {

    public static final String VALUE_PROPERTY = "value";

    /**
     * Init select element with the selections given.
     *
     * @param options
     *            select options to populate select with
     */
    public SelectElement(String... options) {
        super(VALUE_PROPERTY, "", false);
        if (options.length == 0) {
            throw new IllegalArgumentException(
                    "Select should be given at least one option");
        }

        for (String selection : options) {
            Element option = ElementFactory.createOption(selection);
            getElement().appendChild(option);
        }
        setValue(options[0]);
        getElement().setProperty("selectedIndex", 0);
        getElement().addEventListener("change", e -> {
        }).synchronizeProperty(VALUE_PROPERTY);
    }

}
