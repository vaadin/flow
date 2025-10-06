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

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.configuration.ObjectPostProcessorConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.internal.hilla.FileRouterRequestUtil;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@WebAppConfiguration
@ContextConfiguration(classes = { SpringBootAutoConfiguration.class,
        SpringSecurityAutoConfiguration.class,
        ObjectPostProcessorConfiguration.class,
        VaadinSecurityConfigurerTest.TestConfig.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
class VaadinSecurityConfigurerTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectPostProcessor<Object> postProcessor;

    @Autowired
    private PathPatternRequestMatcher.Builder requestMatcherBuilder;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private HttpSecurity http;

    private SecurityContextHolderStrategy securityContextHolderStrategy;

    private VaadinSecurityConfigurer configurer;

    @MockitoBean
    private EndpointRequestUtil endpointRequestUtil;

    @MockitoBean
    private FileRouterRequestUtil fileRouterRequestUtil;

    @BeforeEach
    void setUp() {
        var authManagerBuilder = new AuthenticationManagerBuilder(postProcessor)
                .authenticationProvider(new TestingAuthenticationProvider());
        securityContextHolderStrategy = new VaadinAwareSecurityContextHolderStrategy();
        http = new HttpSecurity(postProcessor, authManagerBuilder,
                Map.of(ApplicationContext.class, applicationContext,
                        PathPatternRequestMatcher.Builder.class,
                        requestMatcherBuilder,
                        SecurityContextHolderStrategy.class,
                        securityContextHolderStrategy));
        configurer = VaadinSecurityConfigurer.vaadin();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void withDefaults_chainHasDefaultFilters() throws Exception {
        var filters = http.with(configurer, Customizer.withDefaults()).build()
                .getFilters();

        assertThat(filters).hasOnlyElementsOfTypes(CsrfFilter.class,
                LogoutFilter.class, AuthorizationFilter.class,
                RequestCacheAwareFilter.class,
                ExceptionTranslationFilter.class);
    }

    @Test
    void loginViewClass_chainHasAuthenticationFilter() throws Exception {
        var filters = http.with(configurer, c -> {
            c.loginView(TestLoginView.class);
        }).build().getFilters();

        assertThat(filters).hasAtLeastOneElementOfType(
                UsernamePasswordAuthenticationFilter.class);
    }

    @Test
    void loginViewString_chainHasAuthenticationFilter() throws Exception {
        var filters = http.with(configurer, c -> {
            c.loginView("/login");
        }).build().getFilters();

        assertThat(filters).hasAtLeastOneElementOfType(
                UsernamePasswordAuthenticationFilter.class);
    }

    @Test
    void oauth2LoginPage_chainHasAuthenticationFilter() throws Exception {
        var filters = http.with(configurer, c -> {
            c.oauth2LoginPage("/oauth2/login");
        }).build().getFilters();

        assertThat(filters).hasAtLeastOneElementOfType(
                OAuth2LoginAuthenticationFilter.class);
    }

    @Test
    void logoutSuccessHandler_handlerIsConfigured(
            @Mock LogoutSuccessHandler handler) throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("user", "password");
        securityContextHolderStrategy.getContext().setAuthentication(auth);
        var request = new MockHttpServletRequest("POST", "/logout");
        request.setPathInfo("/logout");

        var filters = http.with(configurer, c -> {
            c.logoutSuccessHandler(handler);
        }).build().getFilters();

        assertThat(filters).filteredOn(LogoutFilter.class::isInstance)
                .singleElement().satisfies(filter -> {
                    filter.doFilter(request, response, chain);
                    verify(handler).onLogoutSuccess(request, response, auth);
                });
    }

    @Test
    void addLogoutHandler_handlerIsAdded(@Mock LogoutHandler handler)
            throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("user", "password");
        securityContextHolderStrategy.getContext().setAuthentication(auth);
        var request = new MockHttpServletRequest("POST", "/logout");
        request.setPathInfo("/logout");

        var filters = http.with(configurer, c -> {
            c.addLogoutHandler(handler);
        }).build().getFilters();

        assertThat(filters).filteredOn(LogoutFilter.class::isInstance)
                .singleElement().satisfies(filter -> {
                    filter.doFilter(request, response, chain);
                    verify(handler).logout(request, response, auth);
                });
    }

    @Test
    void anyRequest_authorizeRuleIsConfigured() throws Exception {
        var auth = new AnonymousAuthenticationToken("key", "user",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        securityContextHolderStrategy.getContext().setAuthentication(auth);
        var request = new MockHttpServletRequest("GET", "/any");
        request.setPathInfo("/any");

        var filters = http.with(configurer, c -> {
            c.anyRequest(AuthorizedUrl::anonymous);
        }).build().getFilters();

        assertThat(filters).filteredOn(AuthorizationFilter.class::isInstance)
                .singleElement()
                .satisfies(filter -> assertThatCode(
                        () -> filter.doFilter(request, response, chain))
                        .doesNotThrowAnyException());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void enableNavigationAccessControl_navigationAccessControlIsConfigured(
            boolean enableNavigationAccessControl) throws Exception {
        http.with(configurer, c -> {
            c.enableNavigationAccessControl(enableNavigationAccessControl);
        }).build();

        var nac = http.getSharedObject(NavigationAccessControl.class);
        assertThat(nac.isEnabled()).isEqualTo(enableNavigationAccessControl);
    }

    @Test
    @SuppressWarnings("unchecked")
    void disableDefaultConfigurers_configurersAreNotApplied() throws Exception {
        http.with(configurer, c -> {
            c.enableCsrfConfiguration(false);
            c.enableLogoutConfiguration(false);
            c.enableRequestCacheConfiguration(false);
            c.enableExceptionHandlingConfiguration(false);
            c.enableAuthorizedRequestsConfiguration(false);
        }).build();

        assertThat(http.getConfigurer(CsrfConfigurer.class)).isNull();
        assertThat(http.getConfigurer(LogoutConfigurer.class)).isNull();
        assertThat(http.getConfigurer(RequestCacheConfigurer.class)).isNull();
        assertThat(http.getConfigurer(ExceptionHandlingConfigurer.class))
                .isNull();
        assertThat(http.getConfigurer(AuthorizeHttpRequestsConfigurer.class))
                .isNull();
    }

    @Test
    void requestCache_customRulesAreApplied() throws Exception {
        VaadinDefaultRequestCache requestCache = applicationContext
                .getBean(VaadinDefaultRequestCache.class);
        requestCache.ignoreRequests(PathPatternRequestMatcher.withDefaults()
                .matcher("/.my-path/**"));

        http.with(configurer, Customizer.withDefaults()).build();

        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/.my-path/foo");
        request.setPathInfo("/.my-path/foo");
        requestCache.saveRequest(request, response);
        assertNull(requestCache.getRequest(request, response),
                "Request should not have been saved");
    }

    @Test
    void loginView_requestCacheApplied() throws Exception {
        VaadinDefaultRequestCache requestCache = applicationContext
                .getBean(VaadinDefaultRequestCache.class);

        var mockSuccessHandler = Mockito.mock(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class);
        http.setSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class,
                mockSuccessHandler);

        http.with(configurer, c -> {
            c.loginView("/login");
        }).build();

        Mockito.verify(mockSuccessHandler, times(1))
                .setRequestCache(Mockito.eq(requestCache));
    }

    @Test
    void hillaAnonymousEndpointRequest_arePermitted() throws Exception {
        try (MockedStatic<EndpointRequestUtil> endpointRequestUtilMockedStatic = Mockito
                .mockStatic(EndpointRequestUtil.class)) {
            endpointRequestUtilMockedStatic
                    .when(EndpointRequestUtil::isHillaAvailable)
                    .thenReturn(true);

            var auth = new AnonymousAuthenticationToken("key", "user",
                    List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
            securityContextHolderStrategy.getContext().setAuthentication(auth);
            var path = "/connect/HillaEndpoint/anonymous";
            var request = new MockHttpServletRequest("POST", path);
            request.setPathInfo(path);

            Mockito.when(endpointRequestUtil.isAnonymousEndpoint(request))
                    .thenReturn(true);

            var filters = http.with(configurer, Customizer.withDefaults())
                    .build().getFilters();

            assertThat(filters)
                    .filteredOn(AuthorizationFilter.class::isInstance)
                    .singleElement()
                    .satisfies(filter -> assertThatCode(
                            () -> filter.doFilter(request, response, chain))
                            .doesNotThrowAnyException());
        }
    }

    @Test
    void hillaEndpointRequest_areAuthenticated() throws Exception {
        try (MockedStatic<EndpointRequestUtil> endpointRequestUtilMockedStatic = Mockito
                .mockStatic(EndpointRequestUtil.class)) {
            endpointRequestUtilMockedStatic
                    .when(EndpointRequestUtil::isHillaAvailable)
                    .thenReturn(true);

            var auth = new TestingAuthenticationToken("user", "password",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            securityContextHolderStrategy.getContext().setAuthentication(auth);
            var path = "/connect/HillaEndpoint/authenticated";
            var request = new MockHttpServletRequest("POST", path);
            request.setPathInfo(path);

            Mockito.when(endpointRequestUtil.isEndpointRequest(request))
                    .thenReturn(true);

            var filters = http.with(configurer, Customizer.withDefaults())
                    .build().getFilters();

            assertThat(filters)
                    .filteredOn(AuthorizationFilter.class::isInstance)
                    .singleElement()
                    .satisfies(filter -> assertThatCode(
                            () -> filter.doFilter(request, response, chain))
                            .doesNotThrowAnyException());
        }
    }

    @Test
    void hilla_checkAllowedRoutes() throws Exception {
        try (MockedStatic<EndpointRequestUtil> endpointRequestUtilMockedStatic = Mockito
                .mockStatic(EndpointRequestUtil.class)) {
            endpointRequestUtilMockedStatic
                    .when(EndpointRequestUtil::isHillaAvailable)
                    .thenReturn(true);

            var auth = new TestingAuthenticationToken("user", "password",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            securityContextHolderStrategy.getContext().setAuthentication(auth);
            var path = "/hilla-view";
            var request = new MockHttpServletRequest("POST", path);
            request.setPathInfo(path);

            // Simulate usage of Hilla API, accessing request principal
            Mockito.when(fileRouterRequestUtil.isRouteAllowed(request))
                    .thenReturn(true);

            var filters = http.with(configurer, Customizer.withDefaults())
                    .build().getFilters();

            assertThat(filters)
                    .filteredOn(AuthorizationFilter.class::isInstance)
                    .singleElement()
                    .satisfies(filter -> assertThatCode(
                            () -> filter.doFilter(request, response, chain))
                            .doesNotThrowAnyException());
        }
    }

    @Route
    static class TestLoginView extends Component {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        PathPatternRequestMatcher.Builder pathMatcherBuilder() {
            return PathPatternRequestMatcher.withDefaults();
        }
    }
}
