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

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AuthorizationManagerWebInvocationPrivilegeEvaluator.HttpServletRequestTransformer;
import org.springframework.security.web.access.PathPatternRequestTransformer;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import com.vaadin.flow.spring.AuthenticationUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitWebConfig
@ContextConfiguration(classes = SpringAccessPathCheckerTest.TestConfig.class)
class SpringAccessPathCheckerTest {

    @Autowired
    private SpringAccessPathChecker accessPathChecker;

    @Autowired
    private WebInvocationPrivilegeEvaluator evaluator;

    @Autowired
    @Qualifier(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME)
    private Filter springSecurityFilterChain;

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

    @Test
    @WithAnonymousUser
    void checkAccess_strategyGiven_authenticationReadFromStrategy() {
        var admin = new UsernamePasswordAuthenticationToken("admin", null,
                AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        SecurityContextHolderStrategy strategy = mock(
                SecurityContextHolderStrategy.class);
        when(strategy.getContext()).thenReturn(new SecurityContextImpl(admin));
        var checker = new SpringAccessPathChecker(evaluator, null, strategy);

        assertTrue(checker.hasAccess("admin", admin, role -> false),
                "Access to admin only path should be allowed to admin user from the given strategy");
        assertFalse(checker.hasAccess("anon", admin, role -> false),
                "Access to anonymous only path should not be allowed to admin user from the given strategy");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkAccess_nullPrincipal_evaluatedAsAnonymous() {
        assertFalse(accessPathChecker.hasAccess("admin", null, role -> false),
                "Access to admin only path should not be allowed when principal is null");
        assertTrue(accessPathChecker.hasAccess("anon", null, role -> false),
                "Access to anonymous only path should be allowed when principal is null");
    }

    @Test
    void checkAccess_nullPrincipalWithoutAuthentication_evaluatedAsAnonymous() {
        SecurityContextHolder.clearContext();
        assertTrue(accessPathChecker.hasAccess("anon", null, role -> false),
                "Access to anonymous only path should be allowed when principal is null and there is no authentication");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkAccess_nullPrincipalWithFilterChainProxy_configuredAnonymousAuthoritiesApplied() {
        var checker = createCheckerWithFilterChainProxy(
                new PathPatternRequestTransformer());

        assertTrue(checker.hasAccess("guest", null, role -> false),
                "Access to guest only path should be allowed when principal is null and the filter chain gives GUEST to anonymous users");
        assertFalse(checker.hasAccess("admin", null, role -> false),
                "Access to admin only path should not be allowed when principal is null, even if an admin user is on the thread");
        assertTrue(checker.hasAccess("api", null, role -> false),
                "Access to api path should be allowed when principal is null and the matching api filter chain gives API to anonymous users");
        assertTrue(checker.hasAccess("empty", null, role -> false),
                "Access to empty path should be allowed with the default anonymous authentication when the matching filter chain gives no authorities to anonymous users");
    }

    @Test
    @WithAnonymousUser
    void checkAccess_nullPrincipalWithFilterChainProxy_threadAnonymousAuthenticationReplaced() {
        var checker = createCheckerWithFilterChainProxy(
                new PathPatternRequestTransformer());

        assertTrue(checker.hasAccess("guest", null, role -> false),
                "Access to guest only path should be allowed with the anonymous authentication of the matching filter chain, not with the one on the thread");
        assertTrue(checker.hasAccess("api", null, role -> false),
                "Access to api path should be allowed with the anonymous authentication of the matching filter chain, not with the one on the thread");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkAccess_nullPrincipalWithFilterChainProxy_requestTransformerApplied() {
        // Matches every path against the api chain, which gives API to
        // anonymous users: the guest path is then checked with that
        // authentication and denied
        HttpServletRequestTransformer toApiPath = request -> new PathPatternRequestTransformer()
                .transform(new HttpServletRequestWrapper(request) {
                    @Override
                    public String getRequestURI() {
                        return getContextPath() + "/api/any";
                    }

                    @Override
                    public String getServletPath() {
                        return "/api/any";
                    }
                });
        var checker = createCheckerWithFilterChainProxy(toApiPath);

        assertFalse(checker.hasAccess("guest", null, role -> false),
                "Access to guest only path should not be allowed when the transformed request selects the api filter chain");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkAccess_nullPrincipalWithFilterChainProxy_proxyResolvedOnce() {
        var lookups = new AtomicInteger();
        var checker = new SpringAccessPathChecker(evaluator, null,
                SecurityContextHolder.getContextHolderStrategy(), () -> {
                    lookups.incrementAndGet();
                    return (FilterChainProxy) springSecurityFilterChain;
                }, new PathPatternRequestTransformer());

        assertTrue(checker.hasAccess("guest", null, role -> false));
        assertTrue(checker.hasAccess("api", null, role -> false));
        assertEquals(1, lookups.get(),
                "The filter chain proxy should be resolved once");
    }

    private SpringAccessPathChecker createCheckerWithFilterChainProxy(
            HttpServletRequestTransformer requestTransformer) {
        return new SpringAccessPathChecker(evaluator, null,
                SecurityContextHolder.getContextHolderStrategy(),
                () -> (FilterChainProxy) springSecurityFilterChain,
                requestTransformer);
    }

    @Test
    void checkAccess_nullPrincipal_customAnonymousAuthenticationPreserved() {
        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken("key",
                        "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_GUEST")));
        try {
            assertTrue(
                    accessPathChecker.hasAccess("guest", null, role -> false),
                    "Access to guest only path should be allowed to anonymous user with GUEST role");
        } finally {
            SecurityContextHolder.clearContext();
        }
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
        SpringAccessPathChecker rootPathAccessChecker(
                WebInvocationPrivilegeEvaluator evaluator) {
            return new SpringAccessPathChecker(evaluator);
        }

        @Bean
        @Order(1)
        public SecurityFilterChain apiFilterChain(HttpSecurity http) {
            var matcherBuilder = PathPatternRequestMatcher.withDefaults();
            http.securityMatcher(matcherBuilder.matcher("/api/**"))
                    .anonymous(anonymous -> anonymous.authorities("ROLE_API"))
                    .authorizeHttpRequests(cfg -> cfg
                            .requestMatchers(matcherBuilder.matcher("/api/**"))
                            .hasRole("API"));
            return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain emptyAnonymousFilterChain(
                HttpSecurity http) {
            var matcherBuilder = PathPatternRequestMatcher.withDefaults();
            http.securityMatcher(matcherBuilder.matcher("/empty/**"))
                    .anonymous(anonymous -> anonymous.authorities(List.of()))
                    .authorizeHttpRequests(cfg -> cfg
                            .requestMatchers(
                                    matcherBuilder.matcher("/empty/**"))
                            .hasRole("ANONYMOUS"));
            return http.build();
        }

        @Bean
        @Order(3)
        public SecurityFilterChain testingFilterChain(HttpSecurity http) {
            // @formatter:off
            var matcherBuilder = PathPatternRequestMatcher.withDefaults();
            http.anonymous(anonymous -> anonymous.authorities("ROLE_GUEST"))
                .authorizeHttpRequests(cfg -> cfg
                    .requestMatchers(matcherBuilder.matcher("/")).anonymous()
                    .requestMatchers(matcherBuilder.matcher("/admin/**")).hasRole("ADMIN")
                    .requestMatchers(matcherBuilder.matcher("/guest/**")).hasRole("GUEST")
                    .requestMatchers(matcherBuilder.matcher("/protected/**")).authenticated()
                    .requestMatchers(matcherBuilder.matcher("/anon/**")).anonymous()
                    .requestMatchers(matcherBuilder.matcher("/public/**")).permitAll()
                    .requestMatchers(matcherBuilder.matcher("/forbidden/**")).denyAll());
           // @formatter:on
            return http.build();
        }
    }

}
