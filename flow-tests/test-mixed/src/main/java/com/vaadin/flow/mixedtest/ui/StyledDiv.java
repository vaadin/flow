/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.mixedtest.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route(value = "styling-div")
@StyleSheet("css/styles.css")
public class StyledDiv extends Div {

    public StyledDiv() {
        setText("Foo");
        setId("styling-div");
    }
}
