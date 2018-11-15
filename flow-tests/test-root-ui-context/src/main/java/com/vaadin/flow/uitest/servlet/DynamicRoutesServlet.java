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
package com.vaadin.flow.uitest.servlet;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.RouteRegistry;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.uitest.ui.Login;

@WebServlet(asyncSupported = true, urlPatterns = { "/rpc/*" })
public class DynamicRoutesServlet extends VaadinServlet {

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        VaadinServletService service = super.createServletService(
                deploymentConfiguration);
        // Add a session init listener which add a dynamic route
        service.addSessionInitListener(this::initSession);
        return service;
    }

    /**
     * Add a new routes at the session initialization phase
     */
    private void initSession(SessionInitEvent event) {
        VaadinSession session = event.getSession();
        RouteRegistry registry = RouteRegistry.getSessionRegistry(session);
        registry.addRoute("login", Login.class);
        registry.addRouteAlias("", Login.class);

        // This is not really a proper place for this call since every time when
        // a new session is initialized it will remove the route which has been
        // already (may be) by previous session initializer.
        //
        // But this shows how routes can be filtered out dynamically even if
        // they are statically registered
        RouteRegistry.getApplicationRegistry().removeRoute("external-route");
    }
}
