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
package com.vaadin.router;

import java.util.Optional;

import com.vaadin.flow.router.ImmutableRouterConfiguration;
import com.vaadin.router.event.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.startup.RouteRegistry;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

/**
 * The router takes care of serving content when the user navigates within a
 * site or an application.
 * 
 * @author Vaadin Ltd.
 *
 * @see Route
 */
public class Router implements RouterInterface {

    private RouteResolver routeResolver;

    private final RouterConfiguration configuration = new RouterConfiguration() {
        @Override
        public boolean isConfigured() {
            return true;
        }
    };

    /**
     * Constructs a new router with a {@link DefaultRouteResolver}.
     */
    public Router() {
        routeResolver = new DefaultRouteResolver();
    }

    @Override
    public void initializeUI(UI ui, VaadinRequest initRequest) {

        String pathInfo = initRequest.getPathInfo();

        final String path;
        if (pathInfo == null) {
            path = "";
        } else {
            assert pathInfo.startsWith("/");
            path = pathInfo.substring(1);
        }

        final QueryParameters queryParameters = QueryParameters
                .full(initRequest.getParameterMap());

        ui.getPage().getHistory().setHistoryStateChangeHandler(
                e -> navigate(ui, e.getLocation(), e.getTrigger()));

        Location location = new Location(path, queryParameters);
        int statusCode = navigate(ui, location, NavigationTrigger.PAGE_LOAD);

        VaadinResponse response = VaadinService.getCurrentResponse();
        if (response != null) {
            response.setStatus(statusCode);
        }
    }

    @Override
    public int navigate(UI ui, Location location, NavigationTrigger trigger) {
        assert ui != null;
        assert location != null;
        assert trigger != null;

        NavigationState newState = getRouteResolver()
                .resolve(new ResolveRequest(this, location));
        if (newState != null) {
            NavigationEvent navigationEvent = new NavigationEvent(this,
                    location, ui, trigger);

            NavigationHandler handler = new NavigationStateRenderer(newState);
            return handler.handle(navigationEvent);
        }

        Location slashToggledLocation = location.toggleTrailingSlash();
        NavigationState slashToggledState = getRouteResolver()
                .resolve(new ResolveRequest(this, slashToggledLocation));
        if (slashToggledState != null) {
            NavigationEvent navigationEvent = new NavigationEvent(this,
                    slashToggledLocation, ui, trigger);

            NavigationHandler handler = new InternalRedirectHandler(
                    slashToggledLocation);
            return handler.handle(navigationEvent);
        }

        return 404;
    }

    @Override
    public void reconfigure(RouterConfigurator configurator) {
        // NO-OP
    }

    @Override
    public ImmutableRouterConfiguration getConfiguration() {
        return configuration;
    }

    private RouteResolver getRouteResolver() {
        return routeResolver;
    }

    /**
     * Get the registered url string for given navigation target.
     * 
     * @param navigationTarget
     *            navigation target to get url for
     * @return url for the navigation target
     */
    public String getUrl(Class<? extends Component> navigationTarget) {
        Optional<String> targetUrl = RouteRegistry.getInstance()
                .getTargetUrl(navigationTarget);
        if (!targetUrl.isPresent()) {
            throw new IllegalArgumentException(
                    "No route found for given navigation target!");
        }
        return targetUrl.get();
    }

    /**
     * Get the url string for given navigation target with the parameter in the
     * url.
     * <p>
     * Note! Given parameter is checked for correct class type. This means that
     * if the navigation target defined parameter is of type Boolean then
     * calling getUrl with a String will fail.
     * 
     * @param navigationTarget
     *            navigation target to get url for
     * @param parameter
     *            parameter to embed into the generated url
     * @return url for the naviagtion target with parameter
     */
    public <T> String getUrl(
            Class<? extends HasUrlParameter<T>> navigationTarget, T parameter) {
        String routeString = getUrl(
                (Class<? extends Component>) navigationTarget).replace(
                        "{" + parameter.getClass().getSimpleName() + "}",
                        parameter.toString());

        Optional<Class<? extends Component>> registryTarget = RouteRegistry
                .getInstance().getNavigationTarget(routeString);

        if (registryTarget.isPresent()
                && !hasUrlParameters(registryTarget.get())
                && !registryTarget.get().equals(navigationTarget)) {
            throw new IllegalArgumentException(String.format(
                    "Url matches existing navigation target '%s' with higher priority.",
                    registryTarget.get().getName()));
        }
        return routeString;
    }

    private boolean hasUrlParameters(
            Class<? extends Component> navigationTarget) {
        return HasUrlParameter.class.isAssignableFrom(navigationTarget);
    }
}
