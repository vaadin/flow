/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

import com.vaadin.flow.uitest.ui.dependencies.TestVersion;

/**
 * Custom vaadin text field for testing component theming live reload.
 */
@JsModule("@vaadin/vaadin-text-field/vaadin-text-field.js")
@Tag("vaadin-text-field")
@NpmPackage(value = "@vaadin/vaadin-text-field", version = TestVersion.VAADIN)
public class TestThemedTextField extends Component {

    /**
     * Set the component id.
     *
     * @param id
     *            value to set
     */
    public void withId(String id) {
        setId(id);
    }
}