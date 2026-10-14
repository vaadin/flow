/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route("no-response")
public class TestNoResponseView extends Div {

    public static final String DELAY_NEXT_RESPONSE = "delay-next";
    public static final String ADD = "add";
    public static final String ADDED_PREDICATE = "added_";

    private int elements = 0;

    public TestNoResponseView() {
        NativeButton delayNext = new NativeButton("\"Delay\" next response",
                event -> CustomUidlRequestHandler.emptyResponse
                        .add(VaadinSession.getCurrent()));
        delayNext.setId(DELAY_NEXT_RESPONSE);

        NativeButton addElement = new NativeButton("Add element", event -> {
            Div addedElement = new Div("Added element");
            addedElement.setId(ADDED_PREDICATE + elements++);
            add(addedElement);
        });
        addElement.setId(ADD);

        add(delayNext, addElement);
    }
}
