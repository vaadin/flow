package com.vaadin.flow.spring.security;

import java.security.Principal;
import java.util.Collection;
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
import com.vaadin.flow.server.auth.AccessCheckDecisionResolver;
import com.vaadin.flow.server.auth.AnnotatedViewAccessChecker;
import com.vaadin.flow.server.auth.DefaultAccessCheckDecisionResolver;
import com.vaadin.flow.server.auth.NavigationAccessChecker;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.spring.AuthenticationUtil;

/**
 * A Spring specific navigation access control that falls back to Spring
 * mechanisms for user retrieval and role checking, when the generic mechanisms
 * do not work.
 * <p>
 * </p>
 * In Spring Boot application, a {@link SpringNavigationAccessControl} is
 * provided by default, but its behavior can be configured by defining a
 * {@link NavigationAccessControlConfigurer} bean.
 *
 * @see NavigationAccessControlConfigurer
 */
public class SpringNavigationAccessControl extends NavigationAccessControl {

    /**
     * Create a new instance with the default view annotation checker and
     * decision resolver.
     *
     * @see AnnotatedViewAccessChecker
     * @see DefaultAccessCheckDecisionResolver
     */
    public SpringNavigationAccessControl() {
    }

    /**
     * Create a new instance with given checkers and decision resolver.
     *
     * @param checkerList
     *            collection of navigation access checker.
     * @param decisionResolver
     *            the decision resolver.
     */
    public SpringNavigationAccessControl(
            Collection<NavigationAccessChecker> checkerList,
            AccessCheckDecisionResolver decisionResolver) {
        super(checkerList, decisionResolver);
    }

    @Override
    protected Principal getPrincipal(VaadinRequest request) {
        boolean isWebsocketPush = isWebsocketPush(request);
        if (request == null
                || (isWebsocketPush && request.getUserPrincipal() == null)) {
            return AuthenticationUtil.getSecurityHolderAuthentication();
        }
        return super.getPrincipal(request);
    }

    @Override
    protected Predicate<String> getRolesChecker(VaadinRequest request) {
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
        Optional.ofNullable(VaadinService.getCurrent())
                .map(service -> service.getContext().getAttribute(Lookup.class))
                .map(lookup -> lookup.lookup(VaadinRolePrefixHolder.class))
                .filter(prefixHolder -> !prefixHolder.isSet()).ifPresent(
                        prefixHolder -> prefixHolder.resetRolePrefix(request));

        return super.getRolesChecker(request);
    }

    private static boolean isWebsocketPush(VaadinRequest request) {
        return request != null
                && HandlerHelper.isRequestType(request,
                        HandlerHelper.RequestType.PUSH)
                && "websocket"
                        .equals(request.getHeader("X-Atmosphere-Transport"));
    }
}
