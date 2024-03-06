/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

@Route(value = "serverforwardview", layout = MainLayout.class)
public class ServerSideForwardView extends Div
        implements BeforeEnterObserver, HasUrlParameter<Boolean> {

    NativeButton proceedButton;
    BeforeLeaveEvent lastEvent;

    Boolean parameter;

    public ServerSideForwardView() {
        add(new Text("Server view forward"));
        setId("serverForwardView");

        final NativeButton forwardViewButton = new NativeButton(
                "Open Server View which does forward to Client View",
                buttonClickEvent -> {
                    final UI ui = buttonClickEvent.getSource().getUI().get();

                    ui.navigate(ForwardView.class);
                });
        forwardViewButton.setId("goToServerForwardView");
        add(forwardViewButton);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter Boolean parameter) {
        this.parameter = parameter;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (Boolean.TRUE.equals(parameter)) {
            event.forwardTo("client-view");
        }

        parameter = null;
    }

    @Route(value = "serverforwardingview", layout = MainLayout.class)
    public static class ForwardView extends Div implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.forwardTo("client-view");
        }
    }
}
