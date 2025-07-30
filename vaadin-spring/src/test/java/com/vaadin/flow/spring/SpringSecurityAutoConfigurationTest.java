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

package com.vaadin.flow.spring;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;

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
import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer;
import com.vaadin.flow.spring.security.SpringAccessPathChecker;
import com.vaadin.flow.spring.security.SpringNavigationAccessControl;
import com.vaadin.flow.spring.security.VaadinWebSecurity;

import static org.assertj.core.api.Assertions.assertThat;

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
        NavigationContext navigationContext = Mockito
                .mock(NavigationContext.class);
        Mockito.when(navigationContext.getNavigationTarget())
                .thenReturn((Class) Component.class);
        Mockito.when(navigationContext.getLocation())
                .thenReturn(new Location("path"));
        Mockito.when(navigationContext.allow()).thenCallRealMethod();
        Mockito.when(navigationContext.neutral()).thenCallRealMethod();
        Mockito.when(navigationContext.deny(ArgumentMatchers.anyString()))
                .thenCallRealMethod();

        AccessCheckResult result = control.checkAccess(navigationContext,
                false);
        assertThat(result.decision()).isEqualTo(AccessCheckDecision.DENY);
        assertThat(result.reason()).isEqualTo("Custom Implementation");

        Mockito.verify(navigationContext, Mockito.times(2))
                .deny(ArgumentMatchers.anyString());
        Mockito.verify(navigationContext, Mockito.never()).allow();
        Mockito.verify(navigationContext, Mockito.never()).neutral();
    }

    private <T> void assertObjectIsSerializable(T instance) {
        Object deserialized = Assertions.assertDoesNotThrow(() -> {
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
        Assertions.assertNotNull(deserialized, "Deserialized object is null");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomAccessPathChecker extends VaadinWebSecurity {

        @Bean
        static AccessPathChecker customAccessPathChecker() {
            return DISABLED_PATH_CHECKER;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomAccessAnnotationChecker extends VaadinWebSecurity {

        @Bean
        static AccessAnnotationChecker accessAnnotationChecker() {
            return DISABLED_ANNOTATION_CHECKER;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomNavigationAccessCheckersConfigurer
            extends VaadinWebSecurity {

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

}
