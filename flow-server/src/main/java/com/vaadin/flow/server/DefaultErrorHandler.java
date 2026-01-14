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

import java.io.EOFException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.vaadin.flow.router.InvalidLocationException;

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

    public static final Set<String> SOCKET_EXCEPTIONS = Collections
            .unmodifiableSet(Set.of(SocketException.class.getName(),
                    SocketTimeoutException.class.getName(),
                    EOFException.class.getName(),
                    "org.eclipse.jetty.io.EofException",
                    "org.apache.catalina.connector.ClientAbortException"));

    private final Set<String> ignoredExceptions;
    private final Set<String> routeConfigurationExceptions;

    protected DefaultErrorHandler(Set<String> ignoredExceptions) {
        this.ignoredExceptions = Set.copyOf(ignoredExceptions);
        this.routeConfigurationExceptions = new HashSet<>();
    }

    protected DefaultErrorHandler(Set<String> ignoredExceptions,
            Set<String> routeConfigurationExceptions) {
        this.ignoredExceptions = Set.copyOf(ignoredExceptions);
        this.routeConfigurationExceptions = Set
                .copyOf(routeConfigurationExceptions);
    }

    public DefaultErrorHandler() {
        this.ignoredExceptions = SOCKET_EXCEPTIONS;
        this.routeConfigurationExceptions = Set.of(
                AmbiguousRouteConfigurationException.class.getName(),
                InvalidRouteConfigurationException.class.getName(),
                InvalidRouteLayoutConfigurationException.class.getName());
    }

    @Override
    public void error(ErrorEvent event) {
        Throwable throwable = findRelevantThrowable(event.getThrowable());
        if (shouldHandle(throwable)) {
            if (ErrorHandlerUtil
                    .handleErrorByRedirectingToErrorView(throwable)) {
                return;
            }
            Marker marker = MarkerFactory.getMarker("INVALID_LOCATION");
            if (throwable instanceof InvalidLocationException) {
                if (getLogger().isWarnEnabled(marker)) {
                    getLogger().warn(marker, "Invalid location: {}",
                            throwable.getMessage(), throwable);
                }
            } else {
                if (routeConfigurationExceptions
                        .contains(throwable.getClass().getName())) {
                    getLogger().error("Route configuration error found:");
                    getLogger().error(throwable.getMessage());
                } else {
                    // print the error on console
                    getLogger().error("Unexpected error: {}",
                            throwable.getMessage(), throwable);
                }
            }
        }
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
