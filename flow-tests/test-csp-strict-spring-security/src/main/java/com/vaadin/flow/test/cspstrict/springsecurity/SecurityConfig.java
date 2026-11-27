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
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

/**
 * A Vaadin security configuration where the Content Security Policy of the
 * application is configured in Spring Security, which is where an application
 * with Spring Security configures it for
 * {@code vaadin.contentSecurityPolicy=strict}.
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    /**
     * The policy Spring Security is configured with. It is the policy Vaadin
     * sends in the {@code strict} mode, with the {@code {nonce}} placeholder
     * that Spring Security replaces with the nonce of the request, and with
     * {@code form-action}, which only this policy has.
     */
    public static final String SPRING_SECURITY_POLICY = "default-src 'self'; "
            + "script-src 'nonce-{nonce}' 'strict-dynamic'; "
            + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; "
            + "font-src 'self' data:; connect-src 'self' data:; "
            + "worker-src 'self'; object-src 'none'; base-uri 'self'; "
            + "form-action 'self'";

    /**
     * A path that the policy of Spring Security is not sent for. Spring
     * Security decides where its policy applies, so a Vaadin page of this path
     * is sent without a policy.
     */
    public static final String PATH_WITHOUT_SPRING_SECURITY_POLICY = "no-spring-security-policy";

    @Bean
    SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http) {
        http.headers(headers -> headers.contentSecurityPolicy(csp -> csp
                .policyDirectives(SPRING_SECURITY_POLICY)
                .requestMatcher(new NegatedRequestMatcher(
                        PathPatternRequestMatcher.withDefaults().matcher(
                                "/" + PATH_WITHOUT_SPRING_SECURITY_POLICY)))));
        http.with(vaadin(), cfg -> {
        });
        return http.build();
    }
}
