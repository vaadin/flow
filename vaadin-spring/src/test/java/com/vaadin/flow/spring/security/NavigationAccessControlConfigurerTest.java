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
import java.util.function.BiFunction;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AccessCheckDecisionResolver;
import com.vaadin.flow.server.auth.AccessCheckResult;
import com.vaadin.flow.server.auth.AnnotatedViewAccessChecker;
import com.vaadin.flow.server.auth.DefaultAccessCheckDecisionResolver;
import com.vaadin.flow.server.auth.NavigationAccessChecker;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.server.auth.NavigationContext;
import com.vaadin.flow.server.auth.RoutePathAccessChecker;

class NavigationAccessControlConfigurerTest {

    NavigationAccessControlConfigurer configurer = new NavigationAccessControlConfigurer();

    BiFunction<List<NavigationAccessChecker>, AccessCheckDecisionResolver, TestNavigationAccessControl> factory = TestNavigationAccessControl::new;
    List<NavigationAccessChecker> registeredCheckers = new ArrayList<>();
    AnnotatedViewAccessChecker viewAccessChecker = new AnnotatedViewAccessChecker(
            new AccessAnnotationChecker());
    RoutePathAccessChecker routePathAccessChecker = new RoutePathAccessChecker(
            (path, principal, roleChecker) -> false);

    @BeforeEach
    void setUp() {
        registeredCheckers.add(viewAccessChecker);
        registeredCheckers.add(routePathAccessChecker);
        registeredCheckers.add(new TestAccessChecker(10));
        registeredCheckers.add(new TestAccessChecker(15));
        registeredCheckers.add(new TestAccessChecker(20));
        registeredCheckers.add(new TestAccessChecker(30));
    }

    @Test
    void build_defaults() {
        TestNavigationAccessControl accessControl = configurer.build(factory,
                registeredCheckers);
        Assertions.assertThat(accessControl.isEnabled()).isTrue();
        Assertions.assertThat(accessControl.decisionResolver)
                .isExactlyInstanceOf(DefaultAccessCheckDecisionResolver.class);
    }

    @Test
    void build_disable() {
        TestNavigationAccessControl accessControl = configurer.disabled()
                .build(factory, registeredCheckers);

        Assertions.assertThat(accessControl.isEnabled()).isFalse();
    }

    @Test
    void build_customDecisionResolver() {
        AccessCheckDecisionResolver customResolver = (results,
                context) -> context.deny("CUSTOM");
        TestNavigationAccessControl accessControl = configurer
                .withDecisionResolver(customResolver)
                .build(factory, registeredCheckers);

        Assertions.assertThat(accessControl.decisionResolver)
                .isSameAs(customResolver);
    }

    @Test
    void build_enableViewAccessChecker() {
        TestNavigationAccessControl accessControl = configurer
                .withAnnotatedViewAccessChecker()
                .build(factory, registeredCheckers);

        Assertions.assertThat(accessControl.checkerList)
                .containsExactly(viewAccessChecker);
    }

    @Test
    void build_enableViewAccessChecker_checkerNotAvailable_throws() {
        registeredCheckers.remove(viewAccessChecker);
        Assertions.assertThatIllegalStateException()
                .isThrownBy(() -> configurer.withAnnotatedViewAccessChecker()
                        .build(factory, registeredCheckers))
                .withMessageContaining(
                        AnnotatedViewAccessChecker.class.getName())
                .withMessageContaining(" is not available");
    }

    @Test
    void build_enableRoutePathAccessChecker() {
        TestNavigationAccessControl accessControl = configurer
                .withRoutePathAccessChecker()
                .build(factory, registeredCheckers);

        Assertions.assertThat(accessControl.checkerList)
                .containsExactly(routePathAccessChecker);
    }

