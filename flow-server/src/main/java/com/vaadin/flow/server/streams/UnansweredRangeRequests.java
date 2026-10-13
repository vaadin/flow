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

import jakarta.servlet.http.HttpServletResponse;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Warns in development mode when a {@link DownloadHandler} answers a byte range
 * request with the whole content, once per kind of handler and application.
 * Media players need ranges to seek, and Safari to play video at all, but
 * nothing else tells the developer.
 */
final class UnansweredRangeRequests implements Serializable {

    /**
     * The kinds of handlers that have been warned about: the handler class, or
     * for a lambda the class that declares it. Bounded by the classes of the
     * application, and names rather than classes, so that the classes of an
     * application that is reloaded in development mode are not kept in memory.
     */
    private final Set<String> warnedHandlerTypes = ConcurrentHashMap
            .newKeySet();

    private UnansweredRangeRequests() {
    }

    /**
     * Logs a warning if the given handler answered a byte range request with
     * the whole content, unless a handler of the same kind has already been
     * warned about in this application or the application runs in production
     * mode.
     *
     * @param handler
     *            the handler that has handled a request with a
     *            <code>Range</code> header
     * @param response
     *            the response that the handler has written
     * @param session
     *            the session that the request belongs to
     */
    static void warnIfWholeContentSent(DownloadHandler handler,
            VaadinResponse response, VaadinSession session) {
        // A handler that failed, or answered the range itself, has not sent
        // the whole content
        if (!(response instanceof HttpServletResponse httpResponse)
                || httpResponse.getStatus() != HttpStatusCode.OK.getCode()) {
            return;
        }
        VaadinService service = session.getService();
        if (service.getDeploymentConfiguration().isProductionMode()) {
            return;
        }
        UnansweredRangeRequests warnings = service.getContext().getAttribute(
                UnansweredRangeRequests.class, UnansweredRangeRequests::new);
        String className = handler.getClass().getName();
        // The class of a lambda is named after the class that declares it,
        // followed by a generated suffix
        int lambdaSuffix = className.indexOf("$$Lambda");
        boolean lambda = lambdaSuffix > 0;
        String handlerType = lambda ? className.substring(0, lambdaSuffix)
                : className;
        if (warnings.warnedHandlerTypes.add(handlerType)) {
            String urlPostfix = handler.getUrlPostfix();
            String description = (lambda
                    ? "A download handler declared as a lambda in "
                    : "The download handler ") + handlerType
                    + (urlPostfix == null ? ""
                            : " serving '" + urlPostfix + "'");
            LoggerFactory.getLogger(UnansweredRangeRequests.class).warn(
                    "{} answered a byte range request with the whole content. Media players need ranges to seek, and Safari to play video at all; download managers use them to resume. This is logged once per kind of handler, so other files served the same way are affected too. Serve a file with DownloadHandler.forFile(...), call enableRangeRequests() on a class resource, servlet resource or input stream handler, or answer ranges in a handler class of your own and return true from isRangeRequestsEnabled().",
                    description);
        }
    }
}
