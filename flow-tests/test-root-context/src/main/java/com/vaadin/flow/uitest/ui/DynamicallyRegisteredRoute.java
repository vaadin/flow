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

public class DynamicallyRegisteredRoute extends Div {

    public static final String TEXT = "This route has been registered dynamically with a ServiceInitListener";
    public static final String ID = "foobar";

    public DynamicallyRegisteredRoute() {
        setId(ID);
        setText(TEXT);
    }
}
