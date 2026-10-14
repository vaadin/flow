/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;

public class OtherExportedComponent extends Div {

    public static final String EXPORTED_ID_TWO = "exported-inner-other";

    public OtherExportedComponent() {
        setId(EXPORTED_ID_TWO);

        NativeButton testButton = new NativeButton("Test Button");
        add(testButton);
    }
}
