/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.react.ReactAdapterComponent;
import com.vaadin.flow.function.SerializableConsumer;

@JsModule("./ReactInput.tsx")
@Tag("react-input")
public class ReactInput extends ReactAdapterComponent {

    public ReactInput() {
        this("");
    }

    public ReactInput(String initialValue) {
        setValue(initialValue);
    }

    public String getValue() {
        return getState("value", String.class);
    }

    public void setValue(String value) {
        setState("value", value);
    }

    public void addValueChangeListener(SerializableConsumer<String> onChange) {
        addStateChangeListener("value", String.class, onChange);
    }

}
