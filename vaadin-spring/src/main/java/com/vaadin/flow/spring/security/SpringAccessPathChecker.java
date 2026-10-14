/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.security;

import java.security.Principal;
import java.util.function.Predicate;

import org.springframework.security.core.context.SecurityContextHolder;
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
}
