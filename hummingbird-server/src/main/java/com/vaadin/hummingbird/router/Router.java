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
package com.vaadin.hummingbird.router;

import com.vaadin.server.VaadinRequest;

/**
 * The router takes care of serving content when the user navigates within a
 * site or application.
 *
 * @since
 * @author Vaadin Ltd
 */
public class Router {
    private Resolver resolver;

    /**
     * Enables navigation for a new UI instance. This initializes the UI content
     * based on the location used for loading the UI and sets up the UI to be
     * updated when the user navigates to some other location.
     *
     * @param ui
     *            the router UI that navigation should be set up for
     * @param initRequest
     *            the Vaadin request that loads the UI
     */
    public void initializeUI(RouterUI ui, VaadinRequest initRequest) {
        String pathInfo = initRequest.getPathInfo();

        String path;
        if (pathInfo == null) {
            path = "";
        } else {
            assert pathInfo.startsWith("/");
            path = pathInfo.substring(1);
        }

        ui.getPage().getHistory().setPopStateHandler(e -> {
            String newLocation = e.getLocation();

            // Should be enabled when the event handler has been updated to give
            // relative urls (separate PR)

            // navigate(ui, new Location(newLocation));
            throw new RuntimeException(
                    "Needs relative location support instead of "
                            + newLocation);
        });

        Location location = new Location(path);
        navigate(ui, location);
    }

    /**
     * Navigates the given URL to the given location.
     *
     * @param ui
     *            the router UI to update
     * @param location
     *            the location to navigate to
     */
    public void navigate(RouterUI ui, Location location) {
        if (resolver == null) {
            throw new IllegalStateException(
                    "Resolver has not yet been initialized");
        }

        NavigationEvent navigationEvent = new NavigationEvent(this, location,
                ui);

        Content content = resolver.resolve(navigationEvent);

        if (content == null) {
            content = new ErrorContent(404);
        }

        content.show(navigationEvent);
    }

    /**
     * Sets the resolver to use for locating content.
     *
     * @param resolver
     *            the resolver
     */
    public void setResolver(Resolver resolver) {
        this.resolver = resolver;
    }
}
