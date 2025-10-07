/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.spring.flowsecurity.views;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.internal.AfterNavigationHandler;
import com.vaadin.flow.router.internal.BeforeEnterHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Lists all accessible menu views collected with
 * {@link com.vaadin.flow.server.RouteRegistry#getRegisteredAccessibleMenuRoutes}.
 */
@Route("menu-list")
@PageTitle("Menu List View")
@AnonymousAllowed
public class MenuListView extends Div implements AfterNavigationObserver {

    public MenuListView() {
        add(new Span("Menu List View"));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String targetAccessibleMenuRoutes = event.getSource().getRegistry()
                .getRegisteredAccessibleMenuRoutes(VaadinRequest.getCurrent(),
                        UI.getCurrent().getInternals()
                                .getListeners(BeforeEnterHandler.class).stream()
                                .filter(handler -> handler instanceof BeforeEnterListener)
                                .map(BeforeEnterListener.class::cast).toList())
                .stream()
                .sorted(Comparator.comparing(
                        routeData -> ((routeData.getMenuData() != null)
                                ? routeData.getMenuData().getOrder()
                                : -1)))
                .map(RouteBaseData::getNavigationTarget)
                .map(Class::getSimpleName).collect(Collectors.joining(", "));
        removeAll();

        var span = new Span(targetAccessibleMenuRoutes);
        span.setId("menu-list");
        add(span);
    }
}
