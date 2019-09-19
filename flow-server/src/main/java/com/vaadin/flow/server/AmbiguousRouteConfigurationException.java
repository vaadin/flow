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
package com.vaadin.flow.server;

import com.vaadin.flow.component.Component;

/**
 * Exception indicating that the application's routes already has the navigation
 * target with the given path.
 *
 * @author Vaadin Ltd
 * @since 1.4
 */
public class AmbiguousRouteConfigurationException
        extends InvalidRouteConfigurationException {

    private final Class<? extends Component> navigationTarget;

    /**
     * Constructs a new invalid route configuration exception with the specified
     * detail message and the existing navigation target component which already
     * presents in the configuration with the route path.
     *
     * @param message
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     * @param navigationTarget
     *            the configured navigation target, not {@code null}
     */
    public AmbiguousRouteConfigurationException(String message,
            Class<? extends Component> navigationTarget) {
        super(message);
        this.navigationTarget = navigationTarget;
    }

    /**
     * Returns the already configured navigation target component class which
     * caused the exception.
     * <p>
     * In case the exception happens as a result of a navigation target
     * collision for the same route path this method returns the configured
     * navigation target for the path.
     *
     * @return an optional existing navigation target in the configuration which
     *         caused the exception, or an empty optional if the exception is
     *         not caused by a collision, not {@code null}
     */
    public Class<? extends Component> getConfiguredNavigationTarget() {
        return navigationTarget;
    }

}
