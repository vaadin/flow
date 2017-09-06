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

import com.vaadin.ui.Component;

/**
 * A simple route target renderer that always uses the given view type and route
 * configured for it.
 */
public class NEW_StaticRouteTargetRenderer extends NEW_RouteTargetRenderer {

    private final String route;
    private final Class<? extends Component> routeTargetType;

    /**
     * Creates a new route target renderer for the given component type and
     * route.
     *
     * @param viewType
     *            the component type to show, not <code>null</code>
     * @param route
     *            the route to use when evaluating path parameters for the
     *            component, or <code>null</code> if the route target should not
     *            get any path parameters
     */
    public NEW_StaticRouteTargetRenderer(
            Class<? extends Component> routeTargetType, String route) {
        if (routeTargetType == null) {
            throw new IllegalArgumentException(
                    "routeTargetType cannot be null");
        }

        this.route = route;
        this.routeTargetType = routeTargetType;
    }

    @Override
    public Class<? extends Component> getRouteTargetType(
            NavigationEvent event) {
        return routeTargetType;
    }

    @Override
    protected String getRoute() {
        return route;
    }

}
