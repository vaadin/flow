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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.webcomponent.MyView")
public class MyView extends Div {

    static final String APP_TEXT_ID = "app_text";

    public MyView() {
        final Span textSpan = new Span("This is the application view");
        textSpan.setId(APP_TEXT_ID);
        add(textSpan);
    }

}
