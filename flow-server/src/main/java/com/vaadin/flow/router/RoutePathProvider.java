/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

/**
 * Allows to implement a custom navigation target path generation logic for
 * components annotated with {@code @Route(Route.NAMING_CONVENTION)}.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
public interface RoutePathProvider {

    /**
     * Produces a path for the {@code navigationTarget} component class.
     *
     * @param navigationTarget
     *            a navigation target class
     * @return a route path for the navigation target, may be {@code null} if
     *         the provided class is not a navigation target
     */
    String getRoutePath(Class<?> navigationTarget);

}
