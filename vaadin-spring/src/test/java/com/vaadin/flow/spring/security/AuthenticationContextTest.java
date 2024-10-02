/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.configuration.ObjectPostProcessorConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;

import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.spring.security.AuthenticationContext.CompositeLogoutHandler;
import elemental.json.JsonValue;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ObjectPostProcessorConfiguration.class)
public class AuthenticationContextTest {

    @Autowired
    ObjectPostProcessor<Object> postProcessor;

    @Autowired
    ApplicationContext appCtx;

    private final AuthenticationContext authContext = new AuthenticationContext();

    @Test
    public void isAuthenticated_notAuthenticated_false() {
        Assert.assertFalse(authContext.isAuthenticated());
    }

    @Test
    @WithAnonymousUser
    public void isAuthenticated_anonymous_false() {
        Assert.assertFalse(authContext.isAuthenticated());
    }

    @Test
    @WithMockUser
    public void isAuthenticated_loggedUser_true() {
        Assert.assertTrue(authContext.isAuthenticated());
    }

    @Test
    public void getAuthenticatedUser_notAuthenticated_getsEmpty() {
        Assert.assertTrue(
                authContext.getAuthenticatedUser(Object.class).isEmpty());
    }

    @Test
    @WithAnonymousUser
    public void getAuthenticatedUser_anonymous_getsEmpty() {
        Assert.assertTrue(
                authContext.getAuthenticatedUser(Object.class).isEmpty());
    }

    @Test
    @WithMockUser()
    public void getAuthenticatedUser_loggedUser_getsUserInstance() {
        Optional<User> maybeUser = authContext.getAuthenticatedUser(User.class);
        Assert.assertTrue(maybeUser.isPresent());
        Assert.assertEquals("user", maybeUser.get().getUsername());
    }

    @Test
    @WithMockUser()
    public void getAuthenticatedUser_loggedUserWrongUserType_throws() {
        Assert.assertThrows(ClassCastException.class, () -> authContext
                .getAuthenticatedUser(AuthenticatedPrincipal.class));
    }

    @Test
    public void getPrincipalName_notAuthenticated_getsEmpty() {
        Assert.assertTrue(authContext.getPrincipalName().isEmpty());
    }

    @Test
    @WithAnonymousUser
    public void getPrincipalName_anonymous_getsEmpty() {
        Assert.assertTrue(authContext.getPrincipalName().isEmpty());
    }

    @Test
    @WithMockUser(username = "the-username")
    public void getPrincipalName_loggedUser_getsAuthenticationName() {
        Optional<String> maybePrincipalName = authContext.getPrincipalName();
        Assert.assertTrue(maybePrincipalName.isPresent());
        Assert.assertEquals("the-username", maybePrincipalName.get());
    }

    @Test
    @WithMockUser()
    public void logout_allowNullResponse() {
        authContext.setLogoutHandlers(Mockito.mock(LogoutSuccessHandler.class),
                List.of(Mockito.mock(LogoutHandler.class)));
        try {
            CurrentInstance.set(VaadinRequest.class,
                    Mockito.mock(VaadinServletRequest.class));
            UI.setCurrent(Mockito.mock(UI.class));
            mockPush(UI.getCurrent(), Transport.WEBSOCKET_XHR);
            try {
                authContext.logout();
            } catch (NullPointerException e) {
                Assert.fail("Should not throw NPE");
            }
        } finally {
            CurrentInstance.clearAll();
        }
    }

    @Test
    @WithMockUser()
    public void logout_handlersEngaged() throws Exception {
        SetupForLogoutTest setup = getSetupForLogoutTest();

        UI ui = Mockito.mock(UI.class);
        Mockito.doAnswer(i -> {
            i.<Command> getArgument(0).execute();
            return null;
        }).when(ui).accessSynchronously(ArgumentMatchers.any());
        mockPush(ui);
        try {
            CurrentInstance.set(VaadinRequest.class, setup.vaadinRequest());
            CurrentInstance.set(VaadinResponse.class, setup.vaadinResponse());
            UI.setCurrent(ui);
            authContext.logout();

            Mockito.verify(setup.successHandler()).onLogoutSuccess(
                    setup.request(), setup.response(), setup.authentication());
            Mockito.verify(setup.handler2()).logout(setup.request(),
                    setup.response(), setup.authentication());
            Mockito.verify(setup.handler1()).logout(setup.request(),
                    setup.response(), setup.authentication());
        } finally {
            CurrentInstance.clearAll();
        }
    }

    @Test
    @WithMockUser()
    public void logout_pushWithWebsocket_handlersEngaged() throws Exception {
        SetupForLogoutTest setup = getSetupForLogoutTest();

        UI ui = Mockito.mock(UI.class);
        Mockito.doAnswer(i -> {
            i.<Command> getArgument(0).execute();
            return null;
        }).when(ui).accessSynchronously(ArgumentMatchers.any());
        mockPush(ui, Transport.WEBSOCKET);
        Page page = Mockito.mock(Page.class);
        Mockito.when(ui.getPage()).thenReturn(page);
        Mockito.when(page.executeJs(Mockito.anyString()))
                .thenReturn(new PendingJavaScriptResult() {
                    @Override
                    public boolean cancelExecution() {
                        return false;
                    }

                    @Override
                    public boolean isSentToBrowser() {
                        return true;
                    }

                    @Override
                    public void then(
                            SerializableConsumer<JsonValue> resultHandler,
                            SerializableConsumer<String> errorHandler) {
                        resultHandler.accept(null);
                    }
                });
        try {
            CurrentInstance.set(VaadinRequest.class, setup.vaadinRequest());
            CurrentInstance.set(VaadinResponse.class, setup.vaadinResponse());
            UI.setCurrent(ui);
            authContext.logout();

            Mockito.verify(setup.successHandler()).onLogoutSuccess(
                    setup.request(), setup.response(), setup.authentication());
            Mockito.verify(setup.handler2()).logout(setup.request(),
                    setup.response(), setup.authentication());
            Mockito.verify(setup.handler1()).logout(setup.request(),
                    setup.response(), setup.authentication());
        } finally {
            CurrentInstance.clearAll();
        }
    }

