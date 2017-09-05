/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tutorial.routing;

import com.vaadin.flow.html.Div;
import com.vaadin.flow.html.NativeButton;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.tutorial.routing.RoutingRouterConfiguration.CompanyView;
import com.vaadin.flow.tutorial.routing.RoutingRouterConfiguration.HomeView;

@CodeFor("tutorial-routing-navigation.asciidoc")
public class RouterNavigation {

    void navigation() {
        NativeButton button = new NativeButton("Navigate to company");
        button.addClickListener(e -> {
            button.getUI().ifPresent(ui -> ui.navigateTo("company"));
        });

    }

    void routerLink() {
        Div menu = new Div();
        menu.add(new RouterLink("Home", HomeView.class));
        menu.add(new RouterLink("Company", CompanyView.class));
    }

}
