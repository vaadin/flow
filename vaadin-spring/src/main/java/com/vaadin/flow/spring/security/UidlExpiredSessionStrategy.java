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
package com.vaadin.flow.spring.security;

import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;

import com.vaadin.flow.server.HandlerHelper;

/**
 * A strategy to handle expired sessions which is aware of UIDL requests.
 */
public class UidlExpiredSessionStrategy
        implements SessionInformationExpiredStrategy {

    private static final String UIDL_REFRESH_TOKEN = "Vaadin-Refresh";

    private final String destinationUrl;

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    /**
     * Creates a strategy that sends the browser to the context root.
     */
    public UidlExpiredSessionStrategy() {
        this("/");
    }

    /**
     * Creates a strategy that sends the browser to the given context-relative
     * URL.
     *
     * @param destinationUrl
     *            the context-relative URL to redirect to
     */
    public UidlExpiredSessionStrategy(String destinationUrl) {
        this.destinationUrl = destinationUrl;
    }

    /**
     * Sets the redirect strategy used for non-UIDL requests.
     *
     * @param redirectStrategy
     *            the redirect strategy to use
     */
    public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
    }

    @Override
    public void onExpiredSessionDetected(SessionInformationExpiredEvent event)
            throws IOException {
        var request = event.getRequest();
        var response = event.getResponse();
        var servletMapping = request.getHttpServletMapping().getPattern();
        if (HandlerHelper.isFrameworkInternalRequest(servletMapping, request)) {
            var refreshUrl = request.getContextPath() + destinationUrl;
            LoggerFactory.getLogger(UidlExpiredSessionStrategy.class).debug(
                    "Session expired during an internal request: writing a "
                            + "{} token pointing to {} into the response body.",
                    UIDL_REFRESH_TOKEN, refreshUrl);
            response.getWriter().write(UIDL_REFRESH_TOKEN + ": " + refreshUrl);
        } else {
            LoggerFactory.getLogger(UidlExpiredSessionStrategy.class).debug(
                    "Session expired: redirecting to {}.", destinationUrl);
            redirectStrategy.sendRedirect(request, response, destinationUrl);
        }
    }
}
