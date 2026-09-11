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
package com.vaadin.flow.uitest.ui;

import java.util.stream.Collectors;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Renders a breadcrumb trail built from the instance-free route hierarchy
 * ({@link RouteParent} + {@link RouteConfiguration#getRouteHierarchy}), with
 * the title of each entry resolved without instantiating the ancestor route.
 */
@Route(value = "com.vaadin.flow.uitest.ui.RouteHierarchyView", layout = ViewTestLayout.class)
@RouteParent(RouteHierarchyParentView.class)
@PageTitle("Order details")
public class RouteHierarchyView extends AbstractDivView {

    @Override
    protected void onShow() {
        removeAll();
        String breadcrumb = RouteConfiguration.forApplicationScope()
                .getRouteHierarchy(
                        RouteHierarchyView.class, RouteParameters.empty())
                .stream()
                .map(reference -> MenuRegistry.getTitle(
                        reference.navigationTarget(),
                        reference.routeParameters()))
                .collect(Collectors.joining(" / "));

        Div div = new Div();
        div.setId("breadcrumb");
        div.setText(breadcrumb);
        add(div);
    }
}
