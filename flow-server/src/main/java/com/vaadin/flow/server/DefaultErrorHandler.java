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

package com.vaadin.flow.server;

import java.io.EOFException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.InvalidLocationException;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

/**
 * The default implementation of {@link ErrorHandler}.
 *
 * This implementation logs the exception at ERROR level, unless the exception
 * is in the ignore list.
 *
 * By default, the following exceptions are ignored to prevent logs to be
 * flooded by errors that are usually not raised by application logic, but are
 * caused by external event, such as broken connections or network issues.
 *
 * <ul>
 * <li>java.net.SocketException</li>
 * <li>java.net.SocketTimeoutException</li>
 * <li>java.io.EOFException</li>
 * <li>org.apache.catalina.connector.ClientAbortException</li>
 * <li>org.eclipse.jetty.io.EofException</li>
 * </ul>
 *
 * If the handler logger is set to DEBUG level, all exceptions are logged,
 * despite they are in the ignore list.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultErrorHandler implements ErrorHandler {

    private final Set<String> ignoredExceptions;

    protected DefaultErrorHandler(Set<String> ignoredExceptions) {
        this.ignoredExceptions = Set.copyOf(ignoredExceptions);
    }

    public DefaultErrorHandler() {
        this.ignoredExceptions = Set.of(SocketException.class.getName(),
                SocketTimeoutException.class.getName(),
                EOFException.class.getName(),
                "org.eclipse.jetty.io.EofException",
                "org.apache.catalina.connector.ClientAbortException");
    }

    @Override
    public void error(ErrorEvent event) {
        Throwable throwable = findRelevantThrowable(event.getThrowable());
        if (shouldHandle(throwable)) {
            if (FeatureFlags.get(VaadinService.getCurrent().getContext())
                    .isEnabled(FeatureFlags.HAS_ERROR_OUTSIDE_NAVIGATION)
                    && throwable instanceof Exception) {
                if (drawErrorViewForException((Exception) throwable)) {
                    return;
                }
            }
            Marker marker = MarkerFactory.getMarker("INVALID_LOCATION");
            if (throwable instanceof InvalidLocationException) {
                if (getLogger().isWarnEnabled(marker)) {
                    getLogger().warn(marker, "", throwable);
                }
            } else {
                // print the error on console
                getLogger().error("", throwable);
            }
        }
    }

    protected static boolean drawErrorViewForException(Exception exception) {
        ApplicationRouteRegistry appRegistry = ApplicationRouteRegistry
                .getInstance(VaadinService.getCurrent().getContext());
        Optional<ErrorTargetEntry> errorNavigationTarget = appRegistry
                .getErrorNavigationTarget(exception);
        // Found error target and handled exception is the same as thrown
        // exception.
        if (errorNavigationTarget.isPresent() && errorNavigationTarget.get()
                .getHandledExceptionType().equals(exception.getClass())) {
            UI ui = UI.getCurrent();
            if (ui == null) {
                return false;
            }
            // Init error view and parents
            Component routeTarget = getRouteTarget(
                    errorNavigationTarget.get().getNavigationTarget(), ui);
            List<Class<? extends RouterLayout>> routeLayouts = RouteUtil
                    .getParentLayoutsForNonRouteTarget(routeTarget.getClass());
            List<RouterLayout> parentLayouts = routeLayouts.stream()
                    .map(route -> getRouteTarget(route, ui))
                    .collect(Collectors.toList());

            // Connect to ui
            ui.getInternals().showRouteTarget(
                    ui.getInternals().getActiveViewLocation(), routeTarget,
                    parentLayouts);

            // Build before enter event for setErrorParameter
            NavigationEvent navigationEvent = new NavigationEvent(
                    ui.getInternals().getRouter(),
                    ui.getInternals().getActiveViewLocation(), ui,
                    NavigationTrigger.PROGRAMMATIC);
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

    protected boolean shouldHandle(Throwable t) {
        return getLogger().isDebugEnabled()
                || !ignoredExceptions.contains(t.getClass().getName());
    }

    /**
     * Vaadin wraps exceptions in its own and due to reflection usage there
     * might be also other irrelevant exceptions that make no sense for Vaadin
     * users (~developers using Vaadin). This method tries to choose the
     * relevant one to be reported.
     *
     * @param t
     *            a throwable passed to ErrorHandler
     * @return the throwable that is relevant for Vaadin users
     */
    public static Throwable findRelevantThrowable(Throwable t) {
        return t;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DefaultErrorHandler.class.getName());
    }

}
