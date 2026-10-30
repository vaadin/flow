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
import java.util.function.Supplier;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AuthorizationManagerWebInvocationPrivilegeEvaluator.HttpServletRequestTransformer;
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
 * check is done for an anonymous user, with the anonymous authentication
 * configured for the security filter chain that would handle a request to the
 * path. When the filter chain proxy is not given, or no chain matches the path,
 * an anonymous authentication from the strategy is kept and any other
 * authentication is replaced by a default {@code ROLE_ANONYMOUS} one.
 * <p>
 * The filter chain proxy is resolved on the first check and the anonymous
 * authentication of each chain is computed once. The proxy is not serialized
 * with the checker: after deserialization the default anonymous authentication
 * is used.
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
     * Supplies the filter chain proxy to read the configured anonymous
     * authentication from, or {@literal null} to use the default one.
     */
    private final transient Supplier<FilterChainProxy> filterChainProxy;
    /**
     * Transforms the request built for the path before matching the filter
     * chains, {@literal null} when the proxy is not given.
     */
    private final transient HttpServletRequestTransformer requestTransformer;
    /**
     * The anonymous authentication of each filter chain, in proxy order,
     * computed on first use.
     */
    private transient volatile List<ChainAnonymousAuthentication> chainAnonymousAuthentications;

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
        this.filterChainProxy = null;
        this.requestTransformer = null;
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
        this.filterChainProxy = null;
        this.requestTransformer = null;
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
     * for the security filter chain that would handle a request to the path, so
     * that anonymous authorities set with {@code http.anonymous(...)} are
     * honoured also when a logged-in user is bound to the current thread.
     * <p>
     * The filter chain proxy is taken from the supplier on the first check, so
     * the checker can be created before the security filter chains are built.
     * The request used to select the chain is transformed with the given
     * transformer, which should be the one the evaluator uses.
     *
     * @param evaluator
     *            evaluator to check path permissions.
     * @param urlMapping
     *            Vaadin servlet url mapping
     * @param securityContextHolderStrategy
     *            the strategy to read the current authentication from, not
     *            {@literal null}
     * @param filterChainProxy
     *            supplies the filter chain proxy to read the configured
     *            anonymous authentication from, not {@literal null}
     * @param requestTransformer
     *            transforms the request built for the path before matching the
     *            filter chains, not {@literal null}
     */
    public SpringAccessPathChecker(WebInvocationPrivilegeEvaluator evaluator,
            String urlMapping,
            SecurityContextHolderStrategy securityContextHolderStrategy,
            Supplier<FilterChainProxy> filterChainProxy,
            HttpServletRequestTransformer requestTransformer) {
        this.urlMapping = urlMapping;
        this.evaluator = evaluator;
        this.securityContextHolderStrategy = Objects.requireNonNull(
                securityContextHolderStrategy,
                "securityContextHolderStrategy must not be null");
        this.filterChainProxy = Objects.requireNonNull(filterChainProxy,
                "filterChainProxy must not be null");
        this.requestTransformer = Objects.requireNonNull(requestTransformer,
                "requestTransformer must not be null");
    }

    @Override
    public boolean hasAccess(String path, Principal principal,
            Predicate<String> roleChecker) {
        path = RequestUtil.applyUrlMapping(urlMapping, path);
        Authentication authentication = getSecurityContextHolderStrategy()
                .getContext().getAuthentication();
        // A null principal means the check is done on behalf of an anonymous
        // user (e.g. when detecting public views), so an authenticated user
        // bound to the current thread must not be used.
        if (principal == null) {
            authentication = getAnonymousAuthentication(path, authentication);
        }
        return evaluator.isAllowed(path, authentication);
    }

    /**
     * Gets the anonymous authentication to check the path with: the one
     * configured for the filter chain that would handle a request to the path.
     * When the proxy is not available or no chain matches, an anonymous
     * authentication already on the thread is kept, otherwise the default one
     * is used.
     */
    private Authentication getAnonymousAuthentication(String path,
            Authentication current) {
        Authentication fallback = current instanceof AnonymousAuthenticationToken
                ? current
                : ANONYMOUS_AUTHENTICATION;
        if (filterChainProxy == null) {
            return fallback;
        }
        // The chains are matched against the same kind of request the
        // privilege evaluator builds, except for the servlet context
        HttpServletRequest request = requestTransformer.transform(
                new FilterInvocation(null, path, null).getHttpRequest());
        return getChainAnonymousAuthentications().stream()
                .filter(entry -> entry.chain().matches(request)).findFirst()
                .map(ChainAnonymousAuthentication::authentication)
                .orElse(fallback);
    }

    private List<ChainAnonymousAuthentication> getChainAnonymousAuthentications() {
        // The chains are fixed once the proxy is built, so the mapping is
        // computed once. A race computes the same list twice, which is
        // harmless.
        List<ChainAnonymousAuthentication> result = chainAnonymousAuthentications;
        if (result == null) {
            FilterChainProxy proxy = filterChainProxy.get();
            result = proxy == null ? List.of()
                    : proxy.getFilterChains().stream()
                            .map(chain -> new ChainAnonymousAuthentication(
                                    chain,
                                    createAnonymousAuthentication(chain)))
                            .toList();
            chainAnonymousAuthentications = result;
        }
        return result;
    }

    private static Authentication createAnonymousAuthentication(
            SecurityFilterChain chain) {
        return chain.getFilters().stream()
                .filter(AnonymousAuthenticationFilter.class::isInstance)
                .map(AnonymousAuthenticationFilter.class::cast).findFirst()
                // The token constructor rejects empty authorities
                .filter(filter -> !filter.getAuthorities().isEmpty())
                // The key is never checked, the evaluator does not
                // authenticate the token
                .map(filter -> (Authentication) new AnonymousAuthenticationToken(
                        "anonymous", filter.getPrincipal(),
                        filter.getAuthorities()))
                .orElse(ANONYMOUS_AUTHENTICATION);
    }

    private record ChainAnonymousAuthentication(SecurityFilterChain chain,
            Authentication authentication) {
    }

    private SecurityContextHolderStrategy getSecurityContextHolderStrategy() {
        return securityContextHolderStrategy != null
                ? securityContextHolderStrategy
                : SecurityContextHolder.getContextHolderStrategy();
    }
}
