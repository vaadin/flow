/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServiceInitListener;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.communication.ServletBootstrapHandler;

/**
 * Servlet service class that allows customization of the service URL
 * (client-server communication base URL).
 */
public class SpringVaadinServletService extends VaadinServletService {

    private String serviceUrl;

    /**
     * Create a servlet service instance that allows the use of a custom service
     * URL.
     *
     * @param servlet
     * @param deploymentConfiguration
     * @param serviceUrl
     *            custom service URL to use (relative to context path, starting
     *            with a slash) or null for default
     * @throws ServiceException
     */
    public SpringVaadinServletService(VaadinServlet servlet,
            DeploymentConfiguration deploymentConfiguration, String serviceUrl)
            throws ServiceException {
        super(servlet, deploymentConfiguration);
        this.serviceUrl = serviceUrl;
    }

    @Override
    protected List<RequestHandler> createRequestHandlers()
            throws ServiceException {
        List<RequestHandler> handlers = super.createRequestHandlers();
        // replace bootstrap handler with a custom one if service URL set
        if (serviceUrl != null) {
            // need to keep the position of the handler on the list
            for (int i = 0; i < handlers.size(); ++i) {
                if (handlers.get(i) instanceof ServletBootstrapHandler) {
                    handlers.set(i, new ServletBootstrapHandler() {
                        @Override
                        protected String getServiceUrl(BootstrapContext context) {
                            return context.getRequest().getContextPath()
                                    + serviceUrl;
                        }
                    });
                }
            }
        }
        return handlers;
    }

    /**
     * Find the Spring web application context related to the servlet context.
     *
     */
    public WebApplicationContext getWebApplicationContext() {
        return WebApplicationContextUtils
                .getWebApplicationContext(getServlet().getServletContext());
    }

    @Override
    protected Iterator<VaadinServiceInitListener> getServiceInitListeners() {
        // Collect from both sources to return a combined iterator
        ArrayList<VaadinServiceInitListener> listeners = new ArrayList<>();

        Iterator<VaadinServiceInitListener> superListeners = super.getServiceInitListeners();
        superListeners.forEachRemaining(listeners::add);

        Map<String, VaadinServiceInitListener> listenerBeans = getWebApplicationContext()
                .getBeansOfType(VaadinServiceInitListener.class);
        listeners.addAll(listenerBeans.values());

        return listeners.iterator();
    }
}
