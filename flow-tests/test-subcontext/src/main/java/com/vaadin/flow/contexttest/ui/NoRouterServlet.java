/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
