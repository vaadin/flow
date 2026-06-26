/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ClientSideValueChangeView", layout = ViewTestLayout.class)
public class ClientSideValueChangeView extends AbstractDivView {

    @Override
    protected void onShow() {
        Input input = new Input();
        input.setId("inputfield");

        input.addValueChangeListener(e -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            Span span = new Span("done");
            span.setId("status");
            add(span);
        });

        add(input);

        Input input2 = new Input();
        input2.setId("inputfieldserversetsvalue");

        input2.addValueChangeListener(e -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            input2.setValue("fromserver");
            Span span = new Span("done");
            span.setId("statusserversetsvalue");
            add(span);
        });

        add(input2);
    }

}
