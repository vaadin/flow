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
 * Warns in development mode when a built-in {@link DownloadHandler} that does
 * not answer byte range requests receives one, once per handler class and
 * application. Media players need ranges to seek, and Safari to play video at
 * all, but nothing else tells the developer.
 */
final class UnansweredRangeRequests implements Serializable {

    /**
     * The names of the handler classes that have been warned about. Names
     * rather than classes, so that the classes of an application that is
     * reloaded in development mode are not kept in memory.
     */
    private final Set<String> warnedHandlerTypes = ConcurrentHashMap
            .newKeySet();

    private UnansweredRangeRequests() {
    }

    /**
     * Logs a warning that the given handler serves the whole file instead of
     * the requested byte range if the request has a <code>Range</code> header,
     * unless a handler of the same class has already been warned about in this
     * application or the application runs in production mode.
     *
     * @param handler
     *            the handler that is about to serve the whole file
     * @param event
     *            the download event of the request
     */
    static void warnIfRangeRequested(DownloadHandler handler,
            DownloadEvent event) {
        // An event that a test mocks may have no request or session
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
        if (warnings.warnedHandlerTypes.add(handlerType)) {
            String urlPostfix = handler.getUrlPostfix();
            LoggerFactory.getLogger(UnansweredRangeRequests.class).warn(
                    "{} does not answer byte range requests, so it serves the whole file instead of the requested range. Media players need ranges to seek, and Safari to play video at all; download managers use them to resume. This is logged once per handler class, so other files served the same way are affected too. Serve a file with DownloadHandler.forFile(...), or call enableRangeRequests() on a class resource, servlet resource or input stream handler.",
                    "The download handler " + handlerType
                            + (urlPostfix == null ? ""
                                    : " serving '" + urlPostfix + "'"));
        }
    }
}
