/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.routing;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.osgi.OSGiMarker;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

@Route("com.vaadin.flow.uitest.ui.PushLayout")
public class PushLayout extends Div
        implements RouterLayout, BeforeEnterObserver {

    public static String FORWARD_PATH = "forward-no-route";

    public PushLayout() {
        setId("push-layout");
        Lookup lookup = VaadinService.getCurrent().getContext()
                .getAttribute(Lookup.class);
        if (lookup.lookup(OSGiMarker.class) == null) {
            PushConfiguration pushConfiguration = UI.getCurrent()
                    .getPushConfiguration();
            pushConfiguration.setPushMode(PushMode.AUTOMATIC);
            pushConfiguration.setTransport(Transport.WEBSOCKET_XHR);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (FORWARD_PATH.equals(event.getLocation().getPath())) {
            event.forwardTo(ForwardPage.class);
        }
    }

}
