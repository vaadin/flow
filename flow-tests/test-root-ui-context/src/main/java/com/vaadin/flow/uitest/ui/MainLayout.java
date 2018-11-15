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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.RouteRegistry;
import com.vaadin.flow.router.RouterLayout;

public class MainLayout extends Div implements RouterLayout {

    public MainLayout() {
        NativeButton logout = new NativeButton("Logout", event -> logout());
        add(logout);
    }

    private void logout() {
        UI ui = getUI().get();
        RouteRegistry registry = RouteRegistry
                .getSessionRegistry(ui.getSession());
        registry.removeRoute("user");
        registry.removeRoute("admin");
        registry.addRoute("login", Login.class);
        ui.navigate("login");
    }

}
