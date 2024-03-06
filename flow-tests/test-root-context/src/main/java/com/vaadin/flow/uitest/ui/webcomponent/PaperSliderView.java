/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.webcomponent;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.webcomponent.PaperSliderView", layout = ViewTestLayout.class)
public class PaperSliderView extends AbstractDivView {
    static final String VALUE_TEXT_ID = "valueText";
    static final String CHANGE_VALUE_ID = "changeValue";
    static final int INITIAL_VALUE = 75;
    static final int UPDATED_VALUE = 50;

    public PaperSliderView() {
        Div valueText = new Div();
        valueText.setId(VALUE_TEXT_ID);
        PaperSlider paperSlider = new PaperSlider();
        paperSlider.setPin(true);
        paperSlider.addValueChangeListener(e -> {
            String text = "Value: " + e.getSource().getValue();
            text += " (set on " + (e.isFromClient() ? "client" : "server")
                    + ')';
            valueText.setText(text);
        });
        paperSlider.setValue(INITIAL_VALUE);
        add(paperSlider, valueText,
                createButton("Set value to " + UPDATED_VALUE, CHANGE_VALUE_ID,
                        e -> paperSlider.setValue(UPDATED_VALUE)));
    }
}
