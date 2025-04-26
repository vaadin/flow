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

import java.io.Serializable;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Functional interface for customizing the Spring Security {@link HttpSecurity}
 * instance specifically for use with Vaadin applications.
 * <p>
 * Implementations of this interface can provide additional configuration or
 * adjustments to the security setup that integrates Vaadin with Spring
 * Security.
 * <p>
 * Beans implementing this interface will be automatically discovered and
 * applied to the filter chain.
 * <p>
 * For example:
 *
 * <pre>
 * <code>
 * &#64;Component
 * public class MyCustomizer implements VaadinWebSecurityCustomizer {
 *
 *     &#64;Override
 *     public void customize(HttpSecurity http) {
 *         http.addFilter(MyCustomFilter.class);
 *     }
 * }
 * </code>
 * </pre>
 */
@FunctionalInterface
public interface VaadinWebSecurityCustomizer extends Serializable {

    /**
     * Customizes the provided {@link HttpSecurity} instance with specific
     * security configurations for integrating Vaadin applications with Spring
     * Security. This method enables implementations to adjust or enhance the
     * default security setup as needed.
     *
     * @param http
     *            the {@link HttpSecurity} instance to be customized
     */
    void customize(HttpSecurity http);
}
