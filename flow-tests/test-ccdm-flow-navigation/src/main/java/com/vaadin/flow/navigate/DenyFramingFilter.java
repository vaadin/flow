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
package com.vaadin.flow.navigate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;

/**
 * Denies framing of the offline stub, the way Spring Security and other
 * security filters do for every response by default.
 * <p>
 * The Flow client renders the offline stub within an iframe, so the service
 * worker is expected to serve it as frameable. Which of the two headers that
 * deny framing is sent depends on the {@value #DENY_FRAMING_HEADER_COOKIE}
 * cookie, so that a test can cover both. See
 * {@code ServiceWorkerIT.offlineStub_framingDeniedByServer_offlineStubShown}
 * and
 * {@code ServiceWorkerIT.offlineStub_framingDeniedByPolicy_offlineStubShown}.
 */
@WebFilter(urlPatterns = "/offline-stub.html")
public class DenyFramingFilter extends HttpFilter {

    /**
     * Name of the cookie that selects the header denying framing.
     */
    public static final String DENY_FRAMING_HEADER_COOKIE = "denyFramingHeader";

    /**
     * Value of {@value #DENY_FRAMING_HEADER_COOKIE} that denies framing with a
     * content security policy instead of {@code X-Frame-Options}.
     */
    public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";

    /**
     * Policy denying framing altogether, sent when the
     * {@value #DENY_FRAMING_HEADER_COOKIE} cookie asks for it.
     * <p>
     * The directive name is spelled in mixed case on purpose: directive names
     * are case-insensitive, so the service worker has to recognize the
     * directive however the deployment happens to spell it.
     */
    public static final String DENY_FRAMING_POLICY = "Frame-Ancestors 'none'";

    /**
     * Header denying framing by default, the way a security filter that knows
     * nothing about content security policies does.
     */
    public static final String FRAME_OPTIONS = "X-Frame-Options";

    /**
     * Value of {@value #FRAME_OPTIONS} denying framing altogether.
     */
    public static final String DENY = "DENY";

    @Override
    protected void doFilter(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (deniesFramingWithPolicy(request)) {
            response.setHeader(CONTENT_SECURITY_POLICY, DENY_FRAMING_POLICY);
        } else {
            response.setHeader(FRAME_OPTIONS, DENY);
        }
        chain.doFilter(request, response);
    }

    private boolean deniesFramingWithPolicy(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        return cookies != null && Arrays.stream(cookies).anyMatch(
                cookie -> DENY_FRAMING_HEADER_COOKIE.equals(cookie.getName())
                        && CONTENT_SECURITY_POLICY.equals(cookie.getValue()));
    }
}
