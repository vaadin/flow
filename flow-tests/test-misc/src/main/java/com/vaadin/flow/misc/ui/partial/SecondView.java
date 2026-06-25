/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui.partial;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route(value = "second", layout = MainLayout.class)
public class SecondView extends Div {
    public static final String SECOND_ID = "hello-second";

    public SecondView() {
        Div helloSecond = new Div("Hello second");
        helloSecond.setId(SECOND_ID);
        add(helloSecond);
    }
}
