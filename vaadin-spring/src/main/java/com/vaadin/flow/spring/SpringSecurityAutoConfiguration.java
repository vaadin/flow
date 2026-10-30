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

import jakarta.servlet.Filter;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.access.AuthorizationManagerWebInvocationPrivilegeEvaluator.HttpServletRequestTransformer;
import org.springframework.security.web.access.PathPatternRequestTransformer;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.security.web.debug.DebugFilter;

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
import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategy;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import com.vaadin.flow.spring.security.VaadinRolePrefixHolder;

/**
 * Spring Boot auto-configuration class for the Flow Spring Security
 * integration.
 * <p>
 * This configuration, together with the beans it exposes (such as
 * {@link RequestUtil}), is only applied when Spring Security is on the
 * classpath, as required by the {@link ConditionalOnClass} on
 * {@link WebSecurityCustomizer}. The {@code spring-security-config} dependency
 * is optional in the Vaadin Spring support, so an application that relies on
 * these beans has to add it explicitly; otherwise the whole configuration is
 * skipped and the beans are not created.
 * <p>
 * In a Spring Boot application this class is registered automatically through
 * the auto-configuration import mechanism. A plain Spring application that does
 * not use that mechanism (for example a Spring MVC application that registers
 * the {@link com.vaadin.flow.spring.SpringServlet} itself, similarly to
 * {@link VaadinMVCWebAppInitializer}) will not register it automatically; as an
 * auto-configuration class it is not intended to be discovered through a
 * component scan. Register it explicitly instead, e.g. with
 * {@code @Import(SpringSecurityAutoConfiguration.class)}, and make sure
 * {@code spring-security-config} is present.
 *
 * @author Vaadin Ltd
 *
 * @since 17.0
 */
@AutoConfiguration
@ConditionalOnClass(WebSecurityCustomizer.class)
@EnableConfigurationProperties(VaadinConfigurationProperties.class)
public class SpringSecurityAutoConfiguration {

    @Autowired
    private ObjectProvider<SecurityContextHolderStrategy> securityContextHolderStrategy;

    /**
     * The transformer the privilege evaluator applies to the request built for
     * a path, when the application defines one.
     */
    @Autowired
    private ObjectProvider<HttpServletRequestTransformer> privilegeEvaluatorRequestTransformer;

    @Autowired
    private BeanFactory beanFactory;

    /**
     * Creates the auto configuration. Spring instantiates it.
     */
    public SpringSecurityAutoConfiguration() {
        // Default constructor
    }

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
     * @param accessControl
     *            the navigation access control
     * @return the default navigation access control initializer
     * @since 24.3
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
     * @param accessCheckerList
     *            the list of navigation access checkers
     * @param configurer
     *            the navigation access control configurer
     * @return the default navigation access control.
     * @since 24.3
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
     * {@link com.vaadin.flow.spring.security.VaadinSecurityConfigurer}.
     * <p>
     * A custom bean can be provided to override default configuration or to
     * configure navigation access control instance when used without
     * {@link com.vaadin.flow.spring.security.VaadinSecurityConfigurer},
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
     * @since 24.3
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
     * @since 24.3
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
     * @since 24.3
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessPathChecker accessPatchChecker(
            VaadinConfigurationProperties vaadinProperties,
            @Lazy WebInvocationPrivilegeEvaluator evaluator) {
        return new SpringAccessPathChecker(evaluator,
                vaadinProperties.getUrlMapping(),
                securityContextHolderStrategy.getObject(),
                this::getFilterChainProxy, privilegeEvaluatorRequestTransformer
                        .getIfUnique(PathPatternRequestTransformer::new));
    }

    /**
     * Gets the filter chain proxy behind the {@code springSecurityFilterChain}
     * bean. The bean is declared as a plain {@link Filter}: it is the proxy
     * itself, or a {@link DebugFilter} around it when security debugging is on.
     * It is looked up on first use, like the privilege evaluator, because the
     * filter chains depend on the navigation access control this configuration
     * creates.
     *
     * @return the filter chain proxy, or {@literal null} if the bean is of an
     *         unknown type
     */
    private FilterChainProxy getFilterChainProxy() {
        Filter filter = beanFactory.getBean(
                AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME,
                Filter.class);
        if (filter instanceof DebugFilter debugFilter) {
            return debugFilter.getFilterChainProxy();
        }
        return filter instanceof FilterChainProxy proxy ? proxy : null;
    }

    /**
     * Makes the default access annotation checker available for security
     * configuration.
     * <p>
     * Fusion makes this bean available by default but if Fusion is excluded
     * from the project, we make it available here
     *
     * @return the default access annotation checker
     * @since 18.0
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
     * @since 18.0
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
     * @since 24.3
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
        return new AuthenticationContext(
                securityContextHolderStrategy.getObject());
    }

    @Bean
    @ConditionalOnMissingBean
    SecurityContextHolderStrategy vaadinAwareSecurityContextHolderStrategy() {
        return new VaadinAwareSecurityContextHolderStrategy();
    }

    @Bean
    SmartInitializingSingleton securityContextHolderStrategyInitializer(
            SecurityContextHolderStrategy strategy) {
        return () -> SecurityContextHolder.setContextHolderStrategy(strategy);
    }
}
