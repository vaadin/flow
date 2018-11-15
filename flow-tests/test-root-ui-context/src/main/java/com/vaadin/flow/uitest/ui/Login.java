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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.RouteRegistry;
import com.vaadin.flow.server.VaadinSession;

public class Login extends Div {

    public Login() {
        NativeButton userLogin = new NativeButton("Login as a user",
                event -> showUserView());
        NativeButton adminLogin = new NativeButton("Login as an admin",
                event -> showUserView());
        add(userLogin, adminLogin);
    }

    private void showUserView() {
        removeLoginRoute();
        RouteRegistry registry = RouteRegistry.getSessionRegistry(getSession());
        registry.removeRoute("admin");
        registry.addRoute("user", UserView.class, MainLayout.class);
        getUI().get().navigate("user");
    }

    private void showAdminView() {
        removeLoginRoute();
        RouteRegistry registry = RouteRegistry.getSessionRegistry(getSession());
        registry.removeRoute("user");
        registry.addRoute("admin", AdminView.class, MainLayout.class);

        // "about" route is registered statically and is available via
        // application scope which means that the session scope may override it.
        // So we remove route which has been added a the bootstrap phase and it
        // should not be available for the CURRENT session. But it's still
        // available in application and for any other session which is not in
        // the "admin" mode.
        registry.removeRoute("about");

        getUI().get().navigate("admin");
    }

    private RouteRegistry removeLoginRoute() {
        RouteRegistry registry = RouteRegistry.getSessionRegistry(getSession());
        registry.removeRoute(Login.class);
        return registry;
    }

    private VaadinSession getSession() {
        return getUI().get().getSession();
    }
}
