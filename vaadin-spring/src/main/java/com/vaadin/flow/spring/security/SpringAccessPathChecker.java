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

import com.vaadin.flow.server.auth.AccessPathChecker;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AuthorizationManagerWebInvocationPrivilegeEvaluator.HttpServletRequestTransformer;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.security.Principal;
import java.util.function.Predicate;

/**
 * A Spring specific route path access checker that delegates the check to
 * Spring Security.
 * <p>
 * </p>
 * It is used in combination with
 * {@link com.vaadin.flow.server.auth.RoutePathAccessChecker} to provide
 * path-based security to Flow
 * {@link com.vaadin.flow.server.auth.NavigationAccessControl}.
 *
 * <p>
 * </p>
 * To enable it, define a {@link NavigationAccessControlConfigurer} bean,
 * configured using
 * {@link NavigationAccessControlConfigurer#withRoutePathAccessChecker()}
 * method.
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
 * <h2>Custom Request Transformer</h2>
 * <p>
 * When using {@link SpringAccessPathChecker} with Spring Security request
 * matchers that need to access
 * {@link jakarta.servlet.http.HttpServletRequest#getUserPrincipal()}, you may
 * need to create a custom
 * {@link org.springframework.security.web.access.AuthorizationManagerWebInvocationPrivilegeEvaluator.HttpServletRequestTransformer}
 * bean using
 * {@link #principalAwareRequestTransformer(org.springframework.security.web.access.AuthorizationManagerWebInvocationPrivilegeEvaluator.HttpServletRequestTransformer)}.
 * This prevents {@link UnsupportedOperationException}s that can occur when
 * Spring Security request matchers attempt to access user principal
 * information.
 *
 * <pre>
 * {@code
 * @Bean
 * HttpServletRequestTransformer customRequestTransformer() {
 *     return SpringAccessPathChecker.principalAwareRequestTransformer(
 *             new PathPatternRequestTransformer());
 * }
 * }
 * </pre>
 *
 * An alternative is to use wrap the single request matchers using
 * {@link RequestUtil#principalAwareRequestMatcher(RequestMatcher)}.
 *
 * <pre>
 * {@code
 * &#64;Bean
 * public SecurityFilterChain webFilterChain(HttpSecurity http) {
 *     http.authorizeRequests(cfg -> cfg.requestMatchers(RequestUtil.principalAwareRequestMatcher(
 *          request -> {
 *              ...
 *              if (request.getUserPrincipal() == null) {
 *                  ....;
 *              }
 *              ...
 *              return true;
 *          }
 *     ));
 * }
 * }
 * </pre>
 */
public class SpringAccessPathChecker implements AccessPathChecker {

    private final transient WebInvocationPrivilegeEvaluator evaluator;
    private final String urlMapping;

    /**
     * Creates a new instance that uses the given
     * {@link WebInvocationPrivilegeEvaluator} to check path permissions.
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
     * delegating the check to the evaluator.
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
    }

    @Override
    public boolean hasAccess(String path, Principal principal,
            Predicate<String> roleChecker) {
        path = RequestUtil.applyUrlMapping(urlMapping, path);
        return evaluator.isAllowed(path,
                SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Provides a security-aware HTTP request transformer that applies
     * additional processing to the transformed request using
     * {@link RequestUtil.PrincipalAwareRequestWrapper}.
     * <p>
     * A custom {@link HttpServletRequestTransformer} bean handling
     * {@link HttpServletRequest#getUserPrincipal()} method should be exposed by
     * the application when {@link SpringAccessPathChecker} is used in
     * conjunction with Spring Security request matchers that requires to access
     * that information to prevent {@link UnsupportedOperationException}s.
     *
     * @param transformer
     *            the original HTTP request transformer to be wrapped
     * @return a new HTTP request transformer that wraps the transformed request
     *         with enhanced security awareness
     */
    public static HttpServletRequestTransformer principalAwareRequestTransformer(
            HttpServletRequestTransformer transformer) {
        return request -> RequestUtil.PrincipalAwareRequestWrapper
                .wrap(transformer.transform(request));
    }

}
