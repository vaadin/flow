/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;

@Route(value = "prevent-leaving", layout = MainLayout.class)
public class PreventLeavingView extends Div implements BeforeLeaveObserver {
    private String preventRoute;

    public PreventLeavingView() {
        Input input = new Input();
        input.setId("preventRouteInput");
        add(input);
        NativeButton submitPreventRoute = new NativeButton(
                "Submit prevent route");
        submitPreventRoute.addClickListener(event -> {
            preventRoute = input.getValue();
            String preventedMessage = String
                    .format("preventing navigation to '%s'", preventRoute);
            Paragraph paragraph = new Paragraph(preventedMessage);
            paragraph.setClassName("prevented-route");
            add(paragraph);
        });
        submitPreventRoute.setId("preventRouteButton");
        add(submitPreventRoute);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (preventRoute != null
                && event.getLocation().getPath().equals(preventRoute)) {
            event.postpone();
        }
    }
}
