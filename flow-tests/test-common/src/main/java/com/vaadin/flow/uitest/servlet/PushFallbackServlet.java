/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.PushRequestHandler;

// Custom servlet for push websockets->long-polling fallback test
@WebServlet(asyncSupported = true, urlPatterns = {"/push-fallback/*" })
public class PushFallbackServlet extends VaadinServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(request, response);
    }

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        PushFallbackServletService service = new PushFallbackServletService(this,
                deploymentConfiguration);
        service.init();
        return service;
    }

    private static class PushFallbackServletService extends VaadinServletService {

        public PushFallbackServletService(VaadinServlet servlet,
                                          DeploymentConfiguration deploymentConfiguration) {
            super(servlet, deploymentConfiguration);
        }

        @Override
        protected List<RequestHandler> createRequestHandlers() throws ServiceException {
            List<RequestHandler> requestHandlers = super.createRequestHandlers();
            requestHandlers.add(new PushFallbackRequestHandler(this));
            return requestHandlers;
        }

        private static class PushFallbackRequestHandler extends PushRequestHandler {

            /**
             * Creates an instance connected to the given service.
             *
             * @param service the service this handler belongs to
             * @throws ServiceException if initialization of Atmosphere fails
             */
            public PushFallbackRequestHandler(VaadinServletService service) throws ServiceException {
                super(service);
            }

            @Override
            public boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response) throws IOException {
                if (HandlerHelper.isRequestType(request,
                        HandlerHelper.RequestType.PUSH)) {
                    if (isWebsocketRequest(request)) {
                        // simulate failure in websocket connection
                        throw new RuntimeException(
                                "Failed to establish websocket connection");
                    } else {
                        return super.handleRequest(session, request, response);
                    }
                }
                return false;
            }

            private boolean isWebsocketRequest(VaadinRequest request) {
                return request != null && "websocket".equals(request.getParameter(
                        "X-Atmosphere-Transport"));
            }
        }
    }
}
