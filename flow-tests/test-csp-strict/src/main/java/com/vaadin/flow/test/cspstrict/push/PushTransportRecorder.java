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
package com.vaadin.flow.test.cspstrict.push;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.springframework.stereotype.Component;

import com.vaadin.flow.server.VaadinSession;

/**
 * Records, per HTTP session, the transport of every push request the browser
 * makes, as the push client names it in the request. With it a view can show
 * which transports the browser really used: a browser that cannot open a
 * WebSocket falls back to long polling without any error, and its long polling
 * requests are recorded here.
 */
@Component
public class PushTransportRecorder implements Filter {

    private static final String TRANSPORT_PARAMETER = "X-Atmosphere-Transport";

    private static final String ATTRIBUTE = PushTransportRecorder.class
            .getName();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        String transport = request.getParameter(TRANSPORT_PARAMETER);
        HttpSession session = ((HttpServletRequest) request).getSession(false);
        if (transport != null && session != null) {
            synchronized (session) {
                if (session.getAttribute(ATTRIBUTE) == null) {
                    session.setAttribute(ATTRIBUTE,
                            new ConcurrentSkipListSet<String>());
                }
            }
            getTransports(session.getAttribute(ATTRIBUTE)).add(transport);
        }
        chain.doFilter(request, response);
    }

    /**
     * Gets the transports of the push requests made in the given session.
     *
     * @param session
     *            the session, not {@code null}
     * @return the transports in alphabetical order, joined with a comma; empty
     *         if the session has made no push request
     */
    static String getTransports(VaadinSession session) {
        Object transports = session.getSession().getAttribute(ATTRIBUTE);
        return transports == null ? ""
                : String.join(",", getTransports(transports));
    }

    /**
     * Forgets the push requests made so far in the given session.
     *
     * @param session
     *            the session, not {@code null}
     */
    static void clear(VaadinSession session) {
        Object transports = session.getSession().getAttribute(ATTRIBUTE);
        if (transports != null) {
            getTransports(transports).clear();
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getTransports(Object attribute) {
        return (Set<String>) attribute;
    }
}
