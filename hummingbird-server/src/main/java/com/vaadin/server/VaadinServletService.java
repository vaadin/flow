/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.server.communication.PushRequestHandler;
import com.vaadin.server.communication.ServletBootstrapHandler;

/**
 * A service implementation connected to a {@link VaadinServlet}.
 *
 * @author Vaadin
 * @since
 */
public class VaadinServletService extends VaadinService {
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

    @Override
    protected List<RequestHandler> createRequestHandlers()
            throws ServiceException {
        List<RequestHandler> handlers = super.createRequestHandlers();
        handlers.add(0, new ServletBootstrapHandler());
        if (isAtmosphereAvailable()) {
            try {
                handlers.add(new PushRequestHandler(this));
            } catch (ServiceException e) {
                // Atmosphere init failed. Push won't work but we don't throw a
                // service exception as we don't want to prevent non-push
                // applications from working
                getLogger().log(Level.WARNING,
                        "Error initializing Atmosphere. Push will not work.",
                        e);
            }
        }
        return handlers;
    }

    /**
     * Retrieves a reference to the servlet associated with this service.
     *
     * @return A reference to the VaadinServlet this service is using
     */
    public VaadinServlet getServlet() {
        return servlet;
    }

    @Override
    public String getStaticFileLocation(VaadinRequest request) {
        VaadinServletRequest servletRequest = (VaadinServletRequest) request;
        String staticFileLocation;
        // if property is defined in configurations, use that
        staticFileLocation = getDeploymentConfiguration().getResourcesPath();
        if (staticFileLocation != null) {
            return staticFileLocation;
        }

        // the last (but most common) option is to generate default location
        // from request by finding how many "../" should be added to the
        // requested path before we get to the context root

        String requestedPath = servletRequest.getServletPath();
        String pathInfo = servletRequest.getPathInfo();
        if (pathInfo != null) {
            requestedPath += pathInfo;
        }

        return getCancelingRelativePath(requestedPath);
    }

    /**
     * Gets a relative path that cancels the provided path. This essentially
     * adds one .. for each part of the path to cancel.
     *
     * @param pathToCancel
     *            the path that should be canceled
     * @return a relative path that cancels out the provided path segment
     */
    public static String getCancelingRelativePath(String pathToCancel) {
        StringBuilder sb = new StringBuilder(".");
        // Start from i = 1 to ignore first slash
        for (int i = 1; i < pathToCancel.length(); i++) {
            if (pathToCancel.charAt(i) == '/') {
                sb.append("/..");
            }
        }
        return sb.toString();
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
        // TODO This should be refactored in some way. It should not be
        // necessary to check all these types.
        return (!ServletHelper.isAppRequest(request)
                && !ServletHelper.isFileUploadRequest(request)
                && !ServletHelper.isHeartbeatRequest(request)
                && !ServletHelper.isPublishedFileRequest(request)
                && !ServletHelper.isUIDLRequest(request)
                && !ServletHelper.isPushRequest(request));
    }

    public static HttpServletRequest getCurrentServletRequest() {
        VaadinRequest currentRequest = VaadinService.getCurrentRequest();
        if (currentRequest instanceof VaadinServletRequest) {
            return (VaadinServletRequest) currentRequest;
        } else {
            return null;
        }
    }

    public static VaadinServletResponse getCurrentResponse() {
        return (VaadinServletResponse) VaadinService.getCurrentResponse();
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
        return Logger.getLogger(VaadinServletService.class.getName());
    }

}
