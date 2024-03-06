/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;

/**
 * Component representing a <code>&lt;select&gt;</code> element.
 *
 * @since 2.0
 */
@Tag(Tag.SELECT)
public class Select extends AbstractSinglePropertyField<Select, String> {

    private static final String VALUE_PROPERTY = "value";
    private Map<Option, Object> items = new HashMap<>();

    /**
     * Creates an empty select.
     */
    public Select() {
        super(VALUE_PROPERTY, "", false);
        getElement().addPropertyChangeListener(VALUE_PROPERTY, "change",
                event -> {
                });
    }

    public void addItem(Object item, String name) {
        Option option = new Option(name);
        items.put(option, item);
        addOption(option);
    }

    public Optional<Object> getItem() {
        Optional<Option> selectedValue = getSelectedValue();
        if (selectedValue.isPresent()) {
            return Optional.of(items.get(selectedValue.get()));
        }

        return Optional.empty();
    }

    /**
     * Adds an option to this select.
     *
     * @param option
     *            A not <code>null</code> Option
     */
    private void addOption(Option option) {
        if (option != null) {
            getElement().appendChild(option.getElement());
        }
    }

    /**
     * Gets the selected String value, if any.
     *
     * @return the selected value in the select element
     */
    private Optional<Option> getSelectedValue() {
        Element el = getElement();
        String selectedValue = el.getProperty(VALUE_PROPERTY);
        return items.keySet().stream()
                .filter(option -> option.getValue().equals(selectedValue))
                .findFirst();
    }

}
