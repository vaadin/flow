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
package com.vaadin.flow.router.legacy;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouterInterface;
import com.vaadin.flow.router.internal.InternalRedirectHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.RouteRegistry;
import com.vaadin.ui.UI;

/**
 * The router takes care of serving content when the user navigates within a
 * site or an application.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class Router implements RouterInterface {
    /**
     * The live configuration instance. All changes to the configuration are
     * done on a copy, which is then swapped into use so that nobody outside
     * this class ever can have a reference to the actively used instance.
     */
    private volatile RouterConfiguration configuration = new RouterConfiguration() {
        @Override
        public boolean isConfigured() {
            // Regular implementation always returns true
            return false;
        }
    };

    /**
     * Lock used to ensure there's only one update going on at once.
     * <p>
     * The lock is configured to always guarantee a fair ordering.
     */
    private final ReentrantLock configUpdateLock = new ReentrantLock(true);

    /**
     * Creates a new router.
     */
    public Router() {
        // Nothing to here
    }

    /**
     * Enables navigation for a new UI instance. This initializes the UI content
     * based on the location used for loading the UI and sets up the UI to be
     * updated when the user navigates to some other location.
     *
     * @param ui
     *            the UI that navigation should be set up for
     * @param initRequest
     *            the Vaadin request that bootstraps the provided UI
     */
    @Override
    public void initializeUI(UI ui, VaadinRequest initRequest) {
        assert getConfiguration().isConfigured();

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

    /**
     * Navigates the given UI to the given location.
     *
     * @param ui
     *            the UI to update, not <code>null</code>
     * @param location
     *            the location to navigate to, not <code>null</code>
     * @param trigger
     *            the type of user action that triggered this navigation, not
     *            <code>null</code>
     * @return the HTTP status code resulting from the navigation
     */
    @Override
    public int navigate(UI ui, Location location, NavigationTrigger trigger) {
        assert ui != null;
        assert location != null;
        assert trigger != null;

        // Read volatile field only once per navigation
        ImmutableRouterConfiguration currentConfig = configuration;
        assert currentConfig.isConfigured();

        NavigationEvent navigationEvent = new NavigationEvent(this, location,
                ui, trigger);

        Optional<NavigationHandler> handler = currentConfig.getResolver()
                .resolve(navigationEvent);

        if (!handler.isPresent()) {
            handler = currentConfig.resolveRoute(location);
        }

        // Redirect foo/bar <-> foo/bar/ if there is no mapping for the given
        // location but there is a mapping for the other
        if (!handler.isPresent() && !location.getPath().isEmpty()) {
            Location toggledLocation = location.toggleTrailingSlash();
            Optional<NavigationHandler> toggledHandler = currentConfig
                    .resolveRoute(toggledLocation);
            if (toggledHandler.isPresent()) {
                handler = Optional
                        .of(new InternalRedirectHandler(toggledLocation));
            }
        }

        if (!handler.isPresent()) {
            NavigationHandler errorHandler = currentConfig.getErrorHandler();
            handler = Optional.of(errorHandler);
        }

        return handler.get().handle(navigationEvent);
    }

    /**
     * Updates the configuration of this router in a thread-safe way.
     *
     * @param configurator
     *            the configurator that will update the configuration
     */
    @Override
    public void reconfigure(RouterConfigurator configurator) {
        /*
         * This is expected to be run so rarely (during service init and OSGi
         * style dynamic reconfiguration) that blocking and copying values is
         * not a problem.
         */
        configUpdateLock.lock();
        try {
            /*
             * Create a copy that the configurator can modify without affecting
             * the live instance.
             */
            RouterConfiguration mutableCopy = new RouterConfiguration(
                    configuration, true);

            configurator.configure(mutableCopy);

            /*
             * Create an use a new immutable copy that can be shared between
             * concurrent request threads without risking any races.
             */
            configuration = new RouterConfiguration(mutableCopy, false);
        } finally {
            configUpdateLock.unlock();
        }
    }

    /**
     * Gets the active router configuration. The returned instance cannot be
     * directly modified. Use {@link #reconfigure(RouterConfigurator)} to update
     * the configuration.
     *
     * @return the currently used router configuration
     */
    @Override
    public ImmutableRouterConfiguration getConfiguration() {
        ImmutableRouterConfiguration currentConfig = configuration;

        assert !currentConfig.isModifiable();
        return currentConfig;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>This implementation always throws
     * {@link UnsupportedOperationException}.</b>
     */
    @Override
    public RouteRegistry getRegistry() {
        /*
         * Throwing of this exception is not mentioned in the inherited JavaDocs
         * of this method since this implementation is expected to be removed or
         * deprecated after the other implementation is completed.
         */
        throw new UnsupportedOperationException(
                "This router implementation doesn't use a route registry");
    }
}
