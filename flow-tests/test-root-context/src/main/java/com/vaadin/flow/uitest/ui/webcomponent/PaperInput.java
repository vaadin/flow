/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.webcomponent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

@Tag("paper-input")
@NpmPackage(value = "@polymer/paper-input", version = "3.0.2")
@JsModule("@polymer/paper-input/paper-input.js")
public class PaperInput extends Component {
    private static final PropertyDescriptor<String, String> valueDescriptor = PropertyDescriptors
            .propertyWithDefault("value", "");

    public PaperInput() {
        // (this public no-arg constructor is required so that Flow can
        // instantiate beans of this type
        // when they are bound to template elements via the @Id() annotation)
    }

    public PaperInput(String value) {
        setValue(value);
    }

    @Synchronize("value-changed")
    public String getValue() {
        return get(valueDescriptor);
    }

    @Synchronize("invalid-changed")
    public String getInvalid() {
        return getElement().getProperty("invalid");
    }

    public void setValue(String value) {
        set(valueDescriptor, value);
    }
}