    @Test
    void build_enableRoutePathAccessChecker_checkerNotAvailable_throws() {
        registeredCheckers.remove(routePathAccessChecker);
        Assertions.assertThatIllegalStateException()
                .isThrownBy(() -> configurer.withRoutePathAccessChecker()
                        .build(factory, registeredCheckers))
                .withMessageContaining(RoutePathAccessChecker.class.getName())
                .withMessageContaining(" is not available");
    }

    @Test
    void build_customAccessChecker() {
        TestAccessChecker accessChecker = new TestAccessChecker(99);
        TestNavigationAccessControl accessControl = configurer
                .withNavigationAccessChecker(accessChecker)
                .build(factory, registeredCheckers);

        Assertions.assertThat(accessControl.checkerList)
                .containsExactly(accessChecker);
    }

    @Test
    void build_customAccessCheckers() {
        List<NavigationAccessChecker> accessCheckers = List.of(
                new TestAccessChecker(99), new TestAccessChecker(100),
                new TestAccessChecker(101));
        TestNavigationAccessControl accessControl = configurer
                .withNavigationAccessCheckers(accessCheckers)
                .build(factory, registeredCheckers);

        Assertions.assertThat(accessControl.checkerList)
                .containsExactlyElementsOf(accessCheckers);
    }

    @Test
    void build_customAccessChecker_calledMultipleTimes() {
        List<NavigationAccessChecker> accessCheckers = List.of(
                new TestAccessChecker(99), new TestAccessChecker(100),
                new TestAccessChecker(101));
        TestAccessChecker accessChecker1 = new TestAccessChecker(60);
        TestAccessChecker accessChecker2 = new TestAccessChecker(70);
        TestAccessChecker accessChecker3 = new TestAccessChecker(130);
        TestNavigationAccessControl accessControl = configurer
                .withNavigationAccessChecker(accessChecker1)
                .withNavigationAccessChecker(accessChecker2)
                .withNavigationAccessCheckers(accessCheckers)
                .withNavigationAccessChecker(accessChecker3)
                .build(factory, registeredCheckers);

        List<NavigationAccessChecker> expectedOrder = new ArrayList<>();
        expectedOrder.add(accessChecker1);
        expectedOrder.add(accessChecker2);
        expectedOrder.addAll(accessCheckers);
        expectedOrder.add(accessChecker3);
        Assertions.assertThat(accessControl.checkerList)
                .containsExactlyElementsOf(expectedOrder);
    }

    @Test
    void build_outOfTheBoxAndCustomAccessCheckers_outOfTheBoxFirst() {
        List<NavigationAccessChecker> accessCheckers = List.of(
                new TestAccessChecker(99), new TestAccessChecker(100),
                new TestAccessChecker(101));
        TestNavigationAccessControl accessControl = configurer
                .withRoutePathAccessChecker().withAnnotatedViewAccessChecker()
                .withNavigationAccessCheckers(accessCheckers)
                .build(factory, registeredCheckers);

        List<NavigationAccessChecker> expectedOrder = new ArrayList<>();
        expectedOrder.add(viewAccessChecker);
        expectedOrder.add(routePathAccessChecker);
        expectedOrder.addAll(accessCheckers);
        Assertions.assertThat(accessControl.checkerList)
                .containsExactlyElementsOf(expectedOrder);
    }

