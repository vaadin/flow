/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("slow-response")
public class SlowResponseView extends Div {

    public static final String SLOW_ADD = "slowAdd";
    public static final String ADD = "add";
    public static final String ADDED_PREDICATE = "added_";

    private int elements = 0;

    public SlowResponseView() {
        int messageTimeoutMillis = UI.getCurrent().getSession().getService()
                .getDeploymentConfiguration().getMaxMessageSuspendTimeout();
        add(new Span("Max message suspend timeout: " + messageTimeoutMillis));
        NativeButton slowAddElement = new NativeButton(
                "Add element (slow response)", event -> {
                    slowAddElement(messageTimeoutMillis + 1000);
                });
        slowAddElement.setId(SLOW_ADD);

        NativeButton addElement = new NativeButton("Add element", event -> {
            addElement();
        });
        addElement.setId(ADD);

        add(slowAddElement, addElement);
    }

    private void addElement() {
        Div addedElement = new Div("Added element");
        addedElement.setId(ADDED_PREDICATE + elements++);
        add(addedElement);
    }

    private void slowAddElement(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        addElement();
    }
}
