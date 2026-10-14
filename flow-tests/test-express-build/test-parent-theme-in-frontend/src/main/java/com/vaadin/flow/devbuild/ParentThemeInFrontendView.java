/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.devbuild;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.devbuild.ParentThemeInFrontendView")
public class ParentThemeInFrontendView extends Div {

    public ParentThemeInFrontendView() {
        add(new Paragraph("Red color text from parent 'reusable-theme'"));
        add(new Span("Green color text from parent 'other-theme'"));
    }
}
