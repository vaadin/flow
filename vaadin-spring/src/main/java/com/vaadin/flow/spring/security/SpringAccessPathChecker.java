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
import java.util.function.Predicate;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;

import com.vaadin.flow.server.auth.AccessPathChecker;

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
 */
public class SpringAccessPathChecker implements AccessPathChecker {

    private final transient SecurityContextHolderStrategy securityContextHolderStrategy;
    private final transient WebInvocationPrivilegeEvaluator evaluator;
    private final String urlMapping;

    /**
     * Creates a new instance that uses the given
     * {@link WebInvocationPrivilegeEvaluator} to check path permissions.
     *
     * @param evaluator
     *            evaluator to check path permissions.
     * @deprecated Use
     *             {@link #SpringAccessPathChecker(WebInvocationPrivilegeEvaluator, String)}
     */
    @Deprecated(since = "24.8", forRemoval = true)
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
     * @deprecated Use
     *             {@link #SpringAccessPathChecker(SecurityContextHolderStrategy, WebInvocationPrivilegeEvaluator, String)}
     */
    @Deprecated(since = "24.8", forRemoval = true)
    public SpringAccessPathChecker(WebInvocationPrivilegeEvaluator evaluator,
            String urlMapping) {
        this(SecurityContextHolder.getContextHolderStrategy(), evaluator,
                urlMapping);
    }

    /**
     * Creates a new instance that uses the given
     * {@link SecurityContextHolderStrategy} to get the security context and
     * {@link WebInvocationPrivilegeEvaluator} to check path permissions.
     *
     * @param securityContextHolderStrategy
     *            strategy to get the security context
     * @param evaluator
     *            evaluator to check path permissions
     */
    public SpringAccessPathChecker(
            SecurityContextHolderStrategy securityContextHolderStrategy,
            WebInvocationPrivilegeEvaluator evaluator) {
        this(securityContextHolderStrategy, evaluator, null);
    }

    /**
     * Creates a new instance that uses the given
     * {@link SecurityContextHolderStrategy} to get the security context and
     * {@link WebInvocationPrivilegeEvaluator} to check path permissions.
     * <p>
     * It applies the given Vaadin servlet url mapping to the input path before
     * delegating the check to the evaluator.
     *
     * @param securityContextHolderStrategy
     *            strategy to get the security context
     * @param evaluator
     *            evaluator to check path permissions
     * @param urlMapping
     *            Vaadin servlet url mapping
     */
    public SpringAccessPathChecker(
            SecurityContextHolderStrategy securityContextHolderStrategy,
            WebInvocationPrivilegeEvaluator evaluator, String urlMapping) {
        this.securityContextHolderStrategy = securityContextHolderStrategy;
        this.evaluator = evaluator;
        this.urlMapping = urlMapping;
    }

    @Override
    public boolean hasAccess(String path, Principal principal,
            Predicate<String> roleChecker) {
        path = RequestUtil.applyUrlMapping(urlMapping, path);
        return evaluator.isAllowed(path,
                securityContextHolderStrategy.getContext().getAuthentication());
    }
}
