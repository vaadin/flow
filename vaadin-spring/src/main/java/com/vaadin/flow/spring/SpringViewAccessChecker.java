/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import java.security.Principal;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;

import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.ViewAccessChecker;

/**
 * A Spring specific view access checker that falls back to Spring mechanisms
 * when the generic mechanisms do not work.
 */
public class SpringViewAccessChecker extends ViewAccessChecker {

    /**
     * Creates an instance with the given annotation checker.
     *
     * The created instance is disabled by default.
     *
     * @param accessAnnotationChecker
     *            the annotation checker to use
     */
    public SpringViewAccessChecker(
            AccessAnnotationChecker accessAnnotationChecker) {
        super(accessAnnotationChecker);
    }

    @Override
    protected Principal getPrincipal(VaadinRequest request) {
        boolean isWebsocketPush = isWebsocketPush(request);
        if (request == null
                || (isWebsocketPush && request.getUserPrincipal() == null)) {
            return AuthenticationUtil.getSecurityHolderAuthentication();
        }
        return request.getUserPrincipal();
    }

    @Override
    protected Function<String, Boolean> getRolesChecker(VaadinRequest request) {
        boolean isWebsocketPush = isWebsocketPush(request);

        // Role checks on PUSH request works out of the box only happen if
        // transport is not WEBSOCKET.
        // For websocket PUSH, HttServletRequest#isUserInRole method in
        // Atmosphere HTTP request wrapper always returns, so we need to
        // fall back to Spring Security.
        if (request == null || isWebsocketPush) {
            AtomicReference<Function<String, Boolean>> roleCheckerHolder = new AtomicReference<>();
            Runnable roleCheckerLookup = AuthenticationUtil::getSecurityHolderRoleChecker;

            Authentication authentication = AuthenticationUtil
                    .getSecurityHolderAuthentication();
            // Spring Security context holder might not have been initialized
            // for thread handling websocket message. If so, create a temporary
            // security context based on the handshake request principal.
            if (authentication == null && isWebsocketPush
                    && request.getUserPrincipal() instanceof Authentication) {
                roleCheckerLookup = new DelegatingSecurityContextRunnable(
                        roleCheckerLookup,
                        new SecurityContextImpl((Authentication) request));
            }

            roleCheckerLookup.run();
            return roleCheckerHolder.get();
        }

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
