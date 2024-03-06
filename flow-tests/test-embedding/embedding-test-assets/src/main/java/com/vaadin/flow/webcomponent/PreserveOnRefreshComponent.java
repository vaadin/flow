/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;

@Tag("preserved-component")
public class PreserveOnRefreshComponent extends Div {

    public PreserveOnRefreshComponent() {
        Input input = new Input();
        input.setId("value");
        add(input);
    }
}
