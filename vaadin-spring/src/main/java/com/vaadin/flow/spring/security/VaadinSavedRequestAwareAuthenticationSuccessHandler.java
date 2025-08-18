/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.vaadin.flow.internal.springcsrf.SpringCsrfToken;
import com.vaadin.flow.internal.springcsrf.SpringCsrfTokenUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

import org.springframework.core.log.LogMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.StringUtils;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.auth.NavigationAccessControl;

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

            if (!isTypescriptLogin(request)) {
                super.sendRedirect(request, response, url);
                return;
            }

            response.setHeader(RESULT_HEADER, "success");
        }
    }

    /**
     * This needs to be stored only because the field in the super class is not
     * accessible.
     */
    private RequestCache requestCache = new HttpSessionRequestCache();

    private CsrfTokenRepository csrfTokenRepository;

    /**
     * Creates a new instance.
     */
    public VaadinSavedRequestAwareAuthenticationSuccessHandler() {
        setRedirectStrategy(new RedirectStrategy());
        setTargetUrlParameter(SAVED_URL_HEADER);
    }

    /**
     * Called when a user has been successfully authenticated and finds out
     * whether it should redirect the user back to a default success url or the
     * originally requested url before the authentication.
     * <p>
     * As the user might have initiated the request to a restricted resource in
     * different ways, this method is responsible for extracting the final
     * target for redirection of the user and to set it on the response header,
     * so that it can be used by the redirection strategy in a unified way. See
     * {@link RedirectStrategy} and
     * {@link VaadinSavedRequestAwareAuthenticationSuccessHandler#determineTargetUrl(HttpServletRequest, HttpServletResponse)}
     * <p>
     * If the redirection to the login page for authentication is initiated by
     * spring security (such as entering some URI manually into the address bar
     * and not navigating via Vaadin application), then a SavedRequest object
     * containing the originally requested path is pushed to the request cache
     * by the Spring Security so the redirect target url would be extracted from
     * that.
     * <p>
     * Contrarily, navigating via Vaadin application router (e.g. via menus or
     * the links within the application) will result in requests being sent to
     * "/" or "/{app-context-root}", so the Spring Security will not intercept
     * and the SavedRequest will be null. In this case, the target redirect url
     * can be extracted from the session. See
     * {@link NavigationAccessControl#beforeEnter(BeforeEnterEvent)}
     *
     * @param request
     *            the request which caused the successful authentication
     * @param response
     *            the response
     * @param authentication
     *            the <code>Authentication</code> object which was created
     *            during the authentication process.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {

        if (isTypescriptLogin(request)) {
            response.setHeader(DEFAULT_URL_HEADER,
                    determineTargetUrl(request, response));
            if (this.csrfTokenRepository != null) {
                CsrfToken csrfToken = this.csrfTokenRepository
                        .generateToken(request);
                this.csrfTokenRepository.saveToken(csrfToken, request,
                        response);
                response.setHeader(SPRING_CSRF_HEADER,
                        csrfToken.getHeaderName());
                response.setHeader(SPRING_CSRF_TOKEN, csrfToken.getToken());
            } else {
                Optional<SpringCsrfToken> springCsrfToken = SpringCsrfTokenUtil
                        .getSpringCsrfToken(request);
                springCsrfToken.ifPresent(csrfToken -> {
                    response.setHeader(SPRING_CSRF_HEADER,
                            csrfToken.getHeaderName());
                    response.setHeader(SPRING_CSRF_TOKEN, csrfToken.getToken());
                });
            }

        } else {
            if (this.csrfTokenRepository != null) {
                // Remove CSRF token to allow generating a new one upon next
                // request
                this.csrfTokenRepository.saveToken(null, request, response);
            }
        }

        SavedRequest savedRequest = this.requestCache.getRequest(request,
                response);
        String fullySavedRequestUrl = getStoredServerNavigation(request);

        if (savedRequest != null) {
            String targetUrlParameter = this.getTargetUrlParameter();
            if (!this.isAlwaysUseDefaultTargetUrl()
                    && (targetUrlParameter == null || !StringUtils.hasText(
                            request.getParameter(targetUrlParameter)))) {
                this.clearAuthenticationAttributes(request);
                String targetUrl = savedRequest.getRedirectUrl();
                response.setHeader(SAVED_URL_HEADER, targetUrl);
                this.getRedirectStrategy().sendRedirect(request, response,
                        targetUrl);
                return;
            } else {
                this.requestCache.removeRequest(request, response);
            }
        } else if (fullySavedRequestUrl != null) {
            response.setHeader(SAVED_URL_HEADER, fullySavedRequestUrl);
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    /**
     * Determines the originally requested path by the user before
     * authentication by reading the target redirect url from the response
     * header.
     * <p>
     * Note that if a defaultSuccessUrl has been configured on the http security
     * configurer, or the value of {@code targetUrlParameter} is {@code null},
     * it will fall back to the default super class implementation.
     *
     * @param request
     *            the http servlet request instance
     * @param response
     *            the http servlet response instance
     * @return the original requested path by the user before authentication.
     */
    @Override
    protected String determineTargetUrl(HttpServletRequest request,
            HttpServletResponse response) {
        if (!isAlwaysUseDefaultTargetUrl()
                && this.getTargetUrlParameter() != null) {
            String targetUrl = response.getHeader(this.getTargetUrlParameter());
            if (StringUtils.hasText(targetUrl)) {
                if (this.logger.isTraceEnabled()) {
                    this.logger.trace(LogMessage.format(
                            "Using url %s from response header %s", targetUrl,
                            this.getTargetUrlParameter()));
                }
                return targetUrl;
            }
        }
        return super.determineTargetUrl(request, response);
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
        String redirectUrl = (String) session.getAttribute(
                NavigationAccessControl.SESSION_STORED_REDIRECT_ABSOLUTE);
        session.removeAttribute(
                NavigationAccessControl.SESSION_STORED_REDIRECT_ABSOLUTE);
        return redirectUrl;
    }

    static boolean isTypescriptLogin(HttpServletRequest request) {
        return "typescript".equals(request.getHeader(SOURCE_HEADER));
    }

    @Override
    public void setRequestCache(RequestCache requestCache) {
        super.setRequestCache(requestCache);
        this.requestCache = requestCache;
    }

    /**
     * Sets the csrf token repository which is used to generate the csrf token
     * when using a cookie based (stateless) csrf store.
     *
     * @param csrfTokenRepository
     *            the csrf token repository
     */
    public void setCsrfTokenRepository(
            CsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;

    }
}