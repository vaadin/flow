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
package com.vaadin.flow.server;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

/**
 * Utility class for use with ErrorHandler to show HasErrorParameter view when
 * an exception happens during a RPC call outside of navigation.
 */
public final class ErrorHandlerUtil {

    /**
     * Check throwable is Exception and redraw error view if matching error
     * handler target exists.
     *
     * @param throwable
     *            throwable to find handler for
     * @return {@code true} if error handled, {@code false} if no error handler
     */
    public static boolean handleErrorByRedirectingToErrorView(
            Throwable throwable) {
        if (throwable instanceof Exception) {
            return handleErrorByRedirectingToErrorView((Exception) throwable);
        }
        return false;
    }

    /**
     * Check throwable is Exception and redraw error view if matching error
     * handler target exists.
     *
     * @param throwable
     *            throwable to find handler for
     * @param context
     *            current {@code VaadinContex} instance
     * @param ui
     *            current UI instance
     * @return
     */
    public static boolean handleErrorByRedirectingToErrorView(
            Throwable throwable, VaadinContext context, UI ui) {
        if (throwable instanceof Exception) {
            return handleErrorByRedirectingToErrorView((Exception) throwable,
                    context, ui);
        }
        return false;
    }

    /**
     * Check if matching error handler target exists for exception and redraw
     * view using ErrorTarget.
     *
     * @param exception
     *            exception to find handler for
     * @return {@code true} if error handled, {@code false} if no error handler
     */
    public static boolean handleErrorByRedirectingToErrorView(
            Exception exception) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return false;
        }
        VaadinSession session = ui.getSession();
        VaadinService current = session != null ? session.getService() : null;
        VaadinContext context = current != null ? current.getContext() : null;
        if (context == null) {
            return false;
        }
        return handleErrorByRedirectingToErrorView(exception, context, ui);
    }

    /**
     * Check if matching error handler target exists for exception and redraw
     * view using ErrorTarget.
     *
     * @param exception
     *            exception to find handler for
     * @param context
     *            current {@code VaadinContex} instance
     * @param ui
     *            current UI instance
     * @return
     */
    public static boolean handleErrorByRedirectingToErrorView(
            Exception exception, VaadinContext context, UI ui) {
        ApplicationRouteRegistry appRegistry = ApplicationRouteRegistry
                .getInstance(context);
        Optional<ErrorTargetEntry> errorNavigationTarget = appRegistry
                .getErrorNavigationTarget(exception);
        // Found error target and handled exception is the same as thrown
        // exception. else do not handle
        if (errorNavigationTarget.isPresent() && errorNavigationTarget.get()
                .getHandledExceptionType().equals(exception.getClass())) {
            // Init error view and parents
            Component routeTarget = getRouteTarget(
                    errorNavigationTarget.get().getNavigationTarget(), ui);
            List<Class<? extends RouterLayout>> routeLayouts = RouteUtil
                    .getParentLayoutsForNonRouteTarget(routeTarget.getClass());
            List<RouterLayout> parentLayouts = routeLayouts.stream()
                    .map(route -> getRouteTarget(route, ui))
                    .collect(Collectors.toList());

            // Connect to ui
            UIInternals internals = ui.getInternals();
            internals.showRouteTarget(internals.getActiveViewLocation(),
                    routeTarget, parentLayouts);

            // Build before enter event for setErrorParameter
            NavigationEvent navigationEvent = new NavigationEvent(
                    internals.getRouter(), internals.getActiveViewLocation(),
                    ui, NavigationTrigger.PROGRAMMATIC);
            BeforeEnterEvent beforeEnterEvent = new BeforeEnterEvent(
                    navigationEvent,
                    errorNavigationTarget.get().getNavigationTarget(),
                    routeLayouts);

            // Execute error view error parameter
            ((HasErrorParameter) routeTarget).setErrorParameter(
                    beforeEnterEvent,
                    new ErrorParameter(exception.getClass(), exception));
            return true;
        }
        return false;
    }

    private static <T extends HasElement> T getRouteTarget(
            Class<T> routeTargetType, UI ui) {
        Optional<HasElement> currentInstance = ui.getInternals()
                .getActiveRouterTargetsChain().stream()
                .filter(component -> component.getClass()
                        .equals(routeTargetType))
                .findAny();
        return (T) currentInstance.orElseGet(() -> Instantiator.get(ui)
                .createRouteTarget(routeTargetType, null));
    }
}
