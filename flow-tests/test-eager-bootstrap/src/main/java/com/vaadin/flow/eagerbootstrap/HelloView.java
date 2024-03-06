/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.eagerbootstrap;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("hello")
public class HelloView extends Div {
    public HelloView() {
        setId("view");
        setText("This is the Hello view");
    }
}
