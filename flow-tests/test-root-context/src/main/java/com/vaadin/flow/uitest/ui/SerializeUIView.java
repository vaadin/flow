/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.SerializeUIView", layout = ViewTestLayout.class)
public class SerializeUIView extends AbstractDivView {
    public SerializeUIView() {
        Div label = new Div();
        label.setId("message");

        NativeButton button = createButton("Serialize", "serialize", event -> {
            UI ui = UI.getCurrent();
            try {
                byte[] serialize = SerializationUtils.serialize(ui);

                String result = serialize.length > 0
                        ? "Successfully serialized ui"
                        : "Serialization failed";
                label.setText(result);
            } catch (SerializationException se) {
                label.setText(se.getMessage());
            }
        });

        add(label, button);
    }
}
