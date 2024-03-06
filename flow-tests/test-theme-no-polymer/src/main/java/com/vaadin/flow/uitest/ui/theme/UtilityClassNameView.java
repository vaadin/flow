/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.theme.UtilityClassNameView")
public class UtilityClassNameView extends Div {

    public UtilityClassNameView() {
        Span styleHeader = new Span("Primary color");
        styleHeader.setClassName("text-primary");
        styleHeader.setId("primary");
        add(styleHeader);
    }
}
