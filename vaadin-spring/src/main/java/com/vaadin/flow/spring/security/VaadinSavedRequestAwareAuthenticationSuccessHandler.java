/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.ViewAccessChecker;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

/**
 * A version of {@link SavedRequestAwareAuthenticationSuccessHandler} that
 * writes a different return value for a Fusion TypeScript client.
 * <p>
 * This class acts as a {@link SavedRequestAwareAuthenticationSuccessHandler}
 * unless the request from the client contains a "source: typescript" header.
 * <p>
 * If the header is present, it sends a return value that is an "ok" instead of
 * a "redirect" response. This is so that the TypeScript caller is able to read
 * the returned values. Additionally it sends the saved URL separately so the
 * client can decide where to redirect if no URL was saved.
 */
public class VaadinSavedRequestAwareAuthenticationSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    /**
     * If this header is present with a value of "typescript" in login requests,
     * this success handler is activated. Other requests are passed through to
     * the parent class.
     */
    private static final String SOURCE_HEADER = "source";

    /** This header contains 'ok' if login was successful. */
    private static final String RESULT_HEADER = "Result";

    /** This header contains the Vaadin CSRF token. */
    private static final String VAADIN_CSRF_HEADER = "Vaadin-CSRF";

    /**
     * This header contains the URL defined as the default URL to redirect to
     * after login.
     */
    private static final String DEFAULT_URL_HEADER = "Default-url";

    /**
     * This header contains the last URL saved by Spring Security. If the user
     * navigates to /private and is redirected to /login, this header will
     * contain "/private" after the login succeeds.
     */
    private static final String SAVED_URL_HEADER = "Saved-url";

    /**
     * This header contains the name of the request header Spring uses for its
     * CSRF token
     */
    private static final String SPRING_CSRF_HEADER = "Spring-CSRF-header";
    /**
     * This header contains the current Spring CSRF token
     */
    private static final String SPRING_CSRF_TOKEN = "Spring-CSRF-token";

    /**
     * Redirect strategy used by
     * {@link VaadinSavedRequestAwareAuthenticationSuccessHandler}.
     */
    public static class RedirectStrategy extends DefaultRedirectStrategy {

        @Override
        public void sendRedirect(HttpServletRequest request,
                HttpServletResponse response, String url) throws IOException {
            String redirectUrl;
            String savedRedirectUrl = response.getHeader(SAVED_URL_HEADER);
            if (savedRedirectUrl != null) {
                redirectUrl = savedRedirectUrl;
            } else {
                redirectUrl = url;
            }

            if (!isTypescriptLogin(request)) {
                super.sendRedirect(request, response, redirectUrl);
                return;
            }

            response.setHeader(RESULT_HEADER, "success");
            Object springCsrfTokenObject = request
                    .getAttribute(CsrfToken.class.getName());
            if (springCsrfTokenObject instanceof CsrfToken) {
                CsrfToken springCsrfToken = (CsrfToken) springCsrfTokenObject;
                response.setHeader(SPRING_CSRF_HEADER,
                        springCsrfToken.getHeaderName());
                response.setHeader(SPRING_CSRF_TOKEN,
                        springCsrfToken.getToken());
            }
        }
    }

    /**
     * This needs to be stored only because the field in the super class is not
     * accessible.
     */
    private RequestCache requestCache = new HttpSessionRequestCache();

    /**
     * Creates a new instance.
     */
    public VaadinSavedRequestAwareAuthenticationSuccessHandler() {
        setRedirectStrategy(new RedirectStrategy());
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {
        SavedRequest savedRequest = this.requestCache.getRequest(request,
                response);
        String storedServerNavigation = getStoredServerNavigation(request);
        if (storedServerNavigation != null) {
            // The saved server navigation URL is relative to the context path
            if (!"".equals(request.getContextPath())) {
                storedServerNavigation = "/" + storedServerNavigation;
            }
            response.setHeader(SAVED_URL_HEADER, storedServerNavigation);
        } else if (savedRequest != null) {
            /*
             * This is here instead of in sendRedirect as we do not want to
             * fallback to the default URL but instead send that separately.
             */
            response.setHeader(SAVED_URL_HEADER, savedRequest.getRedirectUrl());
        }

        if (isTypescriptLogin(request)) {
            response.setHeader(DEFAULT_URL_HEADER,
                    determineTargetUrl(request, response));
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    /**
     * Gets the target URL potentially stored by the server side view access
     * control.
     *
     * @return a URL if the login dialog was triggered by the user trying to
     *         perform (server side) navigation to a protected server side view,
     *         {@code null} otherwise
     */
    private static String getStoredServerNavigation(
            HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (String) session
                .getAttribute(ViewAccessChecker.SESSION_STORED_REDIRECT);
    }

    static boolean isTypescriptLogin(HttpServletRequest request) {
        return "typescript".equals(request.getHeader(SOURCE_HEADER));
    }

    @Override
    public void setRequestCache(RequestCache requestCache) {
        super.setRequestCache(requestCache);
        this.requestCache = requestCache;
    }
}
