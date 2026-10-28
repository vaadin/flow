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

import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.PathPatternRequestTransformer;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

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
 * strategy is kept, any other authentication is replaced by the anonymous
 * authentication configured for the security filter chain that matches the
 * path, or by a default {@code ROLE_ANONYMOUS} one when the filter chains are
 * not given or none of them matches.
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
     * The security filter chains to read the configured anonymous
     * authentication from, or {@literal null} to use the default one.
     */
    private final transient List<SecurityFilterChain> securityFilterChains;

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
        this.securityFilterChains = null;
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
        this.securityFilterChains = null;
    }

    /**
     * Creates a new instance that uses the given
     * {@link WebInvocationPrivilegeEvaluator} to check path permissions.
     *
     * It applies the given Vaadin servlet url mapping to the input path before
     * delegating the check to the evaluator. The current authentication is read
     * from the given {@link SecurityContextHolderStrategy} instead of the one
     * installed in {@link SecurityContextHolder}. A check with a
     * {@literal null} principal uses the anonymous authentication configured
     * for the given security filter chain that matches the path, so that
     * anonymous authorities set with {@code http.anonymous(...)} are honoured
     * also when a logged-in user is bound to the current thread.
     *
     * @param evaluator
     *            evaluator to check path permissions.
     * @param urlMapping
     *            Vaadin servlet url mapping
     * @param securityContextHolderStrategy
     *            the strategy to read the current authentication from, not
     *            {@literal null}
     * @param securityFilterChains
     *            the security filter chains to read the configured anonymous
     *            authentication from, not {@literal null}
     */
    public SpringAccessPathChecker(WebInvocationPrivilegeEvaluator evaluator,
            String urlMapping,
            SecurityContextHolderStrategy securityContextHolderStrategy,
            List<SecurityFilterChain> securityFilterChains) {
        this.urlMapping = urlMapping;
        this.evaluator = evaluator;
        this.securityContextHolderStrategy = Objects.requireNonNull(
                securityContextHolderStrategy,
                "securityContextHolderStrategy must not be null");
        this.securityFilterChains = Objects.requireNonNull(securityFilterChains,
                "securityFilterChains must not be null");
    }

    @Override
    public boolean hasAccess(String path, Principal principal,
            Predicate<String> roleChecker) {
        path = RequestUtil.applyUrlMapping(urlMapping, path);
        Authentication authentication = getSecurityContextHolderStrategy()
                .getContext().getAuthentication();
        // A null principal means the check is done on behalf of an anonymous
        // user (e.g. when detecting public views), so an authenticated user
        // bound to the current thread must not be used. An anonymous
        // authentication already on the thread is kept; otherwise the one
        // configured for the matching filter chain is used, with the default
        // ROLE_ANONYMOUS token as fallback.
        if (principal == null
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            authentication = getConfiguredAnonymousAuthentication(path);
        }
        return evaluator.isAllowed(path, authentication);
    }

    private Authentication getConfiguredAnonymousAuthentication(String path) {
        if (securityFilterChains == null) {
            return ANONYMOUS_AUTHENTICATION;
        }
        // Same request the privilege evaluator builds for the path, so the
        // same filter chain is selected; the wrapper lets path pattern
        // matchers cache the parsed path on it
        HttpServletRequest request = new PathPatternRequestTransformer()
                .transform(new FilterInvocation(null, path, null)
                        .getHttpRequest());
        return securityFilterChains.stream()
                .filter(chain -> chain.matches(request)).findFirst()
                .flatMap(chain -> chain.getFilters().stream()
                        .filter(AnonymousAuthenticationFilter.class::isInstance)
                        .map(AnonymousAuthenticationFilter.class::cast)
                        .findFirst())
                // The key is never checked, the evaluator does not
                // authenticate the token
                .map(filter -> (Authentication) new AnonymousAuthenticationToken(
                        "anonymous", filter.getPrincipal(),
                        filter.getAuthorities()))
                .orElse(ANONYMOUS_AUTHENTICATION);
    }

    private SecurityContextHolderStrategy getSecurityContextHolderStrategy() {
        return securityContextHolderStrategy != null
                ? securityContextHolderStrategy
                : SecurityContextHolder.getContextHolderStrategy();
    }
}
