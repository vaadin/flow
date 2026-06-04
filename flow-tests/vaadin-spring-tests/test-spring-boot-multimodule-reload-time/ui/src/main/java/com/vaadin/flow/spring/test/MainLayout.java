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
package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends Div
        implements RouterLayout, AfterNavigationObserver {

    private final H2 viewTitle = new H2();
    private final Div drawer = new Div();
    private final Div content = new Div();

    public MainLayout() {
        getStyle().set("display", "flex").set("flex-direction", "column");

        NativeButton toggle = new NativeButton("☰",
                e -> drawer.setVisible(!drawer.isVisible()));

        Header navbar = new Header(toggle, viewTitle);
        navbar.getStyle().set("display", "flex").set("gap", "0.5rem");

        H1 appName = new H1("My Spring App");
        Div nav = new Div();
        nav.getStyle().set("display", "flex").set("flex-direction", "column");
        UI.getCurrent().getInternals().getRouter().getRegistry()
                .getRegisteredRoutes()
                .forEach(route -> nav.add(new RouterLink(route.getTemplate(),
                        route.getNavigationTarget())));

        drawer.add(new Header(appName), nav, new Footer());
        drawer.getStyle().set("display", "flex").set("flex-direction",
                "column");

        Div body = new Div(drawer, content);
        body.getStyle().set("display", "flex").set("flex", "1");

        add(navbar, body);
    }

    @Override
    public void showRouterLayoutContent(HasElement contentElement) {
        content.removeAll();
        if (contentElement != null) {
            content.getElement().appendChild(contentElement.getElement());
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        Component current = currentContent();
        if (current == null) {
            return "";
        }
        PageTitle title = current.getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }

    private Component currentContent() {
        return content.getChildren().findFirst().orElse(null);
    }
}
