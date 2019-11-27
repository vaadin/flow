/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.contexttest.ui;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;

public class NoRouterServlet extends VaadinServlet {

    @SuppressWarnings("serial")
    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        // service doesn't use router actually. UI class is responsible to
        // show and update the content by itself with only root route available
        VaadinServletService service = new VaadinServletService(this,
                deploymentConfiguration) {
            @Override
            public Router getRouter() {
                Router router = new Router(getRouteRegistry()) {
                    @Override
                    public int navigate(UI ui, Location location,
                            NavigationTrigger trigger) {
                        return HttpServletResponse.SC_OK;
                    }
                };
                return router;
            }
        };
        service.init();
        return service;
    }
}
