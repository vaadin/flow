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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;

/**
 * Warns in development mode about a byte range request that a
 * {@link DownloadHandler} does not answer, once per handler class and
 * application. A media player cannot seek in a file served without ranges and
 * Safari does not play it, but nothing else tells the developer.
 */
final class UnansweredRangeRequests {

    private final Set<Class<?>> warnedHandlerTypes = ConcurrentHashMap
            .newKeySet();

    private UnansweredRangeRequests() {
    }

    /**
     * Logs a warning about the given handler not answering the byte range that
     * the request asks for, unless a handler of the same class has already been
     * warned about in this application or the application runs in production
     * mode.
     *
     * @param handler
     *            the handler that received the request
     * @param request
     *            the request with a <code>Range</code> header
     */
    static void warnOnce(DownloadHandler handler, VaadinRequest request) {
        VaadinService service = request.getService();
        if (service.getDeploymentConfiguration().isProductionMode()) {
            return;
        }
        UnansweredRangeRequests warnings = service.getContext().getAttribute(
                UnansweredRangeRequests.class, UnansweredRangeRequests::new);
        if (warnings.warnedHandlerTypes.add(handler.getClass())) {
            LoggerFactory.getLogger(UnansweredRangeRequests.class).warn(
                    "{} does not answer byte range requests, so it sent the whole content for one. A media player cannot seek in such a file, Safari does not play it, and a download cannot resume. Serve a file with DownloadHandler.forFile(...), call enableRangeRequests() on a class resource, servlet resource or input stream handler, or answer ranges in a handler class of your own and return true from isRangeRequestsEnabled().",
                    describe(handler));
        }
    }

    private static String describe(DownloadHandler handler) {
        String className = handler.getClass().getName();
        // The class of a lambda is named after the class that declares it,
        // followed by a generated suffix
        int lambdaSuffix = className.indexOf("$$Lambda");
        if (lambdaSuffix > 0) {
            return "A download handler declared as a lambda in %s"
                    .formatted(className.substring(0, lambdaSuffix));
        }
        String urlPostfix = handler.getUrlPostfix();
        if (urlPostfix != null) {
            return "The download handler %s of '%s'".formatted(className,
                    urlPostfix);
        }
        return "The download handler %s".formatted(className);
    }
}
