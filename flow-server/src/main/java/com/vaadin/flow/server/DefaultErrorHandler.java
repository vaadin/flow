/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.vaadin.flow.router.InvalidLocationException;

/**
 * The default implementation of {@link ErrorHandler}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultErrorHandler implements ErrorHandler {
    @Override
    public void error(ErrorEvent event) {
        Throwable throwable = findRelevantThrowable(event.getThrowable());

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
