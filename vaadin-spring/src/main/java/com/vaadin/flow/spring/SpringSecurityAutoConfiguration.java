/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Spring boot auto-configuration class for Flow.
 *
 * @author Vaadin Ltd
 *
 */
@Configuration
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