    @Test
    void build_filterAvailableAccessCheckers() {
        // Accept all registered checker
        TestNavigationAccessControl accessControl = configurer
                .withAvailableNavigationAccessCheckers(checker -> true)
                .build(factory, registeredCheckers);

        List<NavigationAccessChecker> expectedOrder = new ArrayList<>();
        expectedOrder.addAll(registeredCheckers);
        Assertions.assertThat(accessControl.checkerList)
                .containsExactlyElementsOf(expectedOrder);

        // Remove all checkers
        configurer = new NavigationAccessControlConfigurer();
        accessControl = configurer
                .withAvailableNavigationAccessCheckers(checker -> false)
                .build(factory, registeredCheckers);

        Assertions.assertThat(accessControl.checkerList).isEmpty();

        // Custom filter registered checker
        configurer = new NavigationAccessControlConfigurer();
        accessControl = configurer
                .withAvailableNavigationAccessCheckers(
                        checker -> checker instanceof RoutePathAccessChecker
                                || (checker instanceof TestAccessChecker t
                                        && t.id % 15 == 0))
                .build(factory, registeredCheckers);

        expectedOrder = new ArrayList<>();
        expectedOrder.add(routePathAccessChecker);
        expectedOrder.add(registeredCheckers.get(3));
        expectedOrder.add(registeredCheckers.get(5));
        Assertions.assertThat(accessControl.checkerList)
                .containsExactlyElementsOf(expectedOrder);
    }

    @Test
    void build_filterAvailableAccessCheckers_outOfTheBoxCheckerPreserved() {
        TestAccessChecker additionalChecker = new TestAccessChecker(33);
        // Accept all registered checker
        TestNavigationAccessControl accessControl = configurer
                .withAvailableNavigationAccessCheckers(
                        checker -> (checker instanceof TestAccessChecker t
                                && t.id % 15 == 0))
                .withAnnotatedViewAccessChecker().withRoutePathAccessChecker()
                .withNavigationAccessChecker(additionalChecker)
                .build(factory, registeredCheckers);

        List<NavigationAccessChecker> expectedOrder = new ArrayList<>();
        expectedOrder.add(viewAccessChecker);
        expectedOrder.add(routePathAccessChecker);
        expectedOrder.add(additionalChecker);
        expectedOrder.add(registeredCheckers.get(3));
        expectedOrder.add(registeredCheckers.get(5));
        Assertions.assertThat(accessControl.checkerList)
                .containsExactlyElementsOf(expectedOrder);

    }

    @Test
    void build_loginView() {
        TestNavigationAccessControl accessControl = configurer
                .withLoginView(LoginView.class)
                .build(factory, registeredCheckers);

        Assertions.assertThat(accessControl.getLoginView())
                .isEqualTo(LoginView.class);
        Assertions.assertThat(accessControl.getLoginUrl()).isNull();
    }

    @Test
    void build_loginViewPath() {
        TestNavigationAccessControl accessControl = configurer
                .withLoginView("/login").build(factory, registeredCheckers);

        Assertions.assertThat(accessControl.getLoginUrl()).isEqualTo("/login");
        Assertions.assertThat(accessControl.getLoginView()).isNull();
    }

    @Test
    void build_bothLoginViewAndPath_throws() {
        Assertions.assertThatIllegalStateException()
                .isThrownBy(() -> configurer.withLoginView(LoginView.class)
                        .withLoginView("/login")
                        .build(factory, registeredCheckers))
                .withMessageContaining("Already using")
                .withMessageContaining("as the login view");
    }

    private static class LoginView extends Component {
    }

    private static class TestNavigationAccessControl
            extends NavigationAccessControl {

        final Collection<NavigationAccessChecker> checkerList;
        final AccessCheckDecisionResolver decisionResolver;

        public TestNavigationAccessControl(
                Collection<NavigationAccessChecker> checkerList,
                AccessCheckDecisionResolver decisionResolver) {
            super(checkerList, decisionResolver);
            this.checkerList = checkerList;
            this.decisionResolver = decisionResolver;
        }

        @Override
        public String getLoginUrl() {
            return super.getLoginUrl();
        }

        @Override
        public Class<? extends Component> getLoginView() {
            return super.getLoginView();
        }

    }

    private static class TestAccessChecker implements NavigationAccessChecker {

        private final int id;

        TestAccessChecker(int id) {
            this.id = id;
        }

        @Override
        public AccessCheckResult check(NavigationContext context) {
            return context.deny("TEST " + id);
        }

        @Override
        public String toString() {
            return "TestAccessChecker " + id;
        }
    }
}