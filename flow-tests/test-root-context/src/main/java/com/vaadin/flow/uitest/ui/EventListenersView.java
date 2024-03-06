/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.EventListenersView", layout = ViewTestLayout.class)
public class EventListenersView extends AbstractDivView {

    @Override
    protected void onShow() {
        AtomicInteger count = new AtomicInteger();
        NativeButton button = new NativeButton("Click me");
        button.setId("click");
        button.addClickListener(evt -> {
            int value = count.incrementAndGet();
            Label label = new Label(String.valueOf(value));
            label.addClassName("count");
            add(label);
            add(button);
        });
        add(button);
    }

}
