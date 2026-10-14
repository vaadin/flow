/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.UUID;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;

public abstract class AbstractReloadView extends Div {

    public static final String TRIGGER_RELOAD_ID = "triggerReload";

    // This ensures the view is not serializable
    private Object preventSerialization = new Object();

    public UUID viewId = UUID.randomUUID();

    protected void addTriggerButton() {
        final NativeButton triggerButton = new NativeButton("Trigger reload",
                event -> Application.triggerReload());
        triggerButton.setId(TRIGGER_RELOAD_ID);
        add(triggerButton);
    }

    protected void addViewId() {
        Div div = new Div();
        Span span = new Span(viewId.toString());
        span.setId("viewId");
        div.add(new Text("The view id is: "), span);
        add(div);
    }
}
