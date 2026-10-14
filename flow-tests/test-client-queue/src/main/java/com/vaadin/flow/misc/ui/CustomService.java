/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import java.util.List;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.communication.UidlRequestHandler;

public class CustomService extends VaadinServletService {

    public CustomService(VaadinServlet servlet,
            DeploymentConfiguration deploymentConfiguration) {
        super(servlet, deploymentConfiguration);
    }

    @Override
    protected List<RequestHandler> createRequestHandlers()
            throws ServiceException {
        List<RequestHandler> requestHandlers = super.createRequestHandlers();
        requestHandlers.replaceAll(handler -> {
            if (handler instanceof UidlRequestHandler) {
                return new CustomUidlRequestHandler();
            }
            return handler;
        });
        return requestHandlers;
    }
}
