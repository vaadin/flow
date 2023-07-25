/*
 * Copyright 2000-2023 Vaadin Ltd.
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
        this.accessAnnotationChecker = new AccessAnnotationChecker();
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
    public Result check(NavigationContext context) {
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
