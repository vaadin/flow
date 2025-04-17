/*
 * Copyright 2000-2025 Vaadin Ltd.
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
