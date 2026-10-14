/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.auth;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.BeforeEnterListener;

/**
 * Checks access to views using an {@link AccessAnnotationChecker}.
 * <p>
 * An instance of this class should be provided to a
 * {@link NavigationAccessControl} added as a {@link BeforeEnterListener} to the
 * {@link com.vaadin.flow.component.UI} of interest.
 */
public class AnnotatedViewAccessChecker implements NavigationAccessChecker {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AnnotatedViewAccessChecker.class);

    private final AccessAnnotationChecker accessAnnotationChecker;

    /**
     * Creates an instance using the given checker.
     */
    public AnnotatedViewAccessChecker() {
        this(new AccessAnnotationChecker());
    }

    /**
     * Creates an instance using the given checker.
     *
     * @param accessAnnotationChecker
     *            the checker to use
     */
    public AnnotatedViewAccessChecker(
            AccessAnnotationChecker accessAnnotationChecker) {
        this.accessAnnotationChecker = accessAnnotationChecker;
    }

    @Override
    public AccessCheckResult check(NavigationContext context) {
        Class<?> targetView = context.getNavigationTarget();
        boolean hasAccess = accessAnnotationChecker.hasAccess(targetView,
                context.getPrincipal(), context::hasRole);
        LOGGER.debug("Access to view '{}' with path '{}' is {}",
                context.getNavigationTarget().getName(),
                context.getLocation().getPath(),
                ((hasAccess) ? "allowed" : "denied"));
        if (hasAccess) {
            return context.allow();
        }
        String denyReason;
        if (isImplicitlyDenyAllAnnotated(targetView)) {
            denyReason = "Consider adding one of the following annotations "
                    + "to make the view accessible: @AnonymousAllowed, "
                    + "@PermitAll, @RolesAllowed.";
        } else {
            denyReason = "Access is denied by annotations on the view.";
        }
        return context.deny(denyReason);
    }

    private boolean isImplicitlyDenyAllAnnotated(Class<?> targetView) {
        return !(targetView.isAnnotationPresent(DenyAll.class)
                || targetView.isAnnotationPresent(PermitAll.class)
                || targetView.isAnnotationPresent(RolesAllowed.class));
    }

}
