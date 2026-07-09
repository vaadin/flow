/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
