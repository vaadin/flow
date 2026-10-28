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
package com.vaadin.flow.spring;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AccessCheckDecision;
import com.vaadin.flow.server.auth.AccessCheckResult;
import com.vaadin.flow.server.auth.AccessPathChecker;
import com.vaadin.flow.server.auth.AnnotatedViewAccessChecker;
import com.vaadin.flow.server.auth.NavigationAccessChecker;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.server.auth.NavigationContext;
import com.vaadin.flow.server.auth.RoutePathAccessChecker;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer;
import com.vaadin.flow.spring.security.SpringAccessPathChecker;
import com.vaadin.flow.spring.security.SpringNavigationAccessControl;
import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategy;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringSecurityAutoConfigurationTest {

    static final AccessPathChecker DISABLED_PATH_CHECKER = (path, principal,
            roleChecker) -> true;

    static final AccessAnnotationChecker DISABLED_ANNOTATION_CHECKER = new AccessAnnotationChecker() {
        public boolean hasAccess(Method method, Principal principal,
                Function<String, Boolean> roleChecker) {
            return true;
        }

        public boolean hasAccess(Class<?> cls, Principal principal,
                Function<String, Boolean> roleChecker) {
            return true;
        }
    };

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(SecurityAutoConfiguration.class,
                            SpringBootAutoConfiguration.class,
                            SpringSecurityAutoConfiguration.class));

    @Test
    void defaultConfiguration() {
        this.contextRunner.run((context) -> {
            // view access checker
            assertThat(context).hasSingleBean(AccessAnnotationChecker.class);
            assertThat(context).getBean(AccessAnnotationChecker.class)
                    .isExactlyInstanceOf(AccessAnnotationChecker.class);

            assertThat(context).hasSingleBean(AccessPathChecker.class);
            assertThat(context).getBean(AccessPathChecker.class)
                    .isInstanceOf(SpringAccessPathChecker.class);

            assertThat(context).hasSingleBean(NavigationAccessControl.class);
            assertThat(context).getBean(NavigationAccessControl.class)
                    .isInstanceOf(SpringNavigationAccessControl.class);
        });
    }

    @Test
    void customAccessPathChecker() {
        this.contextRunner.withUserConfiguration(CustomAccessPathChecker.class)
                .run((context) -> {
                    assertThat(context).hasSingleBean(AccessPathChecker.class);
                    assertThat(context).getBean(AccessPathChecker.class)
                            .isSameAs(DISABLED_PATH_CHECKER);
                });
    }

    @Test
    void customAccessAnnotationChecker() {
        this.contextRunner
                .withUserConfiguration(CustomAccessAnnotationChecker.class)
                .run((context) -> {
                    assertThat(context)
                            .hasSingleBean(AccessAnnotationChecker.class);
                    assertThat(context).getBean(AccessAnnotationChecker.class)
                            .isSameAs(DISABLED_ANNOTATION_CHECKER);
                });
    }

    @Test
    void customNavigationAccessCheckersConfigurerOnVaadinWebSecurityExtension() {
        this.contextRunner
                .withUserConfiguration(
                        CustomNavigationAccessCheckersConfigurer.class)
                .run(SpringSecurityAutoConfigurationTest::assertThatCustomNavigationAccessCheckerIsUsed);
    }

    @Test
    void customNavigationAccessCheckersConfigurerWithoutVaadinWebSecurityExtension() {
        this.contextRunner.withUserConfiguration(
                CustomNavigationAccessCheckersConfigurerWithoutVaadinWebSecurity.class)
                .run(SpringSecurityAutoConfigurationTest::assertThatCustomNavigationAccessCheckerIsUsed);
    }

    @Test
    void multipleContexts_strategyBeansShareSecurityContext() {
        SecurityContextHolderStrategy installedStrategy = SecurityContextHolder
                .getContextHolderStrategy();
        try {
            this.contextRunner.run((firstContext) -> {
                SecurityContextHolderStrategy firstStrategy = firstContext
                        .getBean(SecurityContextHolderStrategy.class);
                // A second refresh installs the strategy bean of the second
                // context into the classloader wide SecurityContextHolder,
                // like a second cached context in a test JVM does
                this.contextRunner.withPropertyValues("some.property=other")
                        .run((secondContext) -> {
                            assertThat(secondContext)
                                    .getBean(
                                            SecurityContextHolderStrategy.class)
                                    .isNotSameAs(firstStrategy);
                            assertThat(SecurityContextHolder
                                    .getContextHolderStrategy())
                                    .isNotSameAs(firstStrategy);

                            SecurityContext securityContext = SecurityContextHolder
                                    .createEmptyContext();
                            securityContext.setAuthentication(
                                    new UsernamePasswordAuthenticationToken(
                                            "user", "n/a"));
                            SecurityContextHolder.setContext(securityContext);
                            try {
                                // Beans of the first context, e.g. the method
                                // security interceptors, are wired to its own
                                // strategy bean, which has to see the same
                                // security context
                                assertThat(firstStrategy.getContext())
                                        .isSameAs(securityContext);
                            } finally {
                                SecurityContextHolder.clearContext();
                            }
                            // ... and the other way around, so that clearing
                            // after a test does not leak into the next one
                            assertThat(firstStrategy.getContext()
                                    .getAuthentication()).isNull();
                        });
            });
        } finally {
            // getContext() lazily stores an empty context in the thread local
            // shared by all instances, which would otherwise be inherited by
            // the tests running afterwards on this thread
            SecurityContextHolder.clearContext();
            SecurityContextHolder.setContextHolderStrategy(installedStrategy);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void securityBeans_readAuthenticationFromStrategyBean(
            boolean customStrategyBean) {
        SecurityContextHolderStrategy installedStrategy = SecurityContextHolder
                .getContextHolderStrategy();
        WebInvocationPrivilegeEvaluator evaluator = mock(
                WebInvocationPrivilegeEvaluator.class);
        var runner = this.contextRunner.withBean(
                WebInvocationPrivilegeEvaluator.class, () -> evaluator);
        if (customStrategyBean) {
            runner = runner.withBean(SecurityContextHolderStrategy.class,
                    SimpleSecurityContextHolderStrategy::new);
        }
        try {
            runner.run(context -> assertSecurityBeansReadStrategyBean(context,
                    evaluator, customStrategyBean));
        } finally {
            SecurityContextHolder.clearContext();
            SecurityContextHolder.setContextHolderStrategy(installedStrategy);
        }
    }

    private static void assertSecurityBeansReadStrategyBean(
            AssertableWebApplicationContext context,
            WebInvocationPrivilegeEvaluator evaluator,
            boolean customStrategyBean) {
        SecurityContextHolderStrategy strategyBean = context
                .getBean(SecurityContextHolderStrategy.class);
        assertThat(strategyBean).isInstanceOf(
                customStrategyBean ? SimpleSecurityContextHolderStrategy.class
                        : VaadinAwareSecurityContextHolderStrategy.class);
        assertThat(SecurityContextHolder.getContextHolderStrategy())
                .isSameAs(strategyBean);

        // Replace the installed strategy with one that sees no
        // authentication, so the beans only find the user through the
        // injected strategy bean
        SecurityContextHolderStrategy otherStrategy = mock(
                SecurityContextHolderStrategy.class);
        when(otherStrategy.getContext())
                .thenReturn(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.setContextHolderStrategy(otherStrategy);

        var user = new UsernamePasswordAuthenticationToken("user", "n/a",
                AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContext securityContext = strategyBean.createEmptyContext();
        securityContext.setAuthentication(user);
        strategyBean.setContext(securityContext);
        try {
            assertThat(context.getBean(AuthenticationContext.class)
                    .getPrincipalName()).contains("user");
            context.getBean(AccessPathChecker.class).hasAccess("protected",
                    user, role -> false);
            verify(evaluator).isAllowed(anyString(), eq(user));
        } finally {
            strategyBean.clearContext();
        }
    }

    @Test
    void securityBeansAreSerializable() {
        this.contextRunner.run((context) -> {

            // view access checker
            assertThat(context).getBean(AccessAnnotationChecker.class)
                    .satisfies(this::assertObjectIsSerializable);

            assertThat(context).getBean(AnnotatedViewAccessChecker.class)
                    .satisfies(this::assertObjectIsSerializable);

            assertThat(context).getBean(AccessPathChecker.class)
                    .satisfies(this::assertObjectIsSerializable);

            assertThat(context).getBean(RoutePathAccessChecker.class)
                    .satisfies(this::assertObjectIsSerializable);

            assertThat(context).getBean(NavigationAccessControl.class)
                    .satisfies(this::assertObjectIsSerializable);
        });

    }

    private static void assertThatCustomNavigationAccessCheckerIsUsed(
            AssertableWebApplicationContext context) {
        assertThat(context).hasSingleBean(NavigationAccessControl.class);
        NavigationAccessControl control = context
                .getBean(NavigationAccessControl.class);
        NavigationContext navigationContext = mock(NavigationContext.class);
        when(navigationContext.getNavigationTarget())
                .thenReturn((Class) Component.class);
        when(navigationContext.getLocation()).thenReturn(new Location("path"));
        when(navigationContext.allow()).thenCallRealMethod();
        when(navigationContext.neutral()).thenCallRealMethod();
        when(navigationContext.deny(anyString())).thenCallRealMethod();

        AccessCheckResult result = control.checkAccess(navigationContext,
                false);
        assertThat(result.decision()).isEqualTo(AccessCheckDecision.DENY);
        assertThat(result.reason()).isEqualTo("Custom Implementation");

        verify(navigationContext, times(2)).deny(anyString());
        verify(navigationContext, never()).allow();
        verify(navigationContext, never()).neutral();
    }

    private <T> void assertObjectIsSerializable(T instance) {
        Object deserialized = assertDoesNotThrow(() -> {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bs)) {
                out.writeObject(instance);
            }
            byte[] data = bs.toByteArray();
            try (ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(data))) {

                @SuppressWarnings("unchecked")
                T readObject = (T) in.readObject();
                return readObject;
            }
        });
        assertNotNull(deserialized, "Deserialized object is null");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomAccessPathChecker extends BaseSecurityClass {

        @Bean
        static AccessPathChecker customAccessPathChecker() {
            return DISABLED_PATH_CHECKER;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomAccessAnnotationChecker extends BaseSecurityClass {

        @Bean
        static AccessAnnotationChecker accessAnnotationChecker() {
            return DISABLED_ANNOTATION_CHECKER;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomNavigationAccessCheckersConfigurer
            extends BaseSecurityClass {

        private static final NavigationAccessChecker CUSTOM = context -> context
                .deny("Custom Implementation");

        @Bean
        static NavigationAccessControlConfigurer navigationAccessCheckersConfigurer() {
            return new NavigationAccessControlConfigurer()
                    .withNavigationAccessChecker(CUSTOM);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomNavigationAccessCheckersConfigurerWithoutVaadinWebSecurity {

        private static final NavigationAccessChecker CUSTOM = context -> context
                .deny("Custom Implementation");

        @Bean
        NavigationAccessControlConfigurer navigationAccessCheckersConfigurer() {
            return new NavigationAccessControlConfigurer()
                    .withNavigationAccessChecker(CUSTOM);
        }
    }

    private static class SimpleSecurityContextHolderStrategy
            implements SecurityContextHolderStrategy {

        private SecurityContext context = createEmptyContext();

        @Override
        public void clearContext() {
            context = createEmptyContext();
        }

        @Override
        public SecurityContext getContext() {
            return context;
        }

        @Override
        public void setContext(SecurityContext context) {
            this.context = context;
        }

        @Override
        public SecurityContext createEmptyContext() {
            return new SecurityContextImpl();
        }
    }

    @EnableWebSecurity
    private static class BaseSecurityClass {

        @Bean
        public SecurityFilterChain vaadinSecurityFilterChain(
                HttpSecurity httpSecurity) {
            return httpSecurity.with(VaadinSecurityConfigurer.vaadin(),
                    Customizer.withDefaults()).build();
        }
    }

}
