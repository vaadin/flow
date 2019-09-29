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

package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.function.Function;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.JavaScriptBootstrapUI;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.ServletHelper;
import com.vaadin.flow.server.ServletHelper.RequestType;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.theme.ThemeDefinition;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

/**
 * Processes a 'start' request type from the client to initialize server session
 * and UI. It returns a JSON response with everything needed to bootstrapping
 * flow views.
 * <p>
 * The handler is for client driven projects where `index.html` does not contain
 * bootstrap data. Bootstraping is the responsability of the `@vaadin/flow`
 * client that is able to ask the server side to create the vaadin session and
 * do the boostrapping lazily.
 *
 */
public class JavaScriptBootstrapHandler extends BootstrapHandler {

    /**
     * Custom BootstrapContext for {@link JavaScriptBootstrapHandler}.
     */
    private static class JavaScriptBootstrapContext extends BootstrapContext {
        private JavaScriptBootstrapContext(VaadinRequest request,
                                             VaadinResponse response, UI ui,
                                             Function<VaadinRequest, String> callback) {
            super(request, response, ui.getInternals().getSession(), ui, callback);
        }

        @Override
        protected Optional<ThemeDefinition> getTheme() {
            return Optional.empty();
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
        return !request.getService().getDeploymentConfiguration().isCompatibilityMode()
                && ServletHelper.isRequestType(request, RequestType.INIT);
    }

    protected String getRequestUrl(VaadinRequest request) {
        return ((VaadinServletRequest)request).getRequestURL().toString();
    }

    @Override
    protected BootstrapContext createAndInitUI(
            Class<? extends UI> uiClass, VaadinRequest request,
            VaadinResponse response, VaadinSession session) {

        BootstrapContext context = super.createAndInitUI(JavaScriptBootstrapUI.class,
                request, response, session);
        JsonObject config = context.getApplicationParameters();

        String requestURL = getRequestUrl(request);
        String serviceUrl = getServiceUrl(request);

        String pushURL = context.getSession().getConfiguration().getPushURL();
        if (pushURL == null) {
            pushURL = serviceUrl;
        } else {
            try {
                URI uri = new URI(serviceUrl);
                pushURL = uri.resolve(new URI(pushURL)).toASCIIString();
            } catch (URISyntaxException exception) {
                throw new IllegalStateException(String.format(
                        "Can't resolve pushURL '%s' based on the service URL '%s'",
                        pushURL, serviceUrl), exception);
            }
        }
        PushConfiguration pushConfiguration = context.getUI()
                .getPushConfiguration();
        pushConfiguration.setPushUrl(pushURL);

        config.put(ApplicationConstants.SERVICE_URL, serviceUrl);

        // TODO(manolo) this comment is left intentionally because we
        // need to revise whether the info passed to client is valid
        // when initialising push. Right now ccdm is not doing
        // anything with push.
        config.put("pushScript", getPushScript(context));
        config.put("requestURL", requestURL);

        return context;
    }

    @Override
    protected void initializeUIWithRouter(VaadinRequest request, UI ui) {
        String route = request.getParameter(ApplicationConstants.REQUEST_LOCATION_PARAMETER);
        if (route == null) {
            route = (String)request.getAttribute(ApplicationConstants.REQUEST_LOCATION_PARAMETER);
        }
        if (route != null) {
            try {
                route = URLDecoder.decode(route, "UTF-8").replaceFirst("^/+", "");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e);
            }
            Location location = new Location(route);
            ui.getRouter().initializeUI(ui, location);
        }
    }

    @Override
    protected BootstrapContext createBootstrapContext(VaadinRequest request,
                                                      VaadinResponse response, UI ui, Function<VaadinRequest, String> callback) {
        return new JavaScriptBootstrapContext(request, response, ui, callback);
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response) throws IOException {

        ServletHelper.setResponseNoCacheHeaders(response::setHeader,
                response::setDateHeader);

        writeResponse(response, getInitialJson(request, response, session));
        return true;
    }

    @Override
    public JsonObject getInitialJson(VaadinRequest request,
            VaadinResponse response, VaadinSession session) {

        JsonObject initial = super.getInitialJson(request, response, session);

        if (!session.getConfiguration().isProductionMode()) {
            initial.put("stats", getStats());
        }
        initial.put("errors", getErrors());

        return initial;
    }

    private String getServiceUrl(VaadinRequest request) {
        // get service url from 'url' parameter
        String url = request.getParameter("url");
        // if 'url' parameter was not available, use request url
        if (url == null) {
            url = ((VaadinServletRequest) request).getRequestURL().toString();
        }
        return url
                // replace http:// or https:// with // to work with https:// proxies
                // which proxies to the same http:// url
                .replaceFirst("^https?://", "//");
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

    private JsonValue getErrors() {
        JsonObject errors = Json.createObject();
        DevModeHandler devMode = DevModeHandler.getDevModeHandler();
        if (devMode != null) {
            String errorMsg = devMode.getFailedOutput();
            if (errorMsg != null) {
                errors.put("webpack-dev-server", errorMsg);
            }
        }
        return errors.keys().length > 0 ? errors : Json.createNull();
    }

    private void writeResponse(VaadinResponse response, JsonObject json) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpURLConnection.HTTP_OK);
        response.getOutputStream().write(JsonUtil.stringify(json).getBytes("UTF-8"));
    }
}
