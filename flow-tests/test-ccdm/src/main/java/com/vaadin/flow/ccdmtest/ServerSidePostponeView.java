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
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "serverpostponeview", layout = MainLayout.class)
public class ServerSidePostponeView extends Div implements BeforeLeaveObserver {

    NativeButton proceedButton;
    BeforeLeaveEvent lastEvent;

    public ServerSidePostponeView() {
        add(new Text("Server view postpone"));
        setId("serverPostponeView");

        final NativeButton serverViewButton = new NativeButton(
                "Other Server View", buttonClickEvent -> {
                    final UI ui = buttonClickEvent.getSource().getUI().get();

                    ui.navigate(ServerSideView.class);
                });
        serverViewButton.setId("goToServerView");
        add(serverViewButton);

        add(new RouterLink("RouterLink Server View", ServerSideView.class));

        final NativeButton clientViewButton = new NativeButton("Client View",
                buttonClickEvent -> {
                    final UI ui = buttonClickEvent.getSource().getUI().get();

                    ui.navigate("client-view");
                });
        clientViewButton.setId("goToClientView");
        add(clientViewButton);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        event.postpone();
        lastEvent = event;

        if (proceedButton == null) {
            proceedButton = new NativeButton("Proceed", buttonClickEvent -> {
                lastEvent.getContinueNavigationAction().proceed();
            });
            proceedButton.setId("proceedAfterPostpone");
            add(proceedButton);
        }
    }
}
