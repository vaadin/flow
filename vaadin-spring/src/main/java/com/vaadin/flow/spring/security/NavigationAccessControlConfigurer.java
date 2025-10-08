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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.auth.AccessCheckDecisionResolver;
import com.vaadin.flow.server.auth.AnnotatedViewAccessChecker;
import com.vaadin.flow.server.auth.DefaultAccessCheckDecisionResolver;
import com.vaadin.flow.server.auth.NavigationAccessChecker;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.server.auth.RoutePathAccessChecker;

/**
 * Allows to configure the {@link NavigationAccessControl}.
 * <p>
 * To configure Flow navigation access control, a Spring bean on type
 * {@link NavigationAccessControlConfigurer} should be defined.
 * <p>
 * </p>
 * In Spring Boot applications, a default
 * {@link NavigationAccessControlConfigurer} bean is provided. It activates
 * {@link AnnotatedViewAccessChecker}, but it disables the
 * {@link NavigationAccessControl}, for backward compatibility.
 * <p>
 * </p>
 * However, if Spring Security is configured extending
 * {@link VaadinWebSecurity}, the {@link NavigationAccessControl} is enabled
 * automatically.
 * <p>
 * </p>
 *
 * Default settings can be overridden by defining a custom
 * {@link NavigationAccessControlConfigurer} bean.
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
 * <p>
 * </p>
 * NOTE: if the bean in exposed in a configuration class that extends
 * {@link VaadinWebSecurity}, the method must be defined {@code static} to
 * prevent cyclic dependencies errors.
 *
 * <pre>
 * {@code
 * @Bean
 * class SecurityConfig extends VaadinWebSecurity {
 *     static NavigationAccessControlConfigurer navigationAccessControlConfigurer() {
 *         return new NavigationAccessControlConfigurer()
 *                 .withRoutePathAccessChecker().withLoginView(LoginView.class);
 *     }
 * }
 * }
 * </pre>
 *
 * <p>
 * </p>
 * {@link NavigationAccessControl} bean can be configured by:
 *
 * <ul>
 * <li>activating out-of-the-box navigation access checkers</li>
 * <li>providing custom navigation access checkers implementations</li>
 * <li>set the login view class or path</li>
 * <li>completely disable access control</li>
 * </ul>
 * <p>
 * </p>
 * The {@link NavigationAccessControl} will automatically be disabled if no
 * navigation access checkers are provided.
 *
 * @see NavigationAccessControl
 * @see VaadinWebSecurity
 */
public final class NavigationAccessControlConfigurer {

    private final List<NavigationAccessChecker> additionalCheckers = new ArrayList<>();
    private boolean disableAccessControl;
    private boolean enableViewAccessChecker;
    private boolean enablePathAccessChecker;
    private Class<? extends Component> loginView;
    private String loginViewPath;
    private AccessCheckDecisionResolver decisionResolver = new DefaultAccessCheckDecisionResolver();
    private Predicate<NavigationAccessChecker> accessCheckersFilter;

    /**
     * Enables the
     * {@link com.vaadin.flow.server.auth.AnnotatedViewAccessChecker}.
     *
     * @return this instance for further customization.
     */
    public NavigationAccessControlConfigurer withAnnotatedViewAccessChecker() {
        enableViewAccessChecker = true;
        return this;
    }

    /**
     * Enables the {@link com.vaadin.flow.server.auth.RoutePathAccessChecker}.
     *
     * @return this instance for further customization.
     */
    public NavigationAccessControlConfigurer withRoutePathAccessChecker() {
        enablePathAccessChecker = true;
        return this;
    }

    /**
     * Adds the given {@link NavigationAccessChecker} to the collection of
     * checker that will be used by
     * {@link com.vaadin.flow.server.auth.NavigationAccessControl}.
     * <p>
     * Custom checker will be executed after out-of-the-box checker, if they are
     * enabled.
     *
     * @param accessChecker
     *            the navigation access checker
     * @return this instance for further customization.
     */
    public NavigationAccessControlConfigurer withNavigationAccessChecker(
            NavigationAccessChecker accessChecker) {
        additionalCheckers.add(Objects.requireNonNull(accessChecker,
                "navigation access checker must not be null"));
        return this;
    }

    /**
     * Adds the given {@link NavigationAccessChecker} to the collection of
     * checker that will be used by
     * {@link com.vaadin.flow.server.auth.NavigationAccessControl}.
     * <p>
     * Custom checkers will be executed after out-of-the-box checker, if they
     * are enabled.
     *
     * @param accessChecker
     *            the navigation access checker
     * @return this instance for further customization.
     */
    public NavigationAccessControlConfigurer withNavigationAccessCheckers(
            Collection<NavigationAccessChecker> accessChecker) {
        Objects.requireNonNull(accessChecker,
                "navigation access checker collection must not be null");
        additionalCheckers.addAll(
                accessChecker.stream().filter(Objects::nonNull).toList());
        return this;
    }

