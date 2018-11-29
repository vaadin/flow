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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.RouteRegistry;

/**
 * AbstractRouteRegistry with locking support and configuration.
 */
public abstract class AbstractRouteRegistry implements RouteRegistry {

    /**
     * Configuration interface to use for updating the configuration entity
     */
    @FunctionalInterface
    public interface Configuration extends Serializable {
        /**
         * Configure the given RouteConfiguration.
         *
         * @param configuration
         *         mutable routeConfiguration to make changes to
         */
        void configure(RouteConfiguration configuration);
    }

    /**
     * Lock used to ensure there's only one update going on at once.
     * <p>
     * The lock is configured to always guarantee a fair ordering.
     */
    private final ReentrantLock configurationLock = new ReentrantLock(true);

    // The live configuration for this route registry
    protected volatile RouteConfiguration routeConfiguration = new RouteConfiguration();

    /**
     * Thread-safe update of the RouteConfiguration.
     *
     * @param command
     *         command that will mutate the configuration copy.
     */
    protected void configure(Configuration command) {
        configurationLock.lock();
        try {
            RouteConfiguration mutableCopy = getRouteConfiguration(
                    routeConfiguration, true);

            command.configure(mutableCopy);

            routeConfiguration = getRouteConfiguration(mutableCopy, false);
        } finally {
            configurationLock.unlock();
        }
    }

    protected RouteConfiguration getRouteConfiguration(RouteConfiguration original, boolean mutable) {
        return new RouteConfiguration(original, mutable);
    }

    @Override
    public List<Class<? extends RouterLayout>> getRouteLayouts(
            Class<? extends Component> navigationTarget, String path) {
        return RouteUtil.getParentLayouts(navigationTarget, path);
    }

    @Override
    public List<Class<? extends RouterLayout>> getNonRouteLayouts(
            Class<? extends Component> errorTarget) {
        return RouteUtil.getParentLayoutsForNonRouteTarget(errorTarget);
    }
}
