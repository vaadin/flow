/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
 * @since 24.3
 */
public class RoutePathAccessChecker implements NavigationAccessChecker {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(RoutePathAccessChecker.class);
    private final AccessPathChecker accessPathChecker;

    /**
     * Creates an instance for the given checker.
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
