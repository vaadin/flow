/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.example.application;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinService;

/**
 * Shows whether the providers registered in {@code META-INF/services} were
 * loaded, one line per provider.
 */
@Route("service-loader")
public class ServiceLoaderView extends Div {

    public static final String INIT_LISTENER_RAN_ID = "init-listener-ran";
    public static final String FILTERED_ROUTE_REGISTERED_ID = "filtered-route-registered";

    public ServiceLoaderView() {
        boolean initListenerRan = VaadinService.getCurrent().getContext()
                .getAttribute(ServiceLoaderInitListener.Ran.class) != null;
        Div initListener = new Div(
                "Init listener from META-INF/services ran: " + initListenerRan);
        initListener.setId(INIT_LISTENER_RAN_ID);

        boolean filteredRouteRegistered = RouteConfiguration
                .forApplicationScope().isRouteRegistered(FilteredView.class);
        Div routeFilter = new Div(
                "Route rejected by the filter from META-INF/services registered: "
                        + filteredRouteRegistered);
        routeFilter.setId(FILTERED_ROUTE_REGISTERED_ID);

        add(initListener, routeFilter);
    }
}
