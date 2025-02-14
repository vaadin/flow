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

import java.security.Principal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.spring.AuthenticationUtil;

/**
 * Helper class to access user information and perform roles checks.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class SecurityUtil {

    /**
     * Gets the principal for the currently logged in user.
     *
     * @param request
     *            the current request or {@code null} if no request is in
     *            progress (e.g. in a background thread)
     * @return a representation of the currently logged in user or {@code null}
     *         if no user is currently logged in
     */
    static Principal getPrincipal(VaadinRequest request) {
        boolean isWebsocketPush = isWebsocketPush(request);
        if (request == null
                || (isWebsocketPush && request.getUserPrincipal() == null)) {
            return AuthenticationUtil.getSecurityHolderAuthentication();
        }
        return request.getUserPrincipal();
    }

    /**
     * Gets a function for checking roles for the currently logged in user.
     *
     * @param request
     *            the current request or {@code null} if no request is in
     *            progress (e.g. in a background thread)
     * @return a function which takes a role name and returns {@code true} if
     *         the user is included in that role
     */
    static Predicate<String> getRolesChecker(VaadinRequest request) {
        boolean isWebsocketPush = isWebsocketPush(request);

        // Role checks on PUSH request works out of the box only happen if
        // transport is not WEBSOCKET.
        // For websocket PUSH, HttServletRequest#isUserInRole method in
        // Atmosphere HTTP request wrapper always returns, so we need to
        // fall back to Spring Security.
        if (request == null || isWebsocketPush) {
            AtomicReference<Function<String, Boolean>> roleCheckerHolder = new AtomicReference<>();
            Runnable roleCheckerLookup = () -> roleCheckerHolder.set(Optional
                    .ofNullable(request).map(VaadinRequest::getService)
                    .or(() -> Optional.ofNullable(VaadinService.getCurrent()))
                    .map(service -> service.getContext()
                            .getAttribute(Lookup.class))
                    .map(lookup -> lookup.lookup(VaadinRolePrefixHolder.class))
                    .map(VaadinRolePrefixHolder::getRolePrefix)
                    .map(AuthenticationUtil::getSecurityHolderRoleChecker)
                    .orElseGet(
                            AuthenticationUtil::getSecurityHolderRoleChecker));

            Authentication authentication = AuthenticationUtil
                    .getSecurityHolderAuthentication();
            // Spring Security context holder might not have been initialized
            // for thread handling websocket message. If so, create a temporary
            // security context based on the handshake request principal.
            if (authentication == null && isWebsocketPush && request
                    .getUserPrincipal() instanceof Authentication requestAuthentication) {
                roleCheckerLookup = new DelegatingSecurityContextRunnable(
                        roleCheckerLookup,
                        new SecurityContextImpl(requestAuthentication));
            }

            roleCheckerLookup.run();
            return roleCheckerHolder.get()::apply;
        }

        // Update active role prefix if it's not set yet.
        Optional.ofNullable(request.getService())
                .map(service -> service.getContext().getAttribute(Lookup.class))
                .map(lookup -> lookup.lookup(VaadinRolePrefixHolder.class))
                .filter(prefixHolder -> !prefixHolder.isSet()).ifPresent(
                        prefixHolder -> prefixHolder.resetRolePrefix(request));

        return request::isUserInRole;
    }

    private static boolean isWebsocketPush(VaadinRequest request) {
        return request != null
                && HandlerHelper.isRequestType(request,
                        HandlerHelper.RequestType.PUSH)
                && "websocket"
                        .equals(request.getHeader("X-Atmosphere-Transport"));
    }

}
