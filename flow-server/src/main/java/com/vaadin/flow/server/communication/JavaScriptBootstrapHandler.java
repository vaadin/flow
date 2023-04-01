/*
 * Copyright 2000-2023 Vaadin Ltd.
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

package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.function.Function;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.BootstrapHandlerHelper;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.InvalidLocationException;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.LocationUtil;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

/**
 * Processes a 'start' request type from the client to initialize server session
 * and UI. It returns a JSON response with everything needed to bootstrap flow
 * views.
 * <p>
 * The handler is for client driven projects where `index.html` does not contain
 * bootstrap data. Bootstrapping is the responsibility of the `@vaadin/flow`
 * client that is able to ask the server side to create the vaadin session and
 * do the bootstrapping lazily.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 */
public class JavaScriptBootstrapHandler extends BootstrapHandler {

    /**
     * Custom BootstrapContext for {@link JavaScriptBootstrapHandler}.
     */
    public static class JavaScriptBootstrapContext extends BootstrapContext {

        /**
         * Creates a new context instance using the given parameters.
         *
         * @param request
         *            the request object
         * @param response
         *            the response object
         * @param ui
         *            the UI object
         * @param callback
         *            a callback that is invoked to resolve the context root
         *            from the request
         */
        public JavaScriptBootstrapContext(VaadinRequest request,
                VaadinResponse response, UI ui,
                Function<VaadinRequest, String> callback) {
            super(request, response, ui.getInternals().getSession(), ui,
                    callback, JavaScriptBootstrapContext::initRoute);
        }

        private static Location initRoute(VaadinRequest request) {
            // This can be called in two ways:
            // 1. From the JS during the second phase of a two phase init. In
            // this case, the
            // location is included in the REQUEST_LOCATION_PARAMETER and the
            // request always
            // goes to the servlet path
            // 2. During the first request if eagerServerLoad is enabled. In
            // this case, the
            // location comes from the pathinfo + query parameters in the
            // request
            String path = request.getParameter(
                    ApplicationConstants.REQUEST_LOCATION_PARAMETER);
            String params = request
                    .getParameter(ApplicationConstants.REQUEST_QUERY_PARAMETER);
            if (path != null) {
                return new Location(path, QueryParameters.fromString(params));
            }

            // Case 2, use the request
            if (request instanceof VaadinServletRequest) {
                return new Location(request.getPathInfo(),
                        QueryParameters
                                .fromString(((VaadinServletRequest) request)
                                        .getQueryString()));
            } else {
                return new Location(request.getPathInfo(),
                        QueryParameters.empty());
            }
        }

    }

    /**
     * Creates a new bootstrap handler with default page builder.
     */
    public JavaScriptBootstrapHandler() {
        super(context -> null);
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return HandlerHelper.isRequestType(request, RequestType.INIT)
                && isServletRootRequest(request);
    }

    private boolean isServletRootRequest(VaadinRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null || "".equals(pathInfo) || "/".equals(pathInfo);
    }

    protected String getRequestUrl(VaadinRequest request) {
        return ((VaadinServletRequest) request).getRequestURL().toString();
    }

    @Override
    protected BootstrapContext createAndInitUI(Class<? extends UI> uiClass,
            VaadinRequest request, VaadinResponse response,
            VaadinSession session) {

        BootstrapContext context = super.createAndInitUI(UI.class, request,
                response, session);
        JsonObject config = context.getApplicationParameters();

        String requestURL = getRequestUrl(request);

        PushConfiguration pushConfiguration = context.getUI()
                .getPushConfiguration();
        pushConfiguration.setPushServletMapping(
                BootstrapHandlerHelper.determinePushServletMapping(session));

        AppShellRegistry registry = AppShellRegistry
                .getInstance(session.getService().getContext());
        registry.modifyPushConfiguration(pushConfiguration);

        config.put("requestURL", requestURL);

        return context;
    }

    @Override
    protected void initializeUIWithRouter(BootstrapContext context, UI ui) {
    }

    @Override
    protected BootstrapContext createBootstrapContext(VaadinRequest request,
            VaadinResponse response, UI ui,
            Function<VaadinRequest, String> callback) {
        return new JavaScriptBootstrapContext(request, response, ui, callback);
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        try {
            // #9443 Use error code 400 for bad location and don't create UI
            // Normally caught by IndexHtmlRequestHandler, but checking here too
            // for handcrafted requests
            String pathAndParams = request.getParameter(
                    ApplicationConstants.REQUEST_LOCATION_PARAMETER);
            if (pathAndParams == null) {
                throw new InvalidLocationException(
                        "Location parameter missing from bootstrap request to server.");
            }
            LocationUtil.parsePathToSegments(pathAndParams, false);
        } catch (InvalidLocationException invalidLocationException) {
            response.sendError(400, "Invalid location: "
                    + invalidLocationException.getMessage());
            return true;
        }

        HandlerHelper.setResponseNoCacheHeaders(response::setHeader,
                response::setDateHeader);

        writeResponse(response, getInitialJson(request, response, session));
        return true;
    }

    /**
     * Gets the service URL as a URL relative to the request URI.
     *
     * @param vaadinRequest
     *            the request
     * @return the relative service URL
     */
    protected static String getServiceUrl(VaadinRequest vaadinRequest) {
        return BootstrapHandlerHelper.getServiceUrl(vaadinRequest);
    }

    private JsonObject getStats() {
        JsonObject stats = Json.createObject();
        UsageStatistics.getEntries().forEach(entry -> {
            String name = entry.getName();
            String version = entry.getVersion();

            JsonObject json = Json.createObject();
            json.put("is", name);
            json.put("version", version);

            String escapedName = Json.create(name).toJson();
            stats.put(escapedName, json);
        });
        return stats;
    }

    private JsonValue getErrors(VaadinService service) {
        JsonObject errors = Json.createObject();
        Optional<DevModeHandler> devModeHandler = DevModeHandlerManager
                .getDevModeHandler(service);
        if (devModeHandler.isPresent()) {
            String errorMsg = devModeHandler.get().getFailedOutput();
            if (errorMsg != null) {
                errors.put("webpack-dev-server", errorMsg);
            }
        }
        return errors.keys().length > 0 ? errors : Json.createNull();
    }

    private void writeResponse(VaadinResponse response, JsonObject json)
            throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpURLConnection.HTTP_OK);
        response.getOutputStream()
                .write(JsonUtil.stringify(json).getBytes("UTF-8"));
    }

    /**
     * Returns the JSON object with the application config and UIDL info that
     * can be used in the bootstrapper to embed that info in the initial page.
     *
     * @param request
     *            the vaadin request.
     * @param response
     *            the response.
     * @param session
     *            the vaadin session.
     * @return the initial application JSON.
     */
    protected JsonObject getInitialJson(VaadinRequest request,
            VaadinResponse response, VaadinSession session) {

        BootstrapContext context = createAndInitUI(UI.class, request, response,
                session);

        JsonObject initial = Json.createObject();

        boolean productionMode = context.getSession().getConfiguration()
                .isProductionMode();

        JsonObject appConfig = context.getApplicationParameters();

        appConfig.put("productionMode", Json.create(productionMode));
        appConfig.put("appId", context.getAppId());
        appConfig.put("uidl", getInitialUidl(context.getUI()));
        initial.put("appConfig", appConfig);

        if (context.getPushMode().isEnabled()) {
            initial.put("pushScript", getPushScript(context));
        }
        if (!session.getConfiguration().isProductionMode()) {
            initial.put("stats", getStats());
        }
        initial.put("errors", getErrors(request.getService()));

        return initial;
    }

}
