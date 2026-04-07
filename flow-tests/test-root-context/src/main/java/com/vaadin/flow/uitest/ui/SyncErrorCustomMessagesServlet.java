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

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import java.util.List;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.CustomizedSystemMessages;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.communication.UidlRequestHandler;

/**
 * Servlet that uses custom system messages for sync errors.
 */
@WebServlet(urlPatterns = "/sync-error-custom/*", asyncSupported = true)
public class SyncErrorCustomMessagesServlet extends VaadinServlet {

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        VaadinServletService service = new VaadinServletService(this,
                deploymentConfiguration) {
            @Override
            protected List<RequestHandler> createRequestHandlers()
                    throws ServiceException {
                List<RequestHandler> handlers = super.createRequestHandlers();
                handlers.replaceAll(handler -> {
                    if (handler instanceof UidlRequestHandler) {
                        return new SimulateDesyncUidlRequestHandler();
                    }
                    return handler;
                });
                return handlers;
            }
        };
        service.init();
        return service;
    }

    @Override
    protected void servletInitialized() throws ServletException {
        super.servletInitialized();
        // Configure custom sync error messages
        getService().setSystemMessagesProvider(info -> {
            CustomizedSystemMessages messages = new CustomizedSystemMessages();
            messages.setSyncErrorCaption("Custom Sync Caption");
            messages.setSyncErrorMessage(
                    "Custom sync error message for testing.");
            messages.setSyncErrorURL(null);
            return messages;
        });
    }
}
