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
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Method;
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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AbstractAuthenticationTargetUrlRequestHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.security.web.session.ConcurrentSessionFilter;
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
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    private VaadinSecurityConfigurer configurer;

    @MockitoBean
    private EndpointRequestUtil endpointRequestUtil;

    @MockitoBean
    private FileRouterRequestUtil fileRouterRequestUtil;

    @BeforeEach
    void setUp() {
        var authManagerBuilder = new AuthenticationManagerBuilder(postProcessor)
                .authenticationProvider(new TestingAuthenticationProvider());
        http = new HttpSecurity(postProcessor, authManagerBuilder,
                Map.of(ApplicationContext.class, applicationContext,
                        PathPatternRequestMatcher.Builder.class,
                        requestMatcherBuilder));
        configurer = VaadinSecurityConfigurer.vaadin();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void withDefaults_chainHasDefaultFilters() {
        var filters = http.with(configurer, Customizer.withDefaults()).build()
                .getFilters();

        assertThat(filters).hasOnlyElementsOfTypes(CsrfFilter.class,
                LogoutFilter.class, AuthorizationFilter.class,
                RequestCacheAwareFilter.class,
                ExceptionTranslationFilter.class);
    }

    @Test
    void loginViewClass_chainHasAuthenticationFilter() {
        var filters = http.with(configurer, c -> {
            c.loginView(TestLoginView.class);
        }).build().getFilters();

        assertThat(filters).hasAtLeastOneElementOfType(
                UsernamePasswordAuthenticationFilter.class);
    }

    @Test
    void loginViewString_chainHasAuthenticationFilter() {
        var filters = http.with(configurer, c -> {
            c.loginView("/login");
        }).build().getFilters();

        assertThat(filters).hasAtLeastOneElementOfType(
                UsernamePasswordAuthenticationFilter.class);
    }

    @Test
    void oauth2LoginPage_chainHasAuthenticationFilter() {
        var filters = http.with(configurer, c -> {
            c.oauth2LoginPage("/oauth2/login");
        }).build().getFilters();

        assertThat(filters).hasAtLeastOneElementOfType(
                OAuth2LoginAuthenticationFilter.class);
    }

    @Test
    void keycloakRoleMapping_withOAuth2LoginPage_oidcUserServiceMapsRoles()
            throws Exception {
        http.with(configurer,
                c -> c.oauth2LoginPage("/oauth2/authorization/keycloak")
                        .keycloakRoleMapping())
                .build();

        var oidcUserService = http.getSharedObject(OidcUserService.class);

        assertThat(oidcUserService).isNotNull();
        assertThat(getOidcUserConverter(oidcUserService))
                .isInstanceOf(KeycloakOidcUserMapper.class);
    }

    @Test
    void keycloakRoleMapping_withoutOAuth2LoginPage_notConfigured() {
        http.with(configurer, VaadinSecurityConfigurer::keycloakRoleMapping)
                .build();

        assertNull(http.getSharedObject(OidcUserService.class));
    }

    @Test
    void keycloakRoleMapping_rolePrefixOfChain_isUsedForRoles()
            throws Exception {
        var rolePrefixHolder = new VaadinRolePrefixHolder(null);
        http.setSharedObject(VaadinRolePrefixHolder.class, rolePrefixHolder);

        http.with(configurer,
                c -> c.oauth2LoginPage("/oauth2/authorization/keycloak")
                        .keycloakRoleMapping())
                .build();
        // The prefix of the filter chain is only known to the holder after the
        // chain has been configured, so the mapper must pick it up afterwards
        var securityContextFilter = new SecurityContextHolderAwareRequestFilter();
        securityContextFilter.setRolePrefix("AUTHORITY_");
        rolePrefixHolder.resetRolePrefix(securityContextFilter);

        var mapper = (KeycloakOidcUserMapper) getOidcUserConverter(
                http.getSharedObject(OidcUserService.class));

        assertThat(mapper.rolePrefix()).isEqualTo("AUTHORITY_");
    }

    @Test
    void logoutSuccessHandler_handlerIsConfigured(
            @Mock LogoutSuccessHandler handler) {
        var auth = new UsernamePasswordAuthenticationToken("user", "password");
        SecurityContextHolder.getContext().setAuthentication(auth);
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
    void addLogoutHandler_handlerIsAdded(@Mock LogoutHandler handler) {
        var auth = new UsernamePasswordAuthenticationToken("user", "password");
        SecurityContextHolder.getContext().setAuthentication(auth);
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
    void anyRequest_authorizeRuleIsConfigured() {
        var auth = new AnonymousAuthenticationToken("key", "user",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(auth);
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
            boolean enableNavigationAccessControl) {
        http.with(configurer, c -> {
            c.enableNavigationAccessControl(enableNavigationAccessControl);
        }).build();

        var nac = http.getSharedObject(NavigationAccessControl.class);
        assertThat(nac.isEnabled()).isEqualTo(enableNavigationAccessControl);
    }

    @Test
    @SuppressWarnings("unchecked")
    void disableDefaultConfigurers_configurersAreNotApplied() {
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
    void requestCache_customRulesAreApplied() {
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
    void loginView_requestCacheApplied() {
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
    void hillaAnonymousEndpointRequest_arePermitted() {
        try (MockedStatic<EndpointRequestUtil> endpointRequestUtilMockedStatic = Mockito
                .mockStatic(EndpointRequestUtil.class)) {
            endpointRequestUtilMockedStatic
                    .when(EndpointRequestUtil::isHillaAvailable)
                    .thenReturn(true);

            var auth = new AnonymousAuthenticationToken("key", "user",
                    List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
            SecurityContextHolder.getContext().setAuthentication(auth);
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
    void hillaEndpointRequest_areAuthenticated() {
        try (MockedStatic<EndpointRequestUtil> endpointRequestUtilMockedStatic = Mockito
                .mockStatic(EndpointRequestUtil.class)) {
            endpointRequestUtilMockedStatic
                    .when(EndpointRequestUtil::isHillaAvailable)
                    .thenReturn(true);

            var auth = new TestingAuthenticationToken("user", "password",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
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
    void hilla_checkAllowedRoutes() {
        try (MockedStatic<EndpointRequestUtil> endpointRequestUtilMockedStatic = Mockito
                .mockStatic(EndpointRequestUtil.class)) {
            endpointRequestUtilMockedStatic
                    .when(EndpointRequestUtil::isHillaAvailable)
                    .thenReturn(true);

            var auth = new TestingAuthenticationToken("user", "password",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            var path = "/hilla-view";
            var request = new MockHttpServletRequest("POST", path);
            request.setPathInfo(path);

            // Simulate usage of Hilla API, providing file-router request info
            Mockito.when(fileRouterRequestUtil.isAnonymousRoute(request))
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
    void defaultSuccessUrl_withLoginView_successHandlerIsConfigured()
            throws Exception {
        http.with(configurer,
                c -> c.loginView("/login").defaultSuccessUrl("/dashboard"))
                .build();

        var handler = http.getSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class);

        assertThat(handler).isNotNull();
        assertThat(getDefaultTargetUrl(handler)).isEqualTo("/dashboard");
        assertThat(isAlwaysUseDefaultTargetUrl(handler)).isFalse();
    }

    @Test
    void defaultSuccessUrl_withLoginViewClass_successHandlerIsConfigured()
            throws Exception {
        http.with(configurer, c -> c.loginView(TestLoginView.class)
                .defaultSuccessUrl("/home")).build();

        var handler = http.getSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class);

        assertThat(handler).isNotNull();
        assertThat(getDefaultTargetUrl(handler)).isEqualTo("/home");
        assertThat(isAlwaysUseDefaultTargetUrl(handler)).isFalse();
    }

    @Test
    void defaultSuccessUrl_withOAuth2LoginPage_successHandlerIsConfigured()
            throws Exception {
        http.with(configurer,
                c -> c.oauth2LoginPage("/oauth2/authorization/google")
                        .defaultSuccessUrl("/main"))
                .build();

        var handler = http.getSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class);

        assertThat(handler).isNotNull();
        assertThat(getDefaultTargetUrl(handler)).isEqualTo("/main");
        assertThat(isAlwaysUseDefaultTargetUrl(handler)).isFalse();
    }

    @Test
    void defaultSuccessUrl_withAlwaysUseTrue_alwaysRedirectsToDefaultUrl()
            throws Exception {
        http.with(configurer, c -> c.loginView("/login")
                .defaultSuccessUrl("/dashboard", true)).build();

        var handler = http.getSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class);

        assertThat(handler).isNotNull();
        assertThat(getDefaultTargetUrl(handler)).isEqualTo("/dashboard");
        assertThat(isAlwaysUseDefaultTargetUrl(handler)).isTrue();
    }

    @Test
    void defaultSuccessUrl_withAlwaysUseFalse_redirectsToSavedRequest()
            throws Exception {
        http.with(configurer, c -> c.loginView("/login")
                .defaultSuccessUrl("/dashboard", false)).build();

        var handler = http.getSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class);

        assertThat(handler).isNotNull();
        assertThat(getDefaultTargetUrl(handler)).isEqualTo("/dashboard");
        assertThat(isAlwaysUseDefaultTargetUrl(handler)).isFalse();
    }

    @Test
    void defaultSuccessUrl_notSet_usesRootPath() throws Exception {
        http.with(configurer, c -> c.loginView("/login")).build();

        var handler = http.getSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class);

        assertThat(handler).isNotNull();
        assertThat(getDefaultTargetUrl(handler)).isEqualTo("/");
        assertThat(isAlwaysUseDefaultTargetUrl(handler)).isFalse();
    }

    @Test
    void successUrlResolver_withLoginView_redirectsToResolvedUrl()
            throws Exception {
        http.with(configurer, c -> c.loginView("/login")
                .defaultSuccessUrl("/dashboard")
                .successUrlResolver((request, authentication,
                        savedUrl) -> "/landing-" + authentication.getName()))
                .build();

        var handler = http.getSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class);
        var loginResponse = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(
                new MockHttpServletRequest("POST", "/login"), loginResponse,
                new TestingAuthenticationToken("john", "pwd"));

        assertThat(loginResponse.getRedirectedUrl()).isEqualTo("/landing-john");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void authenticationHandlers_customHandlersAreUsedByAuthenticationFilter(
            boolean oauth2) throws Exception {
        var successHandler = new VaadinSavedRequestAwareAuthenticationSuccessHandler();
        var failureHandler = mock(AuthenticationFailureHandler.class);

        var filters = http.with(configurer, c -> {
            if (oauth2) {
                c.oauth2LoginPage("/oauth2/login");
            } else {
                c.loginView("/login");
            }
            c.authenticationSuccessHandler(successHandler)
                    .authenticationFailureHandler(failureHandler);
        }).build().getFilters();

        var filter = filters.stream().filter(
                AbstractAuthenticationProcessingFilter.class::isInstance)
                .map(AbstractAuthenticationProcessingFilter.class::cast)
                .findFirst().orElseThrow();
        assertThat(invokeGetter(filter, "getSuccessHandler"))
                .isSameAs(successHandler);
        assertThat(invokeGetter(filter, "getFailureHandler"))
                .isSameAs(failureHandler);
    }

    @Test
    void withoutOAuth2ClientOnClasspath_configurerStillLinks() {
        // spring-security-oauth2-client is an optional dependency, and a class
        // is verified as a whole when it is loaded, so a reference to one of
        // its types here would break every application that does not have it
        assertThatCode(
                () -> Class.forName(VaadinSecurityConfigurer.class.getName(),
                        true, new OAuth2ClientHidingClassLoader()))
                .doesNotThrowAnyException();
    }

    @Test
    void sessionConcurrency_expiredUidlRequest_continuesThroughFilterChain()
            throws Exception {
        var request = uidlRequest();

        var response = expireSessionAndRunConcurrentSessionFilter(configurer,
                request);

        verify(chain).doFilter(request, response);
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void expiredSessionStrategy_customStrategyIsUsed() throws Exception {
        var request = uidlRequest();

        var response = expireSessionAndRunConcurrentSessionFilter(
                configurer.expiredSessionStrategy(event -> event.getResponse()
                        .getWriter().write("expired")),
                request);

        assertThat(response.getContentAsString()).isEqualTo("expired");
        verifyNoInteractions(chain);
    }

    @Test
    void sessionManagementConfigurationDisabled_springDefaultIsUsed()
            throws Exception {
        var request = uidlRequest();

        var response = expireSessionAndRunConcurrentSessionFilter(
                configurer.enableSessionManagementConfiguration(false),
                request);

        assertThat(response.getContentAsString())
                .startsWith("This session has been expired");
        verifyNoInteractions(chain);
    }

    private MockHttpServletRequest uidlRequest() {
        var request = new MockHttpServletRequest("GET", "/");
        request.setParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER,
                ApplicationConstants.REQUEST_TYPE_UIDL);
        return request;
    }

    private MockHttpServletResponse expireSessionAndRunConcurrentSessionFilter(
            VaadinSecurityConfigurer configurer, MockHttpServletRequest request)
            throws Exception {
        var sessionRegistry = new SessionRegistryImpl();
        var filters = http.with(configurer, Customizer.withDefaults())
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionConcurrency(
                                concurrency -> concurrency.maximumSessions(1)
                                        .sessionRegistry(sessionRegistry)))
                .build().getFilters();

        var sessionId = request.getSession().getId();
        sessionRegistry.registerNewSession(sessionId, "principal");
        sessionRegistry.getSessionInformation(sessionId).expireNow();

        var response = new MockHttpServletResponse();
        filters.stream().filter(ConcurrentSessionFilter.class::isInstance)
                .findFirst().orElseThrow().doFilter(request, response, chain);
        return response;
    }

    @ParameterizedTest
    @ValueSource(strings = { "style", "script", "image", "font" })
    void anonymousSubResourceRequest_respondsWithUnauthorized(String fetchDest)
            throws Exception {
        var response = sendAnonymousGetRequest("/styles/imported.css",
                fetchDest);

        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getRedirectedUrl()).isNull();
    }

    @Test
    void anonymousDocumentRequest_redirectsToLoginView() throws Exception {
        var response = sendAnonymousGetRequest("/private", "document");

        assertThat(response.getRedirectedUrl()).endsWith("/login");
    }

    /**
     * Sends an anonymous {@code GET} request for the given path, carrying the
     * given {@code Sec-Fetch-Dest} header value, through the filter chain of a
     * configurer set up with a login view, and returns the response.
     */
    private MockHttpServletResponse sendAnonymousGetRequest(String path,
            String fetchDest) throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken("key",
                        "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        var filters = http.with(configurer, c -> c.loginView("/login")).build()
                .getFilters();

        var request = new MockHttpServletRequest("GET", path);
        request.setPathInfo(path);
        request.addHeader("Sec-Fetch-Dest", fetchDest);
        var mockResponse = new MockHttpServletResponse();
        new MockFilterChain(new HttpServlet() {
        }, filters.toArray(Filter[]::new)).doFilter(request, mockResponse);
        return mockResponse;
    }

    /**
     * Loads the Vaadin security classes itself, so that they are verified
     * against a classpath without {@code spring-security-oauth2-client}.
     */
    private static class OAuth2ClientHidingClassLoader extends ClassLoader {

        private static final String HIDDEN_PACKAGE = "org.springframework.security.oauth2.client.";

        private static final String RELOADED_PACKAGE = "com.vaadin.flow.spring.security.";

        OAuth2ClientHidingClassLoader() {
            super(VaadinSecurityConfigurer.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (name.startsWith(HIDDEN_PACKAGE)) {
                throw new ClassNotFoundException(name);
            }
            if (name.startsWith(RELOADED_PACKAGE)) {
                synchronized (getClassLoadingLock(name)) {
                    var loaded = findLoadedClass(name);
                    if (loaded == null) {
                        loaded = defineClass(name, readBytes(name));
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
            }
            return super.loadClass(name, resolve);
        }

        private Class<?> defineClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }

        private byte[] readBytes(String name) throws ClassNotFoundException {
            var resource = name.replace('.', '/') + ".class";
            try (var stream = getParent().getResourceAsStream(resource)) {
                if (stream == null) {
                    throw new ClassNotFoundException(name);
                }
                return stream.readAllBytes();
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }

    // Helper methods to access protected fields using reflection
    private Object getOidcUserConverter(OidcUserService oidcUserService)
            throws Exception {
        var field = OidcUserService.class.getDeclaredField("oidcUserConverter");
        field.setAccessible(true);
        return field.get(oidcUserService);
    }

    private String getDefaultTargetUrl(
            VaadinSavedRequestAwareAuthenticationSuccessHandler handler)
            throws Exception {
        Method method = AbstractAuthenticationTargetUrlRequestHandler.class
                .getDeclaredMethod("getDefaultTargetUrl");
        method.setAccessible(true);
        return (String) method.invoke(handler);
    }

    private Object invokeGetter(AbstractAuthenticationProcessingFilter filter,
            String getterName) throws Exception {
        Method method = AbstractAuthenticationProcessingFilter.class
                .getDeclaredMethod(getterName);
        method.setAccessible(true);
        return method.invoke(filter);
    }

    private boolean isAlwaysUseDefaultTargetUrl(
            VaadinSavedRequestAwareAuthenticationSuccessHandler handler)
            throws Exception {
        Method method = AbstractAuthenticationTargetUrlRequestHandler.class
                .getDeclaredMethod("isAlwaysUseDefaultTargetUrl");
        method.setAccessible(true);
        return (boolean) method.invoke(handler);
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
