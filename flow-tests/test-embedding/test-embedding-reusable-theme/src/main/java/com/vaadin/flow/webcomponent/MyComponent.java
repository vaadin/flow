/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

import com.vaadin.flow.uitest.ui.dependencies.TestVersion;

/**
 * Polymer version of vaadin text field for testing component theming.
 */
@JsModule("@vaadin/text-field/vaadin-text-field.js")
@Tag("vaadin-text-field")
@NpmPackage(value = "@vaadin/vaadin-text-field", version = TestVersion.VAADIN)
public class MyComponent extends Component {

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
