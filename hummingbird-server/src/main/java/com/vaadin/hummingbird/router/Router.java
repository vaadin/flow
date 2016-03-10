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

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.server.VaadinRequest;

/**
 * The router takes care of serving content when the user navigates within a
 * site or an application.
 *
 * @since
 * @author Vaadin Ltd
 */
public class Router implements Serializable {
    /**
     * The live configuration instance. All changes to the configuration are
     * done on a copy, which is then swapped into use so that nobody outside
     * this class ever can have a reference to the actively used instance.
     */
    private volatile RouterConfiguration configuration = new RouterConfiguration();

    /**
     * Lock used to ensure there's only one update going on at once.
     * <p>
     * The lock is configured to always guarantee a fair ordering.
     */
    private final ReentrantLock configurationUpdateLock = new ReentrantLock(
            true);

    /**
     * Enables navigation for a new UI instance. This initializes the UI content
     * based on the location used for loading the UI and sets up the UI to be
     * updated when the user navigates to some other location.
     *
     * @param ui
     *            the router UI that navigation should be set up for
     * @param initRequest
     *            the Vaadin request that bootstraps the provided UI
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

        ui.getPage().getHistory().setLocationChangeHandler(e -> {
            String newLocation = e.getLocation();

            navigate(ui, new Location(newLocation));
        });

        Location location = new Location(path);
        navigate(ui, location);
    }

    /**
     * Navigates the given UI to the given location.
     *
     * @param ui
     *            the router UI to update
     * @param location
     *            the location to navigate to
     */
    public void navigate(RouterUI ui, Location location) {
        NavigationEvent navigationEvent = new NavigationEvent(this, location,
                ui);

        NavigationHandler handler = configuration.getResolver()
                .resolve(navigationEvent);

        if (handler == null) {
            handler = new ErrorNavigationHandler(404);
        }

        handler.handle(navigationEvent);
    }

    /**
     * Updates the configuration of this router in a thread-safe way.
     *
     * @param configurator
     *            the configurator that will update the configuration
     */
    public void reconfigure(RouterConfigurator configurator) {
        /*
         * This is expected to be run so rarely (during service init and OSGi
         * style dynamic reconfiguration) that blocking and excessive copying is
         * not a problem.
         */
        configurationUpdateLock.lock();
        try {
            /*
             * Create a copy so that we don't pass the live reference to the
             * configurator.
             */
            RouterConfiguration copy = new RouterConfiguration(configuration);

            configurator.configure(copy);

            /*
             * Use a copy of the updated instance so that the configurator can't
             * accidentally leak a reference to the instance that will be live.
             *
             * This is a volatile write, which means that all updates made by
             * this thread will be fully flushed before the reference is
             * updated.
             *
             * Updating the field is only done in this critical section, so
             * there are never multiple updates going on at the same time.
             */
            configuration = new RouterConfiguration(copy);
        } finally {
            configurationUpdateLock.unlock();
        }
    }

    /**
     * Gets a copy of the currently used router configuration. This method
     * returns a copy of the configuration to prevent race conditions.
     *
     * @return a copy of the currently used router configuration
     */
    public RouterConfiguration getConfigurationCopy() {
        return new RouterConfiguration(configuration);
    }
}
