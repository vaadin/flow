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
package com.vaadin.flow.test;

import java.util.Comparator;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouterLink;

/**
 * Lists every server side route registered in the application, so the test
 * views can be browsed from the root without keeping a list up to date by hand.
 * Routes with required parameters are listed without a link, as they can not be
 * navigated to without values.
 */
@Route("")
public class IndexView extends Div {

    public IndexView() {
        getStyle().set("font-family", "sans-serif").setPadding("1em 2em");

        UnorderedList routes = new UnorderedList();
        routes.getStyle().setPadding("0").set("list-style", "none")
                .set("line-height", "1.8");
        RouteConfiguration.forApplicationScope().getAvailableRoutes().stream()
                .filter(route -> route.getNavigationTarget() != IndexView.class)
                .sorted(Comparator.comparing(
                        route -> route.getNavigationTarget().getSimpleName()))
                .map(IndexView::createRouteItem).forEach(routes::add);
        add(new H1("Flow default configuration tests"), routes);
    }

    private static ListItem createRouteItem(RouteData route) {
        Class<? extends Component> target = route.getNavigationTarget();
        boolean requiresParameters = route.getRouteParameters().values()
                .stream().anyMatch(parameter -> !parameter.isOptional()
                        && !parameter.isVarargs());
        if (requiresParameters) {
            return new ListItem(new Span(target.getSimpleName()),
                    createPath("/" + route.getTemplate()));
        }
        RouterLink link = new RouterLink(target);
        link.setText(target.getSimpleName());
        link.getStyle().set("text-decoration", "none");
        return new ListItem(link, createPath("/" + link.getHref()));
    }

    private static Span createPath(String path) {
        Span span = new Span(" (" + path + ")");
        span.getStyle().setColor("gray").set("font-family", "monospace");
        return span;
    }
}
