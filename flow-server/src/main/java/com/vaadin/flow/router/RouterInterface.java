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

import java.io.Serializable;

import com.vaadin.flow.router.legacy.ImmutableRouterConfiguration;
import com.vaadin.flow.router.legacy.RouterConfigurator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.startup.RouteRegistry;
import com.vaadin.ui.UI;

/**
 * Interface that defines the contract that different Router implementations
 * need to fulfill.
 */
public interface RouterInterface extends Serializable {

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
    void initializeUI(UI ui, VaadinRequest initRequest);

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
    int navigate(UI ui, Location location, NavigationTrigger trigger);

    /**
     * Updates the configuration of this router in a thread-safe way.
     *
     * @param configurator
     *            the configurator that will update the configuration
     */
    void reconfigure(RouterConfigurator configurator);

    /**
     * Gets the active router configuration. The returned instance cannot be
     * directly modified. Use {@link #reconfigure(RouterConfigurator)} to update
     * the configuration.
     *
     * @return the currently used router configuration
     */
    ImmutableRouterConfiguration getConfiguration();

    /**
     * Gets the route registry used by this router.
     *
     * @return the route registry used by this router
     */
    RouteRegistry getRegistry();
}
