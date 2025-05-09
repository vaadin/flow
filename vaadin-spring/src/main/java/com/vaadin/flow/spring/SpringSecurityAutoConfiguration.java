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

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;

import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AccessPathChecker;
import com.vaadin.flow.server.auth.AnnotatedViewAccessChecker;
import com.vaadin.flow.server.auth.NavigationAccessChecker;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.server.auth.RoutePathAccessChecker;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer;
import com.vaadin.flow.spring.security.NavigationAccessControlInitializer;
import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.SpringAccessPathChecker;
import com.vaadin.flow.spring.security.SpringNavigationAccessControl;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import com.vaadin.flow.spring.security.VaadinRolePrefixHolder;

/**
 * Spring boot auto-configuration class for Flow.
 *
 * @author Vaadin Ltd
 *
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WebSecurityCustomizer.class)
@EnableConfigurationProperties(VaadinConfigurationProperties.class)
public class SpringSecurityAutoConfiguration {

    /**
     * Makes the default request cache available for security configuration.
     *
     * @return the default request cache
     */
    @Bean
    public VaadinDefaultRequestCache vaadinDefaultRequestCache() {
        return new VaadinDefaultRequestCache();
    }

    /**
     * Makes the default navigation access control initializer available for
     * security configuration.
     *
     * @return the default navigation access control initializer
     */
    @Bean
    public NavigationAccessControlInitializer navigationAccessControlInitializer(
            NavigationAccessControl accessControl) {
        return new NavigationAccessControlInitializer(accessControl);
    }

    /**
     * Makes the default navigation access control available for security
     * configuration.
     *
     * @return the default navigation access control.
     */
    @Bean
    public NavigationAccessControl navigationAccessControl(
            List<NavigationAccessChecker> accessCheckerList,
            NavigationAccessControlConfigurer configurer) {
        return configurer.build(SpringNavigationAccessControl::new,
                accessCheckerList);
    }

    /**
     * Makes the default configurer for navigation access control available.
     * <p>
     * The default configurer only enables annotated view access checker. It is
     * disabled by default for backward compatibility, and it will be enabled by
     * {@link com.vaadin.flow.spring.security.VaadinWebSecurity}.
     * <p>
     * A custom bean can be provided to override default configuration or to
     * configure navigation access control instance when used without
     * {@link com.vaadin.flow.spring.security.VaadinWebSecurity},
     *
     * @return the default configurer for navigation access control.
     */
    @Bean
    @ConditionalOnMissingBean
    NavigationAccessControlConfigurer navigationAccessControlConfigurerCustomizer() {
        return new NavigationAccessControlConfigurer()
                .withAnnotatedViewAccessChecker().disabled();
    }

    /**
     * Makes the default annotation based view access checker available for
     * security configuration.
     *
     * @param accessAnnotationChecker
     *            the {@link AccessAnnotationChecker} bean to use
     * @return the default view access checker
     */
    @Bean
    public AnnotatedViewAccessChecker annotatedViewAccessChecker(
            AccessAnnotationChecker accessAnnotationChecker) {
        return new AnnotatedViewAccessChecker(accessAnnotationChecker);
    }

    /**
     * Makes the default route path access checker available for security
     * configuration.
     *
     * @param accessPathChecker
     *            the {@link AccessPathChecker} bean to use
     * @return the default route path access checker
     */
    @Bean
    public RoutePathAccessChecker routePathAccessChecker(
            AccessPathChecker accessPathChecker) {
        return new RoutePathAccessChecker(accessPathChecker);
    }

    /**
     * Makes the default route path access checker available for security
     * configuration.
     *
     * @param vaadinProperties
     *            vaadin configuration properties
     * @param evaluator
     *            URI privileges evaluator
     * @return the default route path access checker
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessPathChecker accessPatchChecker(
            VaadinConfigurationProperties vaadinProperties,
            @Lazy WebInvocationPrivilegeEvaluator evaluator) {
        return new SpringAccessPathChecker(evaluator,
                vaadinProperties.getUrlMapping());
    }

    /**
     * Makes the default access annotation checker available for security
     * configuration.
     * <p>
     * Fusion makes this bean available by default but if Fusion is excluded
     * from the project, we make it available here
     *
     * @return the default access annotation checker
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessAnnotationChecker accessAnnotationChecker() {
        return new AccessAnnotationChecker();
    }

    /**
     * Makes the request util available.
     *
     * @return the request util
     */
    @Bean
    public RequestUtil requestUtil() {
        return new RequestUtil();
    }

    /**
     * Makes role prefix holder available for security configuration.
     *
     * @param grantedAuthorityDefaults
     *            Optional granted authority defaults bean for the default role
     *            prefix
     * @return the role prefix holder
     */
    @Bean
    @ConditionalOnMissingBean
    public VaadinRolePrefixHolder vaadinRolePrefixHolder(
            Optional<GrantedAuthorityDefaults> grantedAuthorityDefaults) {
        return new VaadinRolePrefixHolder(grantedAuthorityDefaults
                .map(GrantedAuthorityDefaults::getRolePrefix).orElse(null));
    }

    @Bean
    @ConditionalOnMissingBean
    AuthenticationContext authenticationContext() {
        return new AuthenticationContext();
    }
}
