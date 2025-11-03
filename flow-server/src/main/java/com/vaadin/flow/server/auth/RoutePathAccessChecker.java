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
package com.vaadin.flow.server.auth;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks if a user has access to a given route path.
 * <p>
 * The check is performed by a pluggable {@link AccessPathChecker} on the actual
 * event location path, without considering if it is the route main path or an
 * alias. Therefore, the provided {@link AccessPathChecker} should be configured
 * to handle both route main paths and aliases.
 * <p>
 * An instance of this class should be provided to a
 * {@link NavigationAccessControl} added as a
 * {@link com.vaadin.flow.router.BeforeEnterListener} to the
 * {@link com.vaadin.flow.component.UI} of interest.
 *
 * @see AccessPathChecker
 * @see NavigationAccessControl
 */
public class RoutePathAccessChecker implements NavigationAccessChecker {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(RoutePathAccessChecker.class);
    private final AccessPathChecker accessPathChecker;

    /**
     * Creates an instance for the given checker.
     *
     * @param accessPathChecker
     *            the access path checker to use
     */
    public RoutePathAccessChecker(AccessPathChecker accessPathChecker) {
        this.accessPathChecker = Objects.requireNonNull(accessPathChecker,
                "access path checker is mandatory");
    }

    @Override
    public AccessCheckResult check(NavigationContext context) {
        if (context.isErrorHandling()) {
            // Error handling rerouting, the path would referer to the original
            // already evaluated route, so do no take decisions.
            return context.neutral();
        }
        String path = context.getLocation().getPath();
        boolean hasPathAccess = accessPathChecker.hasAccess(path,
                context.getPrincipal(), context::hasRole);
        LOGGER.debug("Access to view '{}' with path '{}' is {}",
                context.getNavigationTarget().getName(), path,
                ((hasPathAccess) ? "allowed" : "denied"));
        if (hasPathAccess) {
            return context.allow();
        }
        return context
                .deny("Access to '" + path + "' is denied by security rules.");
    }

}
