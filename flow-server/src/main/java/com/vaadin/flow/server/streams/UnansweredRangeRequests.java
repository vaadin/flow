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
package com.vaadin.flow.server.streams;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Warns in development mode when a built-in {@link DownloadHandler} cannot
 * answer a byte range request that it receives, once per handler class, cause
 * and application. Media players need ranges to seek, and Safari to play video
 * at all, but nothing else tells the developer.
 */
final class UnansweredRangeRequests implements Serializable {

    /**
     * Why a handler serves the whole file instead of the requested range, and
     * what the developer can do about it.
     */
    enum Cause {
        NOT_ENABLED("does not answer byte range requests",
                "Serve a file with DownloadHandler.forFile(...), or call enableRangeRequests() on a class resource, servlet resource or input stream handler."),
        UNKNOWN_LENGTH(
                "cannot answer byte range requests because the length of the content is unknown",
                "Give the content length in the DownloadResponse, or serve a file with DownloadHandler.forFile(...)."),
        NOT_HTTP(
                "cannot answer byte range requests because the response is not an HTTP servlet response",
                "Ranges are only answered through a servlet.");

        private final String problem;
        private final String remedy;

        Cause(String problem, String remedy) {
            this.problem = problem;
            this.remedy = remedy;
        }
    }

    /**
     * The names of the handler classes that have been warned about, each
     * followed by the cause. Names rather than classes, so that the classes of
     * an application that is reloaded in development mode are not kept in
     * memory.
     */
    private final Set<String> warnedHandlersAndCauses = ConcurrentHashMap
            .newKeySet();

    private UnansweredRangeRequests() {
    }

    /**
     * Logs a warning that the given handler serves the whole file instead of
     * the requested byte range if the request has a <code>Range</code> header,
     * unless a handler of the same class has already been warned about for the
     * same cause in this application or the application runs in production
     * mode.
     *
     * @param handler
     *            the handler that is about to serve the whole file
     * @param event
     *            the download event of the request
     * @param cause
     *            why the handler cannot answer the range
     */
    static void warnIfRangeRequested(DownloadHandler handler,
            DownloadEvent event, Cause cause) {
        // Handlers that applications test with a mocked event, which has no
        // request or session, must not fail because of the warning
        VaadinRequest request = event.getRequest();
        VaadinSession session = event.getSession();
        VaadinService service = session == null ? null : session.getService();
        if (request == null || request.getHeader("Range") == null
                || service == null
                || service.getDeploymentConfiguration().isProductionMode()) {
            return;
        }
        UnansweredRangeRequests warnings = service.getContext().getAttribute(
                UnansweredRangeRequests.class, UnansweredRangeRequests::new);
        String handlerType = handler.getClass().getName();
        if (warnings.warnedHandlersAndCauses.add(handlerType + " " + cause)) {
            String urlPostfix = handler.getUrlPostfix();
            String served = urlPostfix == null ? ""
                    : (" serving '" + urlPostfix + "'");
            LoggerFactory.getLogger(UnansweredRangeRequests.class).warn(
                    "The download handler {}{} {}, so it serves the whole file instead of the requested range. Media players need ranges to seek, and Safari to play video at all; download managers use them to resume. This is logged once per handler class and cause, so other files served the same way are affected too. {}",
                    handlerType, served, cause.problem, cause.remedy);
        }
    }
}
