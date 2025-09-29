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

import java.security.Principal;
import java.util.function.Function;

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
import org.springframework.security.web.access.AuthorizationManagerWebInvocationPrivilegeEvaluator;
import org.springframework.security.web.access.PathPatternRequestTransformer;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import com.vaadin.flow.spring.AuthenticationUtil;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitWebConfig
@ContextConfiguration(classes = UrlMappingSpringAccessPathCheckerTest.TestConfig.class)
class UrlMappingSpringAccessPathCheckerTest {

    @Autowired
    private SpringAccessPathChecker accessPathChecker;

    @Test
    @WithAnonymousUser
    void checkAccess_anonymous() {
        assertFalse(checkAccess("admin"),
                "Access to admin only path should not be allowed to anonymous user");
        assertFalse(checkAccess("guest"),
                "Access to guest only path should not be allowed to anonymous user");
        assertFalse(checkAccess("protected"),
                "Access to protected path should not be allowed to anonymous user");
        assertFalse(checkAccess("forbidden"),
                "Access to deny all path should not be allowed to anonymous user");
        assertTrue(checkAccess("anon"),
                "Access to anonymous only path should be allowed to anonymous user");
        assertTrue(checkAccess(""),
                "Access to anonymous only root path should be allowed to anonymous user");
        assertTrue(checkAccess("/"),
                "Access to anonymous only root path ('/') should be allowed to anonymous user");
        assertTrue(checkAccess("public"),
                "Access to public path should be allowed to anonymous user");
        assertFalse(checkAccess("not-defined-path"),
                "Access to not defined path should not be allowed to anonymous user");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkAccess_adminOnly() {
        assertTrue(checkAccess("admin"),
                "Access to admin only path should be allowed to admin user");
        assertFalse(checkAccess("guest"),
                "Access to guest only path should not be allowed to admin user");
        assertTrue(checkAccess("protected"),
                "Access to protected path should be allowed to admin user");
        assertFalse(checkAccess("forbidden"),
                "Access to deny all path should not be allowed to admin user");
        assertFalse(checkAccess("anon"),
                "Access to anonymous only path should not be allowed to admin user");
        assertFalse(checkAccess(""),
                "Access to anonymous only root path should not be allowed to admin user");
        assertFalse(checkAccess("/"),
                "Access to anonymous only root path ('/') should not be allowed to admin user");
        assertTrue(checkAccess("public"),
                "Access to public path should be allowed to admin user");
        assertFalse(checkAccess("not-defined-path"),
                "Access to not defined path should not be allowed to admin user");
    }

    @Test
    @WithMockUser(roles = "GUEST")
    void checkAccess_guestOnly() {
        assertFalse(checkAccess("admin"),
                "Access to admin only path should not be allowed to guest user");
        assertTrue(checkAccess("guest"),
                "Access to guest only path should be allowed to guest user");
        assertTrue(checkAccess("protected"),
                "Access to protected path should be allowed to guest user");
        assertFalse(checkAccess("forbidden"),
                "Access to deny all path should not be allowed to guest user");
        assertFalse(checkAccess("anon"),
                "Access to anonymous only path should not be allowed to guest user");
        assertFalse(checkAccess(""),
                "Access to anonymous only root path should not be allowed to guest user");
        assertFalse(checkAccess("/"),
                "Access to anonymous only root path ('/') should not be allowed to guest user");
        assertTrue(checkAccess("public"),
                "Access to public path should be allowed to guest user");
        assertFalse(checkAccess("not-defined-path"),
                "Access to not defined path should not be allowed to guest user");
    }

    @Test
    @WithMockUser(roles = { "ADMIN", "GUEST" })
    void checkAccess_adminAndGuest() {
        assertTrue(checkAccess("admin"),
                "Access to admin only path should be allowed to user with ADMIN and GUEST roles");
        assertTrue(checkAccess("guest"),
                "Access to guest only path should be allowed to user with ADMIN and GUEST roles");
        assertTrue(checkAccess("protected"),
                "Access to protected path should be allowed to user with ADMIN and GUEST roles");
        assertFalse(checkAccess("forbidden"),
                "Access to deny all path should not be allowed to user with ADMIN and GUEST roles");
        assertFalse(checkAccess("anon"),
                "Access to anonymous only path should not be allowed to user with ADMIN and GUEST roles");
        assertFalse(checkAccess(""),
                "Access to anonymous only root path should not be allowed to user with ADMIN and GUEST roles");
        assertFalse(checkAccess("/"),
                "Access to anonymous only root path ('/') should not be allowed to user with ADMIN and GUEST roles");
        assertTrue(checkAccess("public"),
                "Access to public path should be allowed to user with ADMIN and GUEST roles");
        assertFalse(checkAccess("not-defined-path"),
                "Access to not defined path should not be allowed to user with ADMIN and GUEST roles");
    }

    private boolean checkAccess(String admin) {
        Principal principal = SecurityContextHolder.getContext()
                .getAuthentication();
        Function<String, Boolean> roleChecker = AuthenticationUtil
                .getSecurityHolderRoleChecker();
        return accessPathChecker.hasAccess(admin, principal,
                roleChecker::apply);
    }

    @Configuration
    @EnableWebSecurity
    public static class TestConfig {

        @Bean
        SpringAccessPathChecker urlMappingPpathAccessChecker(
                WebInvocationPrivilegeEvaluator evaluator) {
            return new SpringAccessPathChecker(evaluator, "/url-mapping/*");
        }

        @Bean
        AuthorizationManagerWebInvocationPrivilegeEvaluator.HttpServletRequestTransformer httpServletRequestTransformer() {
            return new PathPatternRequestTransformer();
        }

        @Bean
        public SecurityFilterChain testingFilterChain(HttpSecurity http)
                throws Exception {
            // @formatter:off
            var matcherBuilder = PathPatternRequestMatcher.withDefaults();
            http.authorizeHttpRequests(cfg -> cfg
                    .requestMatchers(matcherBuilder.matcher("/url-mapping/")).anonymous()
                    .requestMatchers(matcherBuilder.matcher("/url-mapping/admin/**")).hasRole("ADMIN")
                    .requestMatchers(matcherBuilder.matcher("/url-mapping/guest/**")).hasRole("GUEST")
                    .requestMatchers(matcherBuilder.matcher("/url-mapping/protected/**")).authenticated()
                    .requestMatchers(matcherBuilder.matcher("/url-mapping/anon/**")).anonymous()
                    .requestMatchers(matcherBuilder.matcher("/url-mapping/public/**")).permitAll()
                    .requestMatchers(matcherBuilder.matcher("/url-mapping/forbidden/**")).denyAll());
            // @formatter:on
            return http.build();
        }
    }

}