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
package com.vaadin.flow.internal.hilla;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

import org.jspecify.annotations.NullMarked;

/**
 * A container for utility methods related with Hilla file-based router.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 24.4
 */
@NullMarked
public interface FileRouterRequestUtil {

    /**
     * Checks if the request corresponds to a Hilla route and, if so, applies
     * the corresponding access control.
     *
     * @param request
     *            the HTTP request to check
     * @return {@code true} if the request is allowed, {@code false} otherwise
     * @deprecated use {@link #isAnonymousRoute(HttpServletRequest)} to match
     *             requests to anonymous routes
     */
    @Deprecated(since = "25.0", forRemoval = true)
    boolean isRouteAllowed(HttpServletRequest request);

    /**
     * Checks if the given request goes to a route that allows anonymous access.
     *
     * @param request
     *            the HTTP request to check
     * @return {@code true} if the request is targeting a route that allows
     *         anonymous access, {@code false} otherwise
     */
    boolean isAnonymousRoute(HttpServletRequest request);

    /**
     * Checks if the given request goes to a route that requires authentication.
     *
     * @param request
     *            the HTTP request to check
     * @return {@code true} if the request is targeting a route that requires
     *         authentication, {@code false} otherwise
     */
    boolean isSecuredRoute(HttpServletRequest request);

    /**
     * Gets the set of authorities that are allowed to access the given request.
     *
     * @param request
     *            the HTTP request to check
     * @return the set of authorities that are allowed to access the given
     *         request, or an empty set if no authority is required
     */
    Set<String> getAllowedAuthorities(HttpServletRequest request);
}
