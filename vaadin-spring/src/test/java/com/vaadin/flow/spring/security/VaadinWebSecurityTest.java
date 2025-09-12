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

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.configuration.ObjectPostProcessorConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.spring.security.AuthenticationContext.CompositeLogoutHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ObjectPostProcessorConfiguration.class,
        VaadinSecurityConfigurerTest.TestConfig.class })
public class VaadinWebSecurityTest {
    @Autowired
    ObjectPostProcessor<Object> postProcessor;

    @Autowired
    private PathPatternRequestMatcher.Builder requestMatcherBuilder;

    @Autowired
    ApplicationContext appCtx;

    @Test
    public void filterChain_additionalLogoutHandlers_configured()
            throws Exception {
        HttpSecurity httpSecurity = new HttpSecurity(postProcessor,
                new AuthenticationManagerBuilder(postProcessor),
                Map.of(ApplicationContext.class, appCtx,
                        PathPatternRequestMatcher.Builder.class,
                        requestMatcherBuilder));
        TestConfig testConfig = new VaadinWebSecurityTest.TestConfig();
        testConfig.filterChain(httpSecurity);

        Assert.assertTrue("VaadinWebSecurity HTTP configuration invoked",
                testConfig.httpConfigured);

        AuthenticationContext authContext = testConfig
                .getAuthenticationContext();
        CompositeLogoutHandler logoutHandler = authContext.getLogoutHandler();

        logoutHandler.logout(mock(HttpServletRequest.class),
                mock(HttpServletResponse.class), mock(Authentication.class));
        Mockito.verify(testConfig.handler1).logout(any(), any(), any());
        Mockito.verify(testConfig.handler2).logout(any(), any(), any());
    }

    @Test
    public void navigationAccessControl_enabledByDefault() throws Exception {
        HttpSecurity httpSecurity = new HttpSecurity(postProcessor,
                new AuthenticationManagerBuilder(postProcessor),
                Map.of(ApplicationContext.class, appCtx,
                        PathPatternRequestMatcher.Builder.class,
                        requestMatcherBuilder));
        VaadinWebSecurity testConfig = new VaadinWebSecurity() {
        };
        mockVaadinWebSecurityInjection(testConfig);

        testConfig.filterChain(httpSecurity);
        Assert.assertTrue(
                "Expecting navigation access control to be enabled by default",
                testConfig.getNavigationAccessControl().isEnabled());
    }

    @Test
    public void navigationAccessControlEnabled_disabledByWebSecurity()
            throws Exception {
        HttpSecurity httpSecurity = new HttpSecurity(postProcessor,
                new AuthenticationManagerBuilder(postProcessor),
                Map.of(ApplicationContext.class, appCtx,
                        PathPatternRequestMatcher.Builder.class,
                        requestMatcherBuilder));
        VaadinWebSecurity testConfig = new VaadinWebSecurity() {
            @Override
            protected boolean enableNavigationAccessControl() {
                return false;
            }
        };
        mockVaadinWebSecurityInjection(testConfig);

        testConfig.filterChain(httpSecurity);
        Assert.assertFalse(
                "Expecting navigation access control to be disable by VaadinWebSecurity subclass",
                testConfig.getNavigationAccessControl().isEnabled());
    }

    @Test
    public void filterChain_oauth2login_configuresLoginPageAndLogoutHandler()
            throws Exception {
        assertOauth2Configuration(null);
        assertOauth2Configuration("/session-ended");
    }