    private SetupForLogoutTest getSetupForLogoutTest() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        LogoutSuccessHandler successHandler = Mockito
                .mock(LogoutSuccessHandler.class);
        LogoutHandler handler1 = Mockito.mock(LogoutHandler.class);
        LogoutHandler handler2 = Mockito.mock(LogoutHandler.class);
        authContext.setLogoutHandlers(successHandler,
                List.of(handler2, handler1));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        VaadinServletRequest vaadinRequest = Mockito
                .mock(VaadinServletRequest.class);
        Mockito.when(vaadinRequest.getHttpServletRequest()).thenReturn(request);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        VaadinServletResponse vaadinResponse = Mockito
                .mock(VaadinServletResponse.class);
        Mockito.when(vaadinResponse.getHttpServletResponse())
                .thenReturn(response);
        return new SetupForLogoutTest(authentication, successHandler, handler1,
                handler2, request, vaadinRequest, response, vaadinResponse);
    }

    private record SetupForLogoutTest(Authentication authentication,
            LogoutSuccessHandler successHandler, LogoutHandler handler1,
            LogoutHandler handler2, HttpServletRequest request,
            VaadinServletRequest vaadinRequest, HttpServletResponse response,
            VaadinServletResponse vaadinResponse) {
    }

    @Test
    public void applySecurityConfiguration_logoutHandlerConfigured()
            throws Exception {
        LogoutSuccessHandler logoutSuccessHandler = Mockito
                .mock(LogoutSuccessHandler.class);
        LogoutHandler handler1 = Mockito.mock(LogoutHandler.class);
        LogoutHandler handler2 = Mockito.mock(LogoutHandler.class);

        HttpSecurity httpSecurity = new HttpSecurity(postProcessor,
                new AuthenticationManagerBuilder(postProcessor),
                Map.of(ApplicationContext.class, appCtx));
        httpSecurity
                .logout(cfg -> cfg.logoutSuccessHandler(logoutSuccessHandler)
                        .addLogoutHandler(handler1).addLogoutHandler(handler2));
        httpSecurity.build();
        AuthenticationContext authCtx = new AuthenticationContext();
        AuthenticationContext.applySecurityConfiguration(httpSecurity, authCtx);

        Assert.assertNotNull(authCtx.getLogoutSuccessHandler());
        Assert.assertNotNull(authCtx.getLogoutHandler());

        CompositeLogoutHandler composite = authCtx.getLogoutHandler();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        composite.logout(request, response, authentication);
        Mockito.verify(handler2).logout(request, response, authentication);
        Mockito.verify(handler1).logout(request, response, authentication);
    }

    @Test
    public void applySecurityConfiguration_unbuiltHttpSecurity_throws()
            throws Exception {
        LogoutSuccessHandler logoutSuccessHandler = Mockito
                .mock(LogoutSuccessHandler.class);
        LogoutHandler handler1 = Mockito.mock(LogoutHandler.class);
        LogoutHandler handler2 = Mockito.mock(LogoutHandler.class);
        HttpSecurity httpSecurity = new HttpSecurity(postProcessor,
                new AuthenticationManagerBuilder(postProcessor),
                Map.of(ApplicationContext.class, appCtx));
        httpSecurity
                .logout(cfg -> cfg.logoutSuccessHandler(logoutSuccessHandler)
                        .addLogoutHandler(handler1).addLogoutHandler(handler2));

        AuthenticationContext authCtx = new AuthenticationContext();

        IllegalStateException exception = Assert.assertThrows(
                IllegalStateException.class, () -> AuthenticationContext
                        .applySecurityConfiguration(httpSecurity, authCtx));
        Assert.assertEquals("This object has not been built",
                exception.getMessage());
    }

    private static void mockPush(UI ui) {
        mockPush(ui, null);
    }

    private static void mockPush(UI ui, Transport pushTransport) {
        UIInternals internals = Mockito.mock(UIInternals.class);
        PushConnection pushConnection = Mockito.mock(PushConnection.class);
        PushConfiguration pushConfiguration = Mockito
                .mock(PushConfiguration.class);

        Mockito.when(ui.getPushConfiguration()).thenReturn(pushConfiguration);
        Mockito.when(pushConfiguration.getTransport())
                .thenReturn(pushTransport == null ? Transport.WEBSOCKET_XHR
                        : pushTransport);
        Mockito.when(ui.getInternals()).thenReturn(internals);
        Mockito.when(internals.getPushConnection()).thenReturn(pushConnection);
        Mockito.when(pushConnection.isConnected()).thenReturn(true);
    }
}
