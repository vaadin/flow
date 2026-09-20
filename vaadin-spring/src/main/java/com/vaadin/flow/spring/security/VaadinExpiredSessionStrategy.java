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

import jakarta.servlet.ServletException;

import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;

/**
 * A strategy to handle expired sessions which is aware of Vaadin requests.
 * <p>
 * When Spring Security concurrency control detects an expired session, it has
 * already logged the user out and invalidated the HTTP session. Instead of
 * writing a response the Vaadin client cannot make sense of, this strategy lets
 * the request continue through the filter chain, so that it is answered like
 * any other request that arrives without a session: Vaadin answers a UIDL
 * request with a session expired message, a heartbeat with 403 and a push
 * request through its own handler, while a request for a view ends in the login
 * view of the application.
 */
public class VaadinExpiredSessionStrategy
        implements SessionInformationExpiredStrategy {

    private static final RedirectStrategy REDIRECT_STRATEGY = new DefaultRedirectStrategy();

    /**
     * Creates the strategy.
     */
    public VaadinExpiredSessionStrategy() {
    }

    @Override
    public void onExpiredSessionDetected(SessionInformationExpiredEvent event)
            throws IOException, ServletException {
        var request = event.getRequest();
        var response = event.getResponse();
        var filterChain = event.getFilterChain();
        if (filterChain == null) {
            // The event may be created without a filter chain, in which case
            // the request cannot continue and the browser is sent to the
            // application root instead.
            LoggerFactory.getLogger(VaadinExpiredSessionStrategy.class).debug(
                    "Session expired, but the event carries no filter chain: "
                            + "redirecting to the application root.");
            REDIRECT_STRATEGY.sendRedirect(request, response, "/");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
