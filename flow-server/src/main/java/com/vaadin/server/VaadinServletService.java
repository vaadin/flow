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

package com.vaadin.server;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.server.communication.PushRequestHandler;
import com.vaadin.server.startup.RouteRegistry;
import com.vaadin.shared.ApplicationConstants;

/**
 * A service implementation connected to a {@link VaadinServlet}.
 *
 * @author Vaadin Ltd
 */
public class VaadinServletService extends VaadinService {

    /**
     * Should never be used directly, always use {@link #getServlet()}.
     */
    private final VaadinServlet servlet;

    /**
     * Creates an instance connected to the given servlet and using the given
     * configuration.
     *
     * @param servlet
     *            the servlet which receives requests
     * @param deploymentConfiguration
     *            the configuration to use
     */
    public VaadinServletService(VaadinServlet servlet,
            DeploymentConfiguration deploymentConfiguration) {
        super(deploymentConfiguration);
        this.servlet = servlet;
    }

    /**
     * Creates a servlet service. This method is for use by dependency injection
     * frameworks etc. {@link #getServlet()} should be overridden (or otherwise
     * intercepted) so it does not return <code>null</code>.
     */
    protected VaadinServletService() {
        this.servlet = null;
    }

    @Override
    protected List<RequestHandler> createRequestHandlers()
            throws ServiceException {
        List<RequestHandler> handlers = super.createRequestHandlers();
        handlers.add(0, new BootstrapHandler());
        if (isAtmosphereAvailable()) {
            try {
                handlers.add(new PushRequestHandler(this));
            } catch (ServiceException e) {
                // Atmosphere init failed. Push won't work but we don't throw a
                // service exception as we don't want to prevent non-push
                // applications from working
                getLogger().warn(
                        "Error initializing Atmosphere. Push will not work.",
                        e);
            }
        }
        return handlers;
    }

    /**
     * Retrieves a reference to the servlet associated with this service.
     * Should be overridden (or otherwise intercepted) if the no-arg
     * constructor is used to prevent NPEs.
     *
     * @return A reference to the VaadinServlet this service is using
     */
    public VaadinServlet getServlet() {
        return servlet;
    }

    @Override
    public String getMimeType(String resourceName) {
        return getServlet().getServletContext().getMimeType(resourceName);
    }

    @Override
    protected boolean requestCanCreateSession(VaadinRequest request) {
        if (isOtherRequest(request)) {
            /*
             * I.e URIs that are not RPC calls or static file requests.
             */
            return true;
        }

        return false;
    }

    private boolean isOtherRequest(VaadinRequest request) {
        return request.getParameter(
                ApplicationConstants.REQUEST_TYPE_PARAMETER) == null;
    }

    public static HttpServletRequest getCurrentServletRequest() {
        return VaadinServletRequest.getCurrent();
    }

    public static VaadinServletResponse getCurrentResponse() {
        return VaadinServletResponse.getCurrent();
    }

    @Override
    public String getServiceName() {
        return getServlet().getServletName();
    }

    @Override
    public String getMainDivId(VaadinSession session, VaadinRequest request) {
        String appId = null;
        try {
            @SuppressWarnings("deprecation")
            URL appUrl = getServlet()
                    .getApplicationUrl((VaadinServletRequest) request);
            appId = appUrl.getPath();
        } catch (MalformedURLException e) {
            // Just ignore problem here
        }

        if (appId == null || "".equals(appId) || "/".equals(appId)) {
            appId = "ROOT";
        }
        appId = appId.replaceAll("[^a-zA-Z0-9]", "");
        // Add hashCode to the end, so that it is still (sort of)
        // predictable, but indicates that it should not be used in CSS
        // and
        // such:
        int hashCode = appId.hashCode();
        if (hashCode < 0) {
            hashCode = -hashCode;
        }
        appId = appId + "-" + hashCode;
        return appId;
    }

    private static final Logger getLogger() {
        return LoggerFactory.getLogger(VaadinServletService.class.getName());
    }

    @Override
    protected RouteRegistry getRouteRegistry() {
        return RouteRegistry.getInstance(getServlet().getServletContext());
    }

}
