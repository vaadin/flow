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
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.shared.Registration;

@Tag("paper-slider")
@NpmPackage(value = "@polymer/paper-slider", version = "3.0.1")
@JsModule("@polymer/paper-slider/paper-slider.js")
public class PaperSlider extends Component implements HasValue {
    @DomEvent("value-change")
    public static class ValueChangeEvent extends ComponentEvent<PaperSlider> {
        public ValueChangeEvent(PaperSlider source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public PaperSlider() {
        getElement().addPropertyChangeListener("value", "value-change",
                event -> {
                });
    }

    public void setPin(boolean pin) {
        getElement().setProperty("pin", pin);
    }

    public boolean isPin() {
        return getElement().getProperty("pin", false);
    }

    public Registration addValueChangeListener(
            ComponentEventListener<ValueChangeEvent> valueChangeListener) {
        return super.addListener(ValueChangeEvent.class, valueChangeListener);
    }
}
