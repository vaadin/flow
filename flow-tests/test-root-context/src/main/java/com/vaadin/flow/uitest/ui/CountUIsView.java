/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.CountUIsView")
public class CountUIsView extends Div {

    public CountUIsView() {
        Div count = new Div();
        add(count);
        count.setId("uis");

        // Don't show the UIs number right away on the component CTOR. Make it
        // explicit via action. At this point all UIs should be already
        // initialized
        NativeButton showUisNumber = new NativeButton("Show created UIs number",
                event -> count.setText(String.valueOf(
                        TestingServiceInitListener.getNotNavigatedUis())));
        add(showUisNumber);
    }
}
