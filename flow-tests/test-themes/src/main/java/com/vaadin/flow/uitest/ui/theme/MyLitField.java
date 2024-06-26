/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

/**
 * LIT version of vaadin radio button for testing component theming.
 */
@JsModule("@vaadin/vaadin-radio-button/vaadin-radio-button.js")
@Tag("vaadin-radio-button")
@NpmPackage(value = "@vaadin/vaadin-radio-button", version = "1.6.0")
public class MyLitField extends Component {

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
