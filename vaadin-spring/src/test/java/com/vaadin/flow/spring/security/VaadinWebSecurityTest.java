/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.configuration.ObjectPostProcessorConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ObjectPostProcessorConfiguration.class)
public class VaadinWebSecurityTest {
    @Autowired
    ObjectPostProcessor<Object> postProcessor;

    @Autowired
    ApplicationContext appCtx;

    @Test
    public void filterChain_additionalLogoutHandlers_configured()
            throws Exception {
        HttpSecurity httpSecurity = new HttpSecurity(postProcessor,
                new AuthenticationManagerBuilder(postProcessor),
                Map.of(ApplicationContext.class, appCtx));
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

}
