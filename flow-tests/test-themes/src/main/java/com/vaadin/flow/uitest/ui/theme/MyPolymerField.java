/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

/**
 * Polymer version of vaadin text field for testing component theming.
 */
@JsModule("@vaadin/vaadin-text-field/vaadin-text-field.js")
@Tag("vaadin-text-field")
@NpmPackage(value = "@vaadin/vaadin-text-field", version = "2.7.1")
public class MyPolymerField extends Component {

    /**
     * Set the component id.
     *
     * @param id
     *            value to set
     * @return this component
     */
    public Component withId(String id) {
        setId(id);
        return this;
    }
}