    /**
     * Adds to {@link com.vaadin.flow.server.auth.NavigationAccessControl} all
     * the registered {@link NavigationAccessChecker} beans that matches the
     * given filter.
     * <p>
     * Note that the filter will not be applied to out-of-the-box checkers
     * enabled with {@link #withAnnotatedViewAccessChecker()} or
     * {@link #withRoutePathAccessChecker()}, not to checkers added within this
     * configurer instance through
     * {@link #withNavigationAccessChecker(NavigationAccessChecker)} and
     * {@link #withNavigationAccessCheckers(Collection)}.
     *
     * @param filter
     *            a function to filter the available navigation access checker
     * @return this instance for further customization.
     */
    public NavigationAccessControlConfigurer withAvailableNavigationAccessCheckers(
            Predicate<NavigationAccessChecker> filter) {
        Objects.requireNonNull(filter,
                "navigation access checker filter must not be null");
        this.accessCheckersFilter = filter;
        return this;
    }

    /**
     * Sets the {@link AccessCheckDecisionResolver} for the navigation access
     * control.
     * <p>
     * The {@link AccessCheckDecisionResolver} is responsible for taking the
     * final decision on target view access grant, based on the response of the
     * navigation access checkers.
     *
     * @param resolver
     *            the decision resolver to use for navigation access control.
     * @return this instance for further customization.
     */
    public NavigationAccessControlConfigurer withDecisionResolver(
            AccessCheckDecisionResolver resolver) {
        this.decisionResolver = Objects.requireNonNull(resolver,
                "Decision resolver must not be null");
        return this;
    }

    /**
     * Disables the {@link com.vaadin.flow.server.auth.NavigationAccessControl}.
     *
     * @return this instance for further customization.
     */
    public NavigationAccessControlConfigurer disabled() {
        this.disableAccessControl = true;
        return this;
    }

    /**
     * Sets the Flow login view to use.
     *
     * @param loginView
     *            the Flow view to use as login view
     * @return this instance for further customization.
     */
    public NavigationAccessControlConfigurer withLoginView(
            Class<? extends Component> loginView) {
        this.loginView = loginView;
        return this;
    }

    /**
     * Sets the path of the login view.
     *
     * @param loginViewPath
     *            the path of the login view
     * @return this instance for further customization.
     */
    public NavigationAccessControlConfigurer withLoginView(
            String loginViewPath) {
        this.loginViewPath = loginViewPath;
        return this;
    }

    /**
     * Builds a {@link NavigationAccessControl} instance, configured according
     * to this configurer instance settings.
     *
     * @param factory
     *            a function that build the desired type of navigation access
     *            control.
     * @param availableCheckers
     *            the list of all available navigation access checkers
     *            registered in the system.
     * @return a configured {@link NavigationAccessControl} instance, never
     *         {@literal null}.
     * @param <T>
     *            the type of the {@link NavigationAccessControl}
     */
    public <T extends NavigationAccessControl> T build(
            BiFunction<List<NavigationAccessChecker>, AccessCheckDecisionResolver, T> factory,
            List<NavigationAccessChecker> availableCheckers) {
        Objects.requireNonNull(factory,
                "navigation access control factory must not be null");
        Objects.requireNonNull(availableCheckers,
                "available checkers list must not be null");
        List<NavigationAccessChecker> availableCopy = new ArrayList<>(
                availableCheckers);
        List<NavigationAccessChecker> checkerList = new ArrayList<>();
        if (enableViewAccessChecker) {
            checkerList.add(extractRegisteredChecker(
                    AnnotatedViewAccessChecker.class, availableCopy));
        }
        if (enablePathAccessChecker) {
            checkerList.add(extractRegisteredChecker(
                    RoutePathAccessChecker.class, availableCopy));
        }

        checkerList.addAll(additionalCheckers);
        availableCopy.removeAll(additionalCheckers);

        if (accessCheckersFilter != null) {
            availableCopy.stream().filter(accessCheckersFilter)
                    .collect(Collectors.toCollection(() -> checkerList));
        }

        T accessControl = factory.apply(checkerList, decisionResolver);
        if (disableAccessControl) {
            accessControl.setEnabled(false);
        }
        if (loginView != null) {
            accessControl.setLoginView(loginView);
        }
        if (loginViewPath != null) {
            accessControl.setLoginView(loginViewPath);
        }
        return accessControl;
    }

    private static NavigationAccessChecker extractRegisteredChecker(
            Class<? extends NavigationAccessChecker> checkerType,
            List<NavigationAccessChecker> checkerList) {
        NavigationAccessChecker result = checkerList.stream()
                .filter(checker -> checkerType.equals(checker.getClass()))
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        checkerType.getName() + " is not available"));
        // Remove all potential instances from the source list
        checkerList.removeIf(checker -> checkerType.equals(checker.getClass()));
        return result;

    }
}
