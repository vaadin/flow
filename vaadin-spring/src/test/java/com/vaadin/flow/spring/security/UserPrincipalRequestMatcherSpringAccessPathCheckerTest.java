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

import com.vaadin.flow.spring.AuthenticationUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AuthorizationManagerWebInvocationPrivilegeEvaluator.HttpServletRequestTransformer;
import org.springframework.security.web.access.PathPatternRequestTransformer;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import java.security.Principal;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitWebConfig
@ContextConfiguration(classes = UserPrincipalRequestMatcherSpringAccessPathCheckerTest.TestConfig.class)
class UserPrincipalRequestMatcherSpringAccessPathCheckerTest {

    @Autowired
    private SpringAccessPathChecker accessPathChecker;

    @Test
    @WithMockUser(roles = "GUEST")
    void checkAccess_user_requestMatchersCanAccessUserPrincipal() {
        assertTrue(checkAccess("path"),
                "Access allowed to authenticated users");
    }

    @Test
    @WithAnonymousUser
    void checkAccess_anonymous_requestMatchersCanAccessUserPrincipal() {
        assertFalse(checkAccess("path"), "Access denied to anonymous users");
    }

    private boolean checkAccess(String path) {
        Principal principal = SecurityContextHolder.getContext()
                .getAuthentication();
        Function<String, Boolean> roleChecker = AuthenticationUtil
                .getSecurityHolderRoleChecker();
        return accessPathChecker.hasAccess(path, principal, roleChecker::apply);
    }

    @Configuration
    @EnableWebSecurity
    public static class TestConfig {

        @Bean
        SpringAccessPathChecker urlMappingPathAccessChecker(
                WebInvocationPrivilegeEvaluator evaluator) {
            return new SpringAccessPathChecker(evaluator);
        }

        /**
         * Provides a custom {@link HttpServletRequestTransformer} to be used by
         * {@link SpringAccessPathChecker}.
         * <p>
         * The aim is to test integration of the transformer security-aware
         * request transformer provided by {@link RequestUtil} with Spring
         * Security defaults.
         *
         * @return a configured {@link HttpServletRequestTransformer} instance
         */
        @Bean
        HttpServletRequestTransformer httpServletRequestTransformer() {
            HttpServletRequestTransformer transformer = new PathPatternRequestTransformer();
            transformer = SpringAccessPathChecker
                    .principalAwareRequestTransformer(transformer);
            return transformer;
        }

        @Bean
        SecurityFilterChain testingFilterChain(HttpSecurity http)
                throws Exception {
            http.authorizeHttpRequests(cfg -> cfg
                    .requestMatchers(new UserPrincipalRequestMatcher())
                    .authenticated());
            return http.build();
        }
    }

    static class UserPrincipalRequestMatcher implements RequestMatcher {

        private boolean enabled;

        @Override
        public boolean matches(HttpServletRequest request) {
            // Throws unsupported operation exception if a request transformer
            // is not set
            Principal userPrincipal = request.getUserPrincipal();
            return userPrincipal != null && userPrincipal.getName() != null;
        }
    }

}
