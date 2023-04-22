/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;

/**
 * The authentication context of the application.
 * <p>
 *
 * It allows to access authenticated user information and to initiate the logout
 * process.
 *
 * An instance of this class is available for injection as bean in view and
 * layout classes. The class is not {@link java.io.Serializable}, so potential
 * referencing fields in Vaadin views should be defined {@literal transient}.
 *
 * @author Vaadin Ltd
 * @since 23.3
 */
public class AuthenticationContext {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AuthenticationContext.class);

    private LogoutSuccessHandler logoutSuccessHandler;

    private CompositeLogoutHandler logoutHandler;

    /**
     * Gets an {@link Optional} with an instance of the current user if it has
     * been authenticated, or empty if the user is not authenticated.
     *
     * Anonymous users are considered not authenticated.
     *
     * @param <U>
     *            the type parameter of the expected user instance
     * @param userType
     *            the type of the expected user instance
     * @return an {@link Optional} with the current authenticated user, or empty
     *         if none available
     * @throws ClassCastException
     *             if the current user instance does not match the given
     *             {@code userType}.
     */
    public <U> Optional<U> getAuthenticatedUser(Class<U> userType) {
        return getAuthentication().map(Authentication::getPrincipal)
                .map(userType::cast);
    }

    /**
     * Gets an {@link Optional} containing the authenticated principal name, or
     * an empty optional if the user is not authenticated.
     *
     * The principal name usually refers to a username or an identifier that can
     * be used to retrieve additional information for the authenticated user.
     *
     * Anonymous users are considered not authenticated.
     *
     * @return an {@link Optional} containing the authenticated principal name
     *         or an empty optional if not available.
     */
    public Optional<String> getPrincipalName() {
        return getAuthentication().map(Principal::getName);
    }

    /**
     * Indicates whether a user is currently authenticated.
     *
     * Anonymous users are considered not authenticated.
     *
     * @return {@literal true} if a user is currently authenticated, otherwise
     *         {@literal false}
     */
    public boolean isAuthenticated() {
        return getAuthentication().map(Authentication::isAuthenticated)
                .orElse(false);
    }

    /**
     * Initiates the logout process of the current authenticated user by
     * invalidating the local session and then notifying
     * {@link org.springframework.security.web.authentication.logout.LogoutHandler}.
     */
    public void logout() {
        HttpServletRequest request = VaadinServletRequest.getCurrent()
                .getHttpServletRequest();
        HttpServletResponse response = VaadinServletResponse.getCurrent()
                .getHttpServletResponse();
        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();

        final UI ui = UI.getCurrent();
        logoutHandler.logout(request, response, auth);
        ui.accessSynchronously(() -> {
            try {
                logoutSuccessHandler.onLogoutSuccess(request, response, auth);
            } catch (IOException | ServletException e) {
                // Raise a warning log message about the failure.
                LOGGER.warn(
                        "There was an error notifying the logout handler about the user logout",
                        e);
            }
        });
    }

    /**
     * Sets component to handle logout process.
     *
     * @param logoutSuccessHandler
     *            {@link LogoutSuccessHandler} instance, not {@literal null}.
     * @param logoutHandlers
     *            {@link LogoutHandler}s list, not {@literal null}.
     */
    void setLogoutHandlers(LogoutSuccessHandler logoutSuccessHandler,
            List<LogoutHandler> logoutHandlers) {
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.logoutHandler = new CompositeLogoutHandler(logoutHandlers);
    }

    private static Optional<Authentication> getAuthentication() {
        return Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(auth -> !(auth instanceof AnonymousAuthenticationToken));
    }

    /* For testing purposes */
    LogoutSuccessHandler getLogoutSuccessHandler() {
        return logoutSuccessHandler;
    }

    /* For testing purposes */
    CompositeLogoutHandler getLogoutHandler() {
        return logoutHandler;
    }

}
