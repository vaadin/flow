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
package com.vaadin.flow.test.cspstrict.springsecurity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

/**
 * A Vaadin security configuration where Spring Security is also told to send a
 * Content Security Policy, as an application may have done before it turned on
 * {@code vaadin.csp=strict}.
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    /**
     * The policy Spring Security is configured with. {@code form-action} is in
     * no other policy of this application, so a policy that has it comes from
     * Spring Security.
     */
    public static final String SPRING_SECURITY_POLICY = "default-src 'self'; form-action 'self'";

    @Bean
    SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http) {
        http.headers(headers -> headers.contentSecurityPolicy(
                csp -> csp.policyDirectives(SPRING_SECURITY_POLICY)));
        http.with(vaadin(), cfg -> {
        });
        return http.build();
    }
}
