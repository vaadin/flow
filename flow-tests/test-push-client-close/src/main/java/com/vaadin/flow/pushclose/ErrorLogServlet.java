/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.pushclose;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

/**
 * Lists the errors the server has logged, so that the test can check that
 * closing push connections did not log any.
 * <p>
 * The test runs in a separate process from the server, so this is how it gets
 * to see the server log.
 */
public class ErrorLogServlet extends HttpServlet {

    /**
     * The path the errors are listed at.
     */
    public static final String PATH = "/error-log";

    private static final List<String> ERRORS = new CopyOnWriteArrayList<>();

    /**
     * Starts recording the errors logged by any logger.
     */
    static void startRecording() {
        LoggerContext context = (LoggerContext) LoggerFactory
                .getILoggerFactory();
        AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
            @Override
            protected void append(ILoggingEvent event) {
                if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
                    ERRORS.add(describe(event));
                }
            }
        };
        appender.setContext(context);
        appender.start();
        context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
    }

    private static String describe(ILoggingEvent event) {
        StringBuilder description = new StringBuilder(event.getLoggerName())
                .append(": ").append(event.getFormattedMessage());
        for (IThrowableProxy cause = event
                .getThrowableProxy(); cause != null; cause = cause.getCause()) {
            description.append(" caused by ").append(cause.getClassName())
                    .append(": ").append(cause.getMessage());
        }
        return description.toString();
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.join("\n", ERRORS));
    }
}