    private void assertOauth2Configuration(String postLogoutUri)
            throws Exception {
        String expectedLogoutUri = postLogoutUri != null ? postLogoutUri
                : "{baseUrl}";
        HttpSecurity httpSecurity = new HttpSecurity(postProcessor,
                new AuthenticationManagerBuilder(postProcessor),
                Map.of(ApplicationContext.class, appCtx,
                        PathPatternRequestMatcher.Builder.class,
                        requestMatcherBuilder));
        AtomicReference<String> postLogoutUriHolder = new AtomicReference<>(
                "NOT SET");
        VaadinWebSecurity testConfig = new VaadinWebSecurity() {
            @Override
            protected void configure(HttpSecurity http) throws Exception {
                super.configure(http);
                if (postLogoutUri != null) {
                    setOAuth2LoginPage(http, "/externalLogin", postLogoutUri);
                } else {
                    setOAuth2LoginPage(http, "/externalLogin");
                }
            }

            @Override
            protected LogoutSuccessHandler oidcLogoutSuccessHandler(
                    String postLogoutRedirectUri) {
                postLogoutUriHolder.set(postLogoutRedirectUri);
                return super.oidcLogoutSuccessHandler(postLogoutRedirectUri);
            }
        };
        TestNavigationAccessControl accessControl = mockVaadinWebSecurityInjection(
                testConfig);
        ClientRegistrationRepository repository = mock(
                ClientRegistrationRepository.class);
        ObjectProvider<ClientRegistrationRepository> provider = new ObjectProvider<ClientRegistrationRepository>() {
            @Override
            public ClientRegistrationRepository getObject()
                    throws BeansException {
                return repository;
            }
        };
        ApplicationContext appCtx = Mockito.mock(ApplicationContext.class);
        Mockito.when(appCtx.getBeanProvider(ClientRegistrationRepository.class))
                .thenReturn(provider);
        ReflectionTestUtils.setField(testConfig, "applicationContext", appCtx);
        httpSecurity.setSharedObject(ClientRegistrationRepository.class,
                repository);

        testConfig.filterChain(httpSecurity);

        Assert.assertEquals("/externalLogin", accessControl.getLoginUrl());
        LogoutSuccessHandler logoutSuccessHandler = httpSecurity
                .getConfigurer(LogoutConfigurer.class)
                .getLogoutSuccessHandler();
        Assert.assertNotNull("Expected logout success handler to be configured",
                logoutSuccessHandler);
        Assert.assertTrue(
                "Expected logout success handler to be of type OidcClientInitiatedLogoutSuccessHandler, but was "
                        + logoutSuccessHandler.getClass().getName(),
                logoutSuccessHandler instanceof OidcClientInitiatedLogoutSuccessHandler);
        Assert.assertEquals("Unexpected post logout uri", expectedLogoutUri,
                postLogoutUriHolder.get());
    }

    private static TestNavigationAccessControl mockVaadinWebSecurityInjection(
            VaadinWebSecurity testConfig) {
        TestNavigationAccessControl accessControl = new TestNavigationAccessControl();
        ReflectionTestUtils.setField(testConfig, "accessControl",
                accessControl);
        RequestUtil requestUtil = mock(RequestUtil.class);
        Mockito.when(requestUtil.getUrlMapping()).thenReturn("/*");
        Mockito.when(requestUtil.applyUrlMapping(anyString())).then(i -> {
            String path = i.getArgument(0, String.class);
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            return path;
        });
        ReflectionTestUtils.setField(testConfig, "requestUtil", requestUtil);
        ReflectionTestUtils.setField(testConfig, "servletContextPath", "");
        return accessControl;
    }

    static class TestConfig extends VaadinWebSecurity {
        LogoutHandler handler1 = mock(LogoutHandler.class);
        LogoutHandler handler2 = mock(LogoutHandler.class);

        boolean httpConfigured;
        boolean webConfigured;

        @Override
        protected void configure(WebSecurity web) throws Exception {
            webConfigured = true;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            httpConfigured = true;
        }

        protected void addLogoutHandlers(Consumer<LogoutHandler> registry) {
            registry.accept(handler1);
            registry.accept(handler2);
        }
    }

    static class TestNavigationAccessControl extends NavigationAccessControl {

        @Override
        protected String getLoginUrl() {
            return super.getLoginUrl();
        }
    }

}
