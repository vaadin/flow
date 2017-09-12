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
package com.vaadin.flow.router;

import com.vaadin.annotations.Route;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.UI;

/**
 * The router takes care of serving content when the user navigates within a
 * site or an application.
 * 
 * @author Vaadin Ltd.
 *
 * @see Route
 */
public class NewRouter implements RouterInterface {

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
    public NewRouter() {
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

            NavigationHandler handler = new StaticRouteTargetRenderer(
                    newState.getNavigationTarget());
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
}
