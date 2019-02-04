/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServiceInitListener;

public class TestingServiceInitListener implements VaadinServiceInitListener {
    public static final String DYNAMICALLY_REGISTERED_ROUTE = "dynamically-registered-route";
    private static AtomicInteger initCount = new AtomicInteger();
    private static AtomicInteger requestCount = new AtomicInteger();

    private boolean redirected;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(
                (UIInitListener & Serializable) initEvent -> initEvent.getUI()
                        .addBeforeEnterListener(
                                (BeforeEnterListener & Serializable) e -> {
                                    if (!redirected
                                            && ServiceInitListenersView.class
                                                    .equals(e
                                                            .getNavigationTarget())) {
                                        e.rerouteTo(e.getLocation().getPath(),
                                                22);
                                        redirected = true;
                                    }
                                }));
        initCount.incrementAndGet();

        RouteConfiguration configuration = RouteConfiguration.forApplicationScope();
        if (!configuration.isPathRegistered(DYNAMICALLY_REGISTERED_ROUTE)) {
            configuration.setRoute(
                    DYNAMICALLY_REGISTERED_ROUTE,
                    DynamicallyRegisteredRoute.class);
        }

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

}
