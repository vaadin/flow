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
package com.vaadin.flow.spring.security;

import java.security.Principal;
import java.util.Objects;
import java.util.function.Predicate;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;

import com.vaadin.flow.server.auth.AccessPathChecker;

/**
 * A Spring specific route path access checker that delegates the check to
 * Spring Security.
 * <p>
 *
 * It is used in combination with
 * {@link com.vaadin.flow.server.auth.RoutePathAccessChecker} to provide
 * path-based security to Flow
 * {@link com.vaadin.flow.server.auth.NavigationAccessControl}.
 *
 * <p>
 *
 * To enable it, define a {@link NavigationAccessControlConfigurer} bean,
 * configured using
 * {@link NavigationAccessControlConfigurer#withRoutePathAccessChecker()}
 * method.
 * <p>
 *
 * The current authentication is read from a
 * {@link SecurityContextHolderStrategy}. When the principal passed to
 * {@link #hasAccess(String, Principal, Predicate)} is {@literal null}, the
 * check is done for an anonymous user: an anonymous authentication from the
 * strategy is kept, any other authentication is replaced by a default
 * {@code ROLE_ANONYMOUS} one.
 *
 * <pre>
 * {@code
 * @Bean
 * NavigationAccessControlConfigurer navigationAccessControlConfigurer() {
 *     return new NavigationAccessControlConfigurer()
 *             .withRoutePathAccessChecker().withLoginView(LoginView.class);
 * }
 * }
 * </pre>
 * 
 * @since 24.3
 */
public class SpringAccessPathChecker implements AccessPathChecker {

    private static final Authentication ANONYMOUS_AUTHENTICATION = new AnonymousAuthenticationToken(
            "anonymous", "anonymousUser",
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    private final transient WebInvocationPrivilegeEvaluator evaluator;
    /** The Vaadin servlet url mapping the checked paths are relative to. */
    private final String urlMapping;
    /**
     * The strategy to read the security context from, or {@literal null} to use
     * the one installed in {@link SecurityContextHolder} at check time.
     */
    private final transient SecurityContextHolderStrategy securityContextHolderStrategy;

    /**
     * Creates a new instance that uses the given
     * {@link WebInvocationPrivilegeEvaluator} to check path permissions.
     * <p>
     * The current authentication is read from the strategy installed in
     * {@link SecurityContextHolder}.
     *
     * @param evaluator
     *            evaluator to check path permissions.
     */
    public SpringAccessPathChecker(WebInvocationPrivilegeEvaluator evaluator) {
        this(evaluator, null);
    }

    /**
     * Creates a new instance that uses the given
     * {@link WebInvocationPrivilegeEvaluator} to check path permissions.
     *
     * It applies the given Vaadin servlet url mapping to the input path before
     * delegating the check to the evaluator. The current authentication is read
     * from the strategy installed in {@link SecurityContextHolder}.
     *
     * @param evaluator
     *            evaluator to check path permissions.
     * @param urlMapping
     *            Vaadin servlet url mapping
     */
    public SpringAccessPathChecker(WebInvocationPrivilegeEvaluator evaluator,
            String urlMapping) {
        this.urlMapping = urlMapping;
        this.evaluator = evaluator;
        this.securityContextHolderStrategy = null;
    }

    /**
     * Creates a new instance that uses the given
     * {@link WebInvocationPrivilegeEvaluator} to check path permissions.
     *
     * It applies the given Vaadin servlet url mapping to the input path before
     * delegating the check to the evaluator. The current authentication is read
     * from the given {@link SecurityContextHolderStrategy} instead of the one
     * installed in {@link SecurityContextHolder}.
     *
     * @param evaluator
     *            evaluator to check path permissions.
     * @param urlMapping
     *            Vaadin servlet url mapping
     * @param securityContextHolderStrategy
     *            the strategy to read the current authentication from, not
     *            {@literal null}
     */
    public SpringAccessPathChecker(WebInvocationPrivilegeEvaluator evaluator,
            String urlMapping,
            SecurityContextHolderStrategy securityContextHolderStrategy) {
        this.urlMapping = urlMapping;
        this.evaluator = evaluator;
        this.securityContextHolderStrategy = Objects.requireNonNull(
                securityContextHolderStrategy,
                "securityContextHolderStrategy must not be null");
    }

    @Override
    public boolean hasAccess(String path, Principal principal,
            Predicate<String> roleChecker) {
        path = RequestUtil.applyUrlMapping(urlMapping, path);
        Authentication authentication = getSecurityContextHolderStrategy()
                .getContext().getAuthentication();
        // A null principal means the check is done on behalf of an anonymous
        // user (e.g. when detecting public views), so an authenticated user
        // bound to the current thread must not be used. The configured
        // anonymous authentication is only honoured when it is already on the
        // thread; for a logged-in user, or when there is no authentication,
        // the default ROLE_ANONYMOUS token is used.
        if (principal == null
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            authentication = ANONYMOUS_AUTHENTICATION;
        }
        return evaluator.isAllowed(path, authentication);
    }

    private SecurityContextHolderStrategy getSecurityContextHolderStrategy() {
        return securityContextHolderStrategy != null
                ? securityContextHolderStrategy
                : SecurityContextHolder.getContextHolderStrategy();
    }
}
