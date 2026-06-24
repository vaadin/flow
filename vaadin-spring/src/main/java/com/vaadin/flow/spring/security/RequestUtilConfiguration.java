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
package com.vaadin.flow.spring.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes {@link RequestUtil} as a bean for applications that discover Vaadin
 * Spring beans through classpath scanning of the {@code com.vaadin.flow.spring}
 * package rather than through Spring Boot auto-configuration.
 * <p>
 * In a Spring Boot application the bean is provided by
 * {@code SpringSecurityAutoConfiguration}, which is loaded through the
 * auto-configuration mechanism and is therefore not visible to a regular
 * component scan. This configuration fills that gap, for instance for a plain
 * Spring MVC application that registers the {@code SpringServlet} itself. The
 * bean definition is conditional so that only one {@link RequestUtil} is
 * created when both this configuration and the auto-configuration are present.
 */
@Configuration(proxyBeanMethods = false)
public class RequestUtilConfiguration {

    /**
     * Makes the request util available.
     *
     * @return the request util
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestUtil requestUtil() {
        return new RequestUtil();
    }
}
