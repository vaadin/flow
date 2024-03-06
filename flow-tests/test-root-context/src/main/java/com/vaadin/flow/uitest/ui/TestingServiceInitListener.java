/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

public class TestingServiceInitListener implements VaadinServiceInitListener {
    public static final String DYNAMICALLY_REGISTERED_ROUTE = "dynamically-registered-route";
    private static AtomicInteger initCount = new AtomicInteger();
    private static AtomicInteger requestCount = new AtomicInteger();
    private static Set<UI> notNavigatedUis = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    private boolean redirected;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(this::handleUIInit);
        initCount.incrementAndGet();

        RouteConfiguration configuration = RouteConfiguration
                .forApplicationScope();
        // lock registry from any other updates to get registrations correctly.
        configuration.getHandledRegistry().update(() -> {
            if (!configuration.isPathRegistered(DYNAMICALLY_REGISTERED_ROUTE)) {
                configuration.setRoute(DYNAMICALLY_REGISTERED_ROUTE,
                        DynamicallyRegisteredRoute.class);
            }
        });

        event.addRequestHandler((session, request, response) -> {
            requestCount.incrementAndGet();
            return false;
        });
    }

    public static int getInitCount() {
        return initCount.get();
    }

    public static int getRequestCount() {
        return requestCount.get();
    }

    public static int getNotNavigatedUis() {
        return notNavigatedUis.size();
    }

    private void handleUIInit(UIInitEvent event) {
        notNavigatedUis.add(event.getUI());
        event.getUI().addBeforeEnterListener(
                (BeforeEnterListener & Serializable) e -> {
                    if (e.getNavigationTarget() != null) {
                        notNavigatedUis.remove(e.getUI());
                    }
                    if (!redirected && ServiceInitListenersView.class
                            .equals(e.getNavigationTarget())) {
                        e.rerouteTo(e.getLocation().getPath(), 22);
                        redirected = true;
                    }
                });
    }

}
