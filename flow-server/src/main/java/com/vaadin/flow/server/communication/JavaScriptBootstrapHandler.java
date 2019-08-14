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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.ServletHelper;
import com.vaadin.flow.server.ServletHelper.RequestType;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.Version;
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
     * Custom UI for {@link JavaScriptBootstrapHandler}.
     */
    public static class JavaScriptBootstrapUI extends UI {
        public static final String NO_NAVIGATION =
                "Classic flow navigation is not supported for clien-side projects";

        /**
         * Connect a client with the server side UI.
         *
         * @param clientElementTag
         *            client side element tag
         * @param clientElementId
         *            client side element id
         * @param flowRoute
         *            flow route that should be attached to the client element
         */
        @ClientCallable
        public void connectClient(String clientElementTag, String clientElementId, String flowRoute) {

            // Get the flow view that the user wants to navigate to.
            final Element viewElement = getViewForRoute(flowRoute).getElement();

            // Create flow reference for the client outlet element
            final Element wrapperElement = new Element(clientElementTag);
            wrapperElement.appendChild(viewElement);

            // Connect server with client
            getElement().getStateProvider().appendVirtualChild(
                    getElement().getNode(), wrapperElement,
                    NodeProperties.INJECT_BY_ID, clientElementId);

            // Inform the client, that everything went fine.
            wrapperElement.executeJs("$0.serverConnected()");
        }

        private HasElement getViewForRoute(String route) {
            Location location = new Location(route);
            Optional<NavigationState> navigationState = this.getRouter()
                    .resolveNavigationTarget(location);
            if (navigationState.isPresent()) {
                NavigationState currentState = navigationState.get();
                Class<? extends Component> routeTargetType = currentState
                        .getNavigationTarget();
                List<RouterLayout> layouts = getRouterLayouts(currentState,
                        routeTargetType);
                return getInternals().constructComponentWithLayouts(
                        getInstanceOf(routeTargetType), layouts);
            }

            return getFlowErrorComponent(location);
        }

        private List<RouterLayout> getRouterLayouts(
                NavigationState navigationState,
                Class<? extends Component> routeTargetType) {
            List<Class<? extends RouterLayout>> routeLayouts = this.getRouter()
                    .getRegistry()
                    .getRouteLayouts(navigationState.getResolvedPath(),
                            routeTargetType);
            List<RouterLayout> layouts = new ArrayList<>();
            for (Class<? extends RouterLayout> routeLayout : routeLayouts) {
                layouts.add(getInstanceOf(routeLayout));
            }
            return layouts;
        }

        private HasElement getFlowErrorComponent(Location location) {
            HasElement errorComponent = createErrorComponentInstance();
            if (errorComponent instanceof HasErrorParameter) {
                // Create a dummy event to set error message
                BeforeEnterEvent beforeEnterEvent = new BeforeEnterEvent(
                        this.getRouter(), NavigationTrigger.PROGRAMMATIC,
                        location, errorComponent.getClass(), this,
                        Collections.emptyList());
                String message = String.format("Route not found: '%s'",
                        location.getPath());
                ((HasErrorParameter) errorComponent).setErrorParameter(
                        beforeEnterEvent,
                        new ErrorParameter<>(Exception.class,
                                new NotFoundException(message)));
            }
            return errorComponent;
        }

        private HasElement createErrorComponentInstance() {
            Optional<NavigationState> errorNavigationState = this.getRouter()
                    .resolveRouteNotFoundNavigationTarget();
            if (!errorNavigationState.isPresent()) {
                // Default built-in RouteNotFoundError component
                return new RouteNotFoundError();
            } else {
                Class<? extends Component> errorNavigationTarget = errorNavigationState
                        .get().getNavigationTarget();
                return getInstanceOf(errorNavigationTarget);
            }
        }

        private <T extends HasElement> T getInstanceOf(
                Class<T> routeTargetType) {
            Optional<HasElement> currentInstance = this.getInternals()
                    .getActiveRouterTargetsChain().stream()
                    .filter(component -> component.getClass()
                            .equals(routeTargetType))
                    .findAny();
            return (T) currentInstance.orElseGet(
                    () -> Instantiator.get(this).getOrCreate(routeTargetType));
        }

        @Override
        public Optional<ThemeDefinition> getThemeFor(Class<?> navigationTarget,
                String path) {
            return Optional.empty();
        }

        @Override
        public void navigate(String location) {
            throw new UnsupportedOperationException(NO_NAVIGATION);
        }

        @Override
        public void navigate(Class<? extends Component> navigationTarget) {
            throw new UnsupportedOperationException(NO_NAVIGATION);
        }

        @Override
        public <T, C extends Component & HasUrlParameter<T>> void navigate(
                Class<? extends C> navigationTarget, T parameter) {
            throw new UnsupportedOperationException(NO_NAVIGATION);
        }

        @Override
        public void navigate(String location, QueryParameters queryParameters) {
            throw new UnsupportedOperationException(NO_NAVIGATION);
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

        // Do not listen to pushState and routerLink events.
        config.put(ApplicationConstants.APP_WC_MODE, true);

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
        // We don't need to initialize UI with Router in CCDM.
        // Showing view is handled by client-side.
    }

    @Override
    protected BootstrapContext createBootstrapContext(VaadinRequest request,
                                                      VaadinResponse response, UI ui, Function<VaadinRequest, String> callback) {
        return new JavaScriptBootstrapContext(request, response, ui, callback);
    }


    @Override
    public boolean synchronizedHandleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response) throws IOException {
        Class<? extends UI> uiClass = getUIClass(request);

        BootstrapContext context = createAndInitUI(uiClass, request, response,
                session);

        ServletHelper.setResponseNoCacheHeaders(response::setHeader,
                response::setDateHeader);

        JsonObject json = Json.createObject();

        DeploymentConfiguration config = context.getSession()
                .getConfiguration();

        if (!config.isProductionMode()) {
            json.put("stats", getStats());
        }
        json.put("errors", getErrors());

        if (context.getPushMode().isEnabled()) {
            json.put("pushScript", getPushScript(context));
        }

        JsonObject initialUIDL = getInitialUidl(context.getUI());
        json.put("appConfig", getAppConfig(initialUIDL, context));

        writeResponse(response, json);
        return true;
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

    private String getPushScript(BootstrapContext context) {
        VaadinRequest request = context.getRequest();
        // Parameter appended to JS to bypass caches after version upgrade.
        String versionQueryParam = "?v=" + Version.getFullVersion();
        // Load client-side dependencies for push support
        String pushJSPath = context.getRequest().getService()
                .getContextRootRelativePath(request);

        if (request.getService().getDeploymentConfiguration()
                .isProductionMode()) {
            pushJSPath += ApplicationConstants.VAADIN_PUSH_JS;
        } else {
            pushJSPath += ApplicationConstants.VAADIN_PUSH_DEBUG_JS;
        }

        pushJSPath += versionQueryParam;
        return pushJSPath;
    }

    private JsonObject getAppConfig(JsonValue initialUIDL,
            BootstrapContext context) {

        boolean productionMode = context.getSession().getConfiguration()
                .isProductionMode();

        JsonObject appConfig = context.getApplicationParameters();

        appConfig.put("productionMode", Json.create(productionMode));
        appConfig.put("appId", context.getAppId());
        appConfig.put("uidl", initialUIDL);

        return appConfig;
    }
    private void writeResponse(VaadinResponse response, JsonObject json) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpURLConnection.HTTP_OK);
        response.getOutputStream().write(JsonUtil.stringify(json).getBytes("UTF-8"));
    }
}
