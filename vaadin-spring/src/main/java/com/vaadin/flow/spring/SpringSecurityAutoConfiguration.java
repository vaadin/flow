/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.ViewAccessChecker;
import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import com.vaadin.flow.spring.security.ViewAccessCheckerInitializer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Spring boot auto-configuration class for Flow.
 *
 * @author Vaadin Ltd
 *
 */
@Configuration
@ConditionalOnClass(WebSecurityConfigurerAdapter.class)
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
     * Makes the default view access check initializer available for security
     * configuration.
     *
     * @return the default access check initializer
     */
    @Bean
    public ViewAccessCheckerInitializer viewAccessCheckerInitializer() {
        return new ViewAccessCheckerInitializer();
    }

    /**
     * Makes the default view access checker available for security
     * configuration.
     *
     * @return the default view access checker
     */
    @Bean
    public ViewAccessChecker viewAccessChecker(
            AccessAnnotationChecker accessAnnotationChecker) {
        return new SpringViewAccessChecker(accessAnnotationChecker);
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
}
