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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.util.Assert;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.shared.ui.Transport;

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

    private VaadinRolePrefixHolder rolePrefixHolder;

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
        final UI ui = UI.getCurrentOrThrow();
        boolean pushWebsocketConnected = ui.getPushConfiguration()
                .getTransport() == Transport.WEBSOCKET
                && ui.getInternals().getPushConnection().isConnected();
        if (pushWebsocketConnected) {
            // WEBSOCKET transport mode would not log out properly after session
            // invalidation. Switching to WEBSOCKET_XHR for a single request
            // to do the logout.
            ui.getPushConfiguration().setTransport(Transport.WEBSOCKET_XHR);
            ui.getPage().executeJs("return true").then(ignored -> {
                LOGGER.debug(
                        "Switched to WEBSOCKET_XHR transport mode successfully for logout operation.");
                ui.getPushConfiguration().setTransport(Transport.WEBSOCKET);
                doLogout(ui);
            }, exception -> {
                LOGGER.debug(
                        "Failed to switch to WEBSOCKET_XHR transport mode for logout operation. "
                                + "Logout is performed anyway even if browser shows 'disconnected' message and browser console has WebSocket errors. "
                                + "Received exception: {}",
                        exception);
                ui.getPushConfiguration().setTransport(Transport.WEBSOCKET);
                doLogout(ui);
            });
        } else if (VaadinRequest.getCurrent() == null) {
            // Logout started from a background thread, force client to send
            // a request
            ui.getPage().executeJs("return true").then(ignored -> doLogout(ui),
                    error -> doLogout(ui));
        } else {
            doLogout(ui);
        }
    }

    private void doLogout(UI ui) {
        HttpServletRequest request = VaadinServletRequest.getCurrent()
                .getHttpServletRequest();
        HttpServletResponse response = Optional
                .ofNullable(VaadinServletResponse.getCurrent())
                .map(VaadinServletResponse::getHttpServletResponse)
                .orElse(null);
        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();

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
     * Gets the authorities granted to the current authenticated user.
     *
     * @return an unmodifiable collection of {@link GrantedAuthority}s or an
     *         empty collection if there is no authenticated user.
     */
    public Collection<? extends GrantedAuthority> getGrantedAuthorities() {
        return getAuthentication().filter(Authentication::isAuthenticated)
                .map(Authentication::getAuthorities)
                .orElse(Collections.emptyList());
    }

    private Stream<String> getGrantedAuthoritiesStream() {
        return getGrantedAuthorities().stream()
                .map(GrantedAuthority::getAuthority);
    }

    /**
     * Gets the roles granted to the current authenticated user.
     *
     * @return an unmodifiable collection of role names (without the role
     *         prefix) or an empty collection if there is no authenticated user.
     */
    public Collection<String> getGrantedRoles() {
        return getGrantedRolesStream().collect(Collectors.toSet());
    }

    private Stream<String> getGrantedRolesStream() {
        var rolePrefix = getRolePrefix();
        return getGrantedAuthoritiesStream()
                .filter(ga -> ga.startsWith(rolePrefix))
                .map(ga -> ga.substring(rolePrefix.length()));
    }

    /**
     * Checks whether the current authenticated user has the given role.
     * <p>
     *
     * The role must be provided without the role prefix, for example
     * {@code hasRole("USER")} instead of {@code hasRole("ROLE_USER")}.
     *
     * @param role
     *            the role to check, without the role prefix.
     * @return {@literal true} if the user holds the given role, otherwise
     *         {@literal false}.
     */
    public boolean hasRole(String role) {
        return getGrantedRolesStream().anyMatch(role::equals);
    }

    /**
     * Checks whether the current authenticated user has any of the given roles.
     * <p>
     *
     * Roles must be provided without the role prefix, for example
     * {@code hasAnyRole(Set.of("USER", "ADMIN"))} instead of
     * {@code hasAnyRole(Set.of("ROLE_USER", "ROLE_ADMIN"))}.
     *
     * @param roles
     *            a collection containing at least one role, without the role
     *            prefix.
     * @return {@literal true} if the user holds at least one of the given
     *         roles, otherwise {@literal false}.
     * @throws IllegalArgumentException
     *             if the given collection is empty.
     */
    public boolean hasAnyRole(Collection<String> roles) {
        if (roles.isEmpty()) {
            throw new IllegalArgumentException(
                    "Must provide at least one role to check");
        }
        return getGrantedRolesStream().anyMatch(roles::contains);
    }

    /**
     * Checks whether the current authenticated user has any of the given roles.
     * <p>
     *
     * Roles must be provided without the role prefix, for example
     * {@code hasAnyRole("USER", "ADMIN")} instead of
     * {@code hasAnyRole("ROLE_USER", "ROLE_ADMIN")}.
     *
     * @param roles
     *            an array containing at least one role, without the role
     *            prefix.
     * @return {@literal true} if the user holds at least one of the given
     *         roles, otherwise {@literal false}.
     * @throws IllegalArgumentException
     *             if the given array is empty.
     */
    public boolean hasAnyRole(String... roles) {
        return hasAnyRole(Set.of(roles));
    }

    /**
     * Checks whether the current authenticated user has all the given roles.
     * <p>
     *
     * Roles must be provided without the role prefix, for example
     * {@code hasAllRoles(Set.of("USER", "ADMIN"))} instead of
     * {@code hasAllRoles(Set.of("ROLE_USER", "ROLE_ADMIN"))}.
     *
     * @param roles
     *            a collection containing at least one role, without the role
     *            prefix.
     * @return {@literal true} if the user holds all the given roles, otherwise
     *         {@literal false}.
     * @throws IllegalArgumentException
     *             if the given collection is empty.
     */
    public boolean hasAllRoles(Collection<String> roles) {
        if (roles.isEmpty()) {
            throw new IllegalArgumentException(
                    "Must provide at least one role to check");
        }
        return getGrantedRolesStream().collect(Collectors.toSet())
                .containsAll(roles);
    }

    /**
     * Checks whether the current authenticated user has all the given roles.
     * <p>
     *
     * Roles must be provided without the role prefix, for example
     * {@code hasAllRoles("USER", "ADMIN")} instead of
     * {@code hasAllRoles("ROLE_USER", "ROLE_ADMIN")}.
     *
     * @param roles
     *            an array containing at least one role, without the role
     *            prefix.
     * @return {@literal true} if the user holds all the given roles, otherwise
     *         {@literal false}.
     * @throws IllegalArgumentException
     *             if the given array is empty.
     */
    public boolean hasAllRoles(String... roles) {
        return hasAllRoles(Set.of(roles));
    }

    /**
     * Checks whether the current authenticated user has the given authority.
     *
     * @param authority
     *            the authority to check.
     * @return {@literal true} if the user holds the given authority, otherwise
     *         {@literal false}.
     */
    public boolean hasAuthority(String authority) {
        return getGrantedAuthoritiesStream().anyMatch(authority::equals);
    }

    /**
     * Checks whether the current authenticated user has any of the given
     * authorities.
     *
     * @param authorities
     *            a collection containing at least one authority.
     * @return {@literal true} if the user holds at least one of the given
     *         authorities, otherwise {@literal false}.
     * @throws IllegalArgumentException
     *             if the given collection is empty.
     */
    public boolean hasAnyAuthority(Collection<String> authorities) {
        if (authorities.isEmpty()) {
            throw new IllegalArgumentException(
                    "Must provide at least one authority to check");
        }
        return getGrantedAuthoritiesStream().anyMatch(authorities::contains);
    }

    /**
     * Checks whether the current authenticated user has any of the given
     * authorities.
     *
     * @param authorities
     *            an array containing at least one authority.
     * @return {@literal true} if the user holds at least one of the given
     *         authorities, otherwise {@literal false}.
     * @throws IllegalArgumentException
     *             if the given array is empty.
     */
    public boolean hasAnyAuthority(String... authorities) {
        return hasAnyAuthority(Set.of(authorities));
    }

    /**
     * Checks whether the current authenticated user has all the given
     * authorities.
     *
     * @param authorities
     *            a collection containing at least one authority.
     * @return {@literal true} if the user holds all the given authorities,
     *         otherwise {@literal false}.
     * @throws IllegalArgumentException
     *             if the given collection is empty.
     */
    public boolean hasAllAuthorities(Collection<String> authorities) {
        if (authorities.isEmpty()) {
            throw new IllegalArgumentException(
                    "Must provide at least one authority to check");
        }
        return getGrantedAuthoritiesStream().collect(Collectors.toSet())
                .containsAll(authorities);
    }

    /**
     * Checks whether the current authenticated user has all the given
     * authorities.
     *
     * @param authorities
     *            an array containing at least one authority.
     * @return {@literal true} if the user holds all the given authorities,
     *         otherwise {@literal false}.
     * @throws IllegalArgumentException
     *             if the given array is empty.
     */
    public boolean hasAllAuthorities(String... authorities) {
        return hasAllAuthorities(Set.of(authorities));
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

    /**
     * Sets the role prefix holder to use when checking the current user's
     * roles.
     *
     * @param rolePrefixHolder
     *            {@link VaadinRolePrefixHolder} instance, or {@literal null} to
     *            revert to the default {@code ROLE_} prefix.
     */
    void setRolePrefixHolder(VaadinRolePrefixHolder rolePrefixHolder) {
        this.rolePrefixHolder = rolePrefixHolder;
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

    private String getRolePrefix() {
        return Optional.ofNullable(rolePrefixHolder)
                .map(VaadinRolePrefixHolder::getRolePrefix).orElse("ROLE_");
    }

    /**
     * Augments the given {@link AuthenticationContext} with Spring Security.
     *
     * This method can be used to configure the {@link AuthenticationContext}
     * when {@link VaadinSecurityConfigurer} is not used to set up Spring
     * Security.
     *
     * @param httpSecurity
     *            Spring {@link HttpSecurity} for security configuration
     * @param authCtx
     *            The authentication context of the application.
     */
    public static void applySecurityConfiguration(HttpSecurity httpSecurity,
            AuthenticationContext authCtx) {
        httpSecurity.getObject(); // Ensure http security has been built
        LogoutConfigurer<?> logoutConfigurer = httpSecurity
                .getConfigurer(LogoutConfigurer.class);
        authCtx.setLogoutHandlers(logoutConfigurer.getLogoutSuccessHandler(),
                logoutConfigurer.getLogoutHandlers());
    }

    // package protected for testing purposes
    static class CompositeLogoutHandler implements LogoutHandler, Serializable {

        private final List<LogoutHandler> logoutHandlers;

        public CompositeLogoutHandler(List<LogoutHandler> logoutHandlers) {
            Assert.notEmpty(logoutHandlers, "LogoutHandlers are required");
            this.logoutHandlers = logoutHandlers;
        }

        @Override
        public void logout(HttpServletRequest request,
                HttpServletResponse response, Authentication authentication) {
            for (LogoutHandler handler : this.logoutHandlers) {
                try {
                    handler.logout(request, response, authentication);
                } catch (IllegalStateException e) {
                    // Tolerate null response when Session is already
                    // invalidated
                    if (response != null
                            || !isContinueToNextHandler(request, handler)) {
                        throw e;
                    }
                }
            }
        }

        private boolean isContinueToNextHandler(HttpServletRequest request,
                LogoutHandler handler) {
            return handler instanceof SecurityContextLogoutHandler
                    && (request.getSession(false) == null
                            || !request.isRequestedSessionIdValid());
        }
    }

}
