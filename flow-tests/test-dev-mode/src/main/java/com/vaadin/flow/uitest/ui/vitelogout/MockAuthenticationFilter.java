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
package com.vaadin.flow.uitest.ui.vitelogout;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.Principal;

/**
 * Mock authentication filter for testing Vite logout redirect behavior.
 * <p>
 * Intercepts login POST requests, sets an authenticated session attribute, and
 * wraps subsequent requests with a principal when authenticated.
 */
@WebFilter(urlPatterns = { "/view/*" })
public class MockAuthenticationFilter implements Filter {

    public static final String AUTHENTICATED_ATTR = "mock.authenticated";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession();
        String path = httpRequest.getRequestURI();

        // On POST to login route, mark as authenticated and redirect
        if (path.endsWith(".LoginView")
                && "POST".equals(httpRequest.getMethod())) {
            session.setAttribute(AUTHENTICATED_ATTR, true);
            httpResponse.sendRedirect(
                    "/view/com.vaadin.flow.uitest.ui.vitelogout.LogoutTestView");
            return;
        }

        // If authenticated, wrap request with principal
        if (Boolean.TRUE.equals(session.getAttribute(AUTHENTICATED_ATTR))) {
            chain.doFilter(new AuthenticatedRequestWrapper(httpRequest),
                    response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private static class AuthenticatedRequestWrapper
            extends HttpServletRequestWrapper {

        AuthenticatedRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public Principal getUserPrincipal() {
            return () -> "testuser";
        }
    }
}
