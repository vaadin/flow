/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.routecontext;

import jakarta.annotation.PostConstruct;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.quarkus.annotation.RouteScoped;

@Route(value = "route", layout = MainLayout.class)
@RouteScoped
public class RootView extends AbstractCountedView {

    public static final String MASTER = "master";
    public static final String REROUTE = "reroute";
    public static final String POSTPONE = "postpone";
    public static final String EVENT = "event";
    public static final String ERROR = "ERROR";
    public static final String PROXIED = "proxied-route-scope";

    @PostConstruct
    private void init() {
        add(new Div(new Span("ROOT")),
                new Div(new RouterLink(MASTER, MasterView.class)),
                new Div(new RouterLink(REROUTE, RerouteView.class)),
                new Div(new RouterLink(POSTPONE, PostponeView.class)),
                new Div(new RouterLink(EVENT, EventView.class)),
                new Div(new RouterLink(ERROR, ErrorView.class)),
                new Div(new RouterLink(PROXIED, ProxiedView.class)));
    }

}
