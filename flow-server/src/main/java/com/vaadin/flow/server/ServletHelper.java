/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.ApplicationConstants;

import java.io.Serializable;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Contains helper methods for {@link VaadinServlet} and generally for handling
 * {@link VaadinRequest VaadinRequests}.
 *
 * @since 1.0
 */
public class ServletHelper implements Serializable {

    /**
     * The default SystemMessages (read-only).
     */
    static final SystemMessages DEFAULT_SYSTEM_MESSAGES = new SystemMessages();

    /**
     * Framework internal enum for tracking the type of a request.
     */
    public enum RequestType {

        /**
         * UIDL requests.
         */
        UIDL(ApplicationConstants.REQUEST_TYPE_UIDL),
        /**
         * Heartbeat requests.
         */
        HEARTBEAT(ApplicationConstants.REQUEST_TYPE_HEARTBEAT),
        /**
         * Push requests (any transport).
         */
        PUSH(ApplicationConstants.REQUEST_TYPE_PUSH);

        private String identifier;

        private RequestType(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Returns the identifier for the request type.
         *
         * @return the identifier
         */
        public String getIdentifier() {
            return identifier;
        }
    }

    private ServletHelper() {
        // Only utility methods
    }

    /**
     * Returns whether the given request is of the given type.
     *
     * @param request
     *            the request to check
     * @param requestType
     *            the type to check for
     * @return <code>true</code> if the request is of the given type,
     *         <code>false</code> otherwise
     */
    public static boolean isRequestType(VaadinRequest request,
            RequestType requestType) {
        return requestType.getIdentifier().equals(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER));
    }

    /**
     * Helper to find the most most suitable Locale. These potential sources are
     * checked in order until a Locale is found:
     * <ol>
     * <li>The passed component (or UI) if not null</li>
     * <li>{@link UI#getCurrent()} if defined</li>
     * <li>The passed session if not null</li>
     * <li>{@link VaadinSession#getCurrent()} if defined</li>
     * <li>The passed request if not null</li>
     * <li>{@link VaadinService#getCurrentRequest()} if defined</li>
     * <li>{@link Locale#getDefault()}</li>
     * </ol>
     *
     * @param session
     *            the session that is searched for locale or <code>null</code>
     *            if not available
     * @param request
     *            the request that is searched for locale or <code>null</code>
     *            if not available
     * @return the found locale
     */
    public static Locale findLocale(VaadinSession session,
            VaadinRequest request) {

        if (session == null) {
            session = VaadinSession.getCurrent();
        }
        if (session != null) {
            Locale locale = session.getLocale();
            if (locale != null) {
                return locale;
            }
        }

        if (request == null) {
            request = VaadinService.getCurrentRequest();
        }
        if (request != null) {
            Locale locale = request.getLocale();
            if (locale != null) {
                return locale;
            }
        }

        return Locale.getDefault();
    }

    /**
     * Sets no cache headers to the specified response.
     *
     * @param headerSetter
     *            setter for string value headers
     * @param longHeaderSetter
     *            setter for long value headers
     */
    public static void setResponseNoCacheHeaders(
            BiConsumer<String, String> headerSetter,
            BiConsumer<String, Long> longHeaderSetter) {
        headerSetter.accept("Cache-Control", "no-cache, no-store");
        headerSetter.accept("Pragma", "no-cache");
        longHeaderSetter.accept("Expires", 0L);
    }

    /**
     * Gets a relative path that cancels the provided path. This essentially
     * adds one .. for each part of the path to cancel.
     *
     * @param pathToCancel
     *            the path that should be canceled
     * @return a relative path that cancels out the provided path segment
     */
    public static String getCancelingRelativePath(String pathToCancel) {
        StringBuilder sb = new StringBuilder(".");
        // Start from i = 1 to ignore first slash
        for (int i = 1; i < pathToCancel.length(); i++) {
            if (pathToCancel.charAt(i) == '/') {
                sb.append("/..");
            }
        }
        return sb.toString();
    }

    /**
     * Gets a relative path you can use to refer to the context root.
     *
     * @param request
     *            the request for which the location should be determined
     * @return A relative path to the context root. Never ends with a slash (/).
     */
    public static String getContextRootRelativePath(VaadinServletRequest request) {
        // Generate location from the request by finding how many "../" should
        // be added to the servlet path before we get to the context root

        // Should not take pathinfo into account because the base URI refers to
        // the servlet path

        String servletPath = request.getServletPath();
        assert servletPath != null;
        if (!servletPath.endsWith("/")) {
            servletPath += "/";
        }
        return ServletHelper.getCancelingRelativePath(servletPath);
    }

}
