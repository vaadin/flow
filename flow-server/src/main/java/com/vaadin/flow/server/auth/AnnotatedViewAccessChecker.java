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

import java.util.Collections;
import java.util.List;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.RouteRegistry;

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
        if (RouteUtil.isAutolayoutEnabled(targetView,
                context.getLocation().getPath())) {
            RouteRegistry registry = context.getRouter().getRegistry();
            boolean noParents = registry.getRegisteredRoutes().stream()
                    .filter(routeData -> routeData.getNavigationTarget()
                            .equals(targetView))
                    .map(data -> data.getParentLayouts().isEmpty()).findFirst()
                    .orElse(true);
            if (noParents
                    && registry.hasLayout(context.getLocation().getPath())) {
                Class<?> layout = registry
                        .getLayout(context.getLocation().getPath());

                boolean hasAccess = accessAnnotationChecker.hasAccess(layout,
                        context.getPrincipal(), context::hasRole);
                if (!hasAccess) {
                    LOGGER.warn(
                            "Denied access to view due to layout '{}' access rules",
                            layout.getSimpleName());
                    return context.deny(
                            "Consider adding one of the following annotations "
                                    + "to make the layout accessible: @AnonymousAllowed, "
                                    + "@PermitAll, @RolesAllowed.");
                }
            }
        } else {
            RouteRegistry registry = context.getRouter().getRegistry();
            List<Class<? extends RouterLayout>> parents = registry
                    .getRegisteredRoutes().stream()
                    .filter(routeData -> routeData.getNavigationTarget()
                            .equals(targetView))
                    .map(RouteBaseData::getParentLayouts).findFirst()
                    .orElse(Collections.emptyList());
            if (!parents.isEmpty()) {
                for (Class<? extends RouterLayout> parent : parents) {
                    boolean hasAccess = accessAnnotationChecker.hasAccess(
                            parent, context.getPrincipal(), context::hasRole);
                    if (!hasAccess) {
                        LOGGER.warn(
                                "Denied access to view due to parent layout '{}' access rules",
                                parent.getSimpleName());
                        return context.deny(
                                "Consider adding one of the following annotations "
                                        + "to make the parent layouts accessible: @AnonymousAllowed, "
                                        + "@PermitAll, @RolesAllowed.");
                    }
                }
            }
        }

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
            if (targetView.isAnnotationPresent(Layout.class)
                    && context.getRouter().getRegistry()
                            .getTargetUrl(
                                    (Class<? extends Component>) targetView)
                            .isEmpty()) {
                LOGGER.debug(
                        "Denied access to view due to layout '{}' access rules",
                        targetView.getSimpleName());
                denyReason = "Consider adding one of the following annotations "
                        + "to make the layout accessible: @AnonymousAllowed, "
                        + "@PermitAll, @RolesAllowed.";
            }
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
