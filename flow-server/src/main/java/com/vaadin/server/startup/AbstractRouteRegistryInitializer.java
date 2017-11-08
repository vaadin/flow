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
package com.vaadin.server.startup;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.router.HasDynamicTitle;
import com.vaadin.router.PageTitle;
import com.vaadin.router.ParentLayout;
import com.vaadin.router.Route;
import com.vaadin.router.RouteAlias;
import com.vaadin.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.ui.Component;

/**
 * Common validation methods for route registry initializer.
 *
 * @author Vaadin Ltd
 *
 */
public abstract class AbstractRouteRegistryInitializer {

    /**
     * Validate the potential route classes stream and return them as a set.
     *
     * @param routeClasses
     *            potential route classes
     * @return a resulting set of the route component classes
     */
    @SuppressWarnings("unchecked")
    protected Set<Class<? extends Component>> validateRouteClasses(
            Stream<Class<?>> routeClasses) {
        return routeClasses.peek(this::checkForConflictingAnnotations)
                .filter(this::isApplicableClass)
                .map(clazz -> (Class<? extends Component>) clazz)
                .collect(Collectors.toSet());
    }

    private boolean isApplicableClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(Route.class)
                && Component.class.isAssignableFrom(clazz);
    }

    private void checkForConflictingAnnotations(Class<?> route) {
        if (route.isAnnotationPresent(RouteAlias.class)
                && !route.isAnnotationPresent(Route.class)) {
            throw new InvalidRouteLayoutConfigurationException(String.format(
                    "'%s'" + " declares '@%s' but doesn't declare '@%s'. "
                            + "The '%s' may not be used without '%s'",
                    route.getCanonicalName(), RouteAlias.class.getSimpleName(),
                    Route.class.getSimpleName(),
                    RouteAlias.class.getSimpleName(),
                    Route.class.getSimpleName()));

        }

        if (route.isAnnotationPresent(ParentLayout.class)) {
            throw new InvalidRouteLayoutConfigurationException(route
                    .getCanonicalName()
                    + " contains both @Route and @ParentLayout annotation. Only use @Route with Route.layout.");
        }
        if (route.isAnnotationPresent(PageTitle.class)
                && HasDynamicTitle.class.isAssignableFrom(route)) {
            throw new DuplicateNavigationTitleException(String.format(
                    "'%s' has a PageTitle annotation, but also implements HasDynamicTitle.",
                    route.getName()));
        }
    }
}
