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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.configuration.ObjectPostProcessorConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.JsonNode;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ObjectPostProcessorConfiguration.class,
        VaadinSecurityConfigurerTest.TestConfig.class })
class AuthenticationContextTest {

    @Autowired
    ObjectPostProcessor<Object> postProcessor;

    @Autowired
    private PathPatternRequestMatcher.Builder requestMatcherBuilder;

    @Autowired
    ApplicationContext appCtx;

    private final AuthenticationContext authContext = new AuthenticationContext();

    @Test
    void isAuthenticated_notAuthenticated_false() {
        assertFalse(authContext.isAuthenticated());
    }

    @Test
    @WithAnonymousUser
    void isAuthenticated_anonymous_false() {
        assertFalse(authContext.isAuthenticated());
    }

    @Test
    @WithMockUser
    void isAuthenticated_loggedUser_true() {
        assertTrue(authContext.isAuthenticated());
    }

    @Test
    void getAuthenticatedUser_notAuthenticated_getsEmpty() {
        assertTrue(authContext.getAuthenticatedUser(Object.class).isEmpty());
    }

    @Test
    @WithAnonymousUser
    void getAuthenticatedUser_anonymous_getsEmpty() {
        assertTrue(authContext.getAuthenticatedUser(Object.class).isEmpty());
    }

    @Test
    @WithMockUser()
    void getAuthenticatedUser_loggedUser_getsUserInstance() {
        Optional<User> maybeUser = authContext.getAuthenticatedUser(User.class);
        assertTrue(maybeUser.isPresent());
        assertEquals("user", maybeUser.get().getUsername());
    }

    @Test
    @WithMockUser()
    void getAuthenticatedUser_loggedUserWrongUserType_throws() {
        assertThrows(ClassCastException.class, () -> authContext
                .getAuthenticatedUser(AuthenticatedPrincipal.class));
    }

    @Test
    void getPrincipalName_notAuthenticated_getsEmpty() {
        assertTrue(authContext.getPrincipalName().isEmpty());
    }

    @Test
    @WithAnonymousUser
    void getPrincipalName_anonymous_getsEmpty() {
        assertTrue(authContext.getPrincipalName().isEmpty());
    }

    @Test
    @WithMockUser(username = "the-username")
    void getPrincipalName_loggedUser_getsAuthenticationName() {
        Optional<String> maybePrincipalName = authContext.getPrincipalName();
        assertTrue(maybePrincipalName.isPresent());
        assertEquals("the-username", maybePrincipalName.get());
    }

    @Test
    void getGrantedAuthorities_notAuthenticated_emptyResult() {
        assertTrue(authContext.getGrantedAuthorities().isEmpty());
    }

    @Test
    @WithAnonymousUser
    void getGrantedAuthorities_anonymous_emptyResult() {
        assertTrue(authContext.getGrantedAuthorities().isEmpty());
    }

    @Test
    @WithMockUser(roles = { "USER", "ADMIN" })
    void getGrantedAuthorities_authenticated_rolesAreIncluded() {
        var authorities = authContext.getGrantedAuthorities();
        assertEquals(2, authorities.size());
        assertTrue(
                authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(
                authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @WithMockUser(authorities = { "AUTH_READ", "AUTH_WRITE" })
    void getGrantedAuthorities_authenticated_authoritiesAreIncluded() {
        var authorities = authContext.getGrantedAuthorities();
        assertEquals(2, authorities.size());
        assertTrue(
                authorities.contains(new SimpleGrantedAuthority("AUTH_READ")));
        assertTrue(
                authorities.contains(new SimpleGrantedAuthority("AUTH_WRITE")));
    }

    @Test
    void getGrantedRoles_notAuthenticated_emptyResult() {
        assertTrue(authContext.getGrantedRoles().isEmpty());
    }

    @Test
    @WithAnonymousUser
    void getGrantedRoles_anonymous_emptyResult() {
        assertTrue(authContext.getGrantedRoles().isEmpty());
    }

    @Test
    @WithMockUser(roles = { "USER", "ADMIN" })
    void getGrantedRoles_authenticated_rolesAreIncluded() {
        var roles = authContext.getGrantedRoles();
        assertEquals(2, roles.size());
        assertTrue(roles.contains("USER"));
        assertTrue(roles.contains("ADMIN"));
    }

    @Test
    @WithMockUser(authorities = { "AUTH_READ", "AUTH_WRITE" })
    void getGrantedRoles_authenticated_authoritiesAreNotIncluded() {
        assertTrue(authContext.getGrantedRoles().isEmpty());
    }

    @Test
    void hasRole_notAuthenticated_false() {
        assertFalse(authContext.hasRole("USER"));
    }

    @Test
    @WithAnonymousUser
    void hasRole_anonymous_false() {
        assertFalse(authContext.hasRole("ANONYMOUS"));
    }

    @Test
    @WithMockUser(roles = { "USER", "ADMIN" })
    void hasRole_hasRole_true() {
        assertTrue(authContext.hasRole("USER"));
        assertTrue(authContext.hasRole("ADMIN"));
    }

    @Test
    @WithMockUser(roles = { "USER", "ADMIN" })
    void hasRole_lacksRole_false() {
        assertFalse(authContext.hasRole("SUPERADMIN"));
    }

    @Test
    void hasAnyRole_notAuthenticated_false() {
        assertFalse(authContext.hasAnyRole("USER"));
        assertFalse(authContext.hasAnyRole(List.of("USER")));
    }

    @Test
    @WithAnonymousUser()
    void hasAnyRole_anonymous_false() {
        assertFalse(authContext.hasAnyRole("ANONYMOUS"));
        assertFalse(authContext.hasAnyRole(List.of("ANONYMOUS")));
    }

    @Test
    @WithMockUser(roles = { "USER", "ADMIN" })
    void hasAnyRole_hasRole_true() {
        assertTrue(authContext.hasAnyRole("USER"));
        assertTrue(authContext.hasAnyRole(List.of("ADMIN")));
    }

    @Test
    @WithMockUser(roles = { "USER", "ADMIN" })
    void hasAnyRole_hasOneOfTheRoles_true() {
        assertTrue(authContext.hasAnyRole("USER", "SUPERADMIN"));
        assertTrue(authContext.hasAnyRole(List.of("ADMIN", "SUPERADMIN")));
    }

    @Test
    @WithMockUser(roles = { "USER", "ADMIN" })
    void hasAnyRole_lacksRole_false() {
        assertFalse(authContext.hasAnyRole("SUPERADMIN"));
        assertFalse(authContext.hasAnyRole(List.of("SUPERADMIN")));
    }

    @Test
    @WithMockUser(roles = {})
    void hasAnyRole_noRoles_false() {
        assertFalse(authContext.hasAnyRole("USER"));
        assertFalse(authContext.hasAnyRole(List.of("USER")));
    }

    @Test
    void hasAllRoles_notAuthenticated_false() {
        assertFalse(authContext.hasAllRoles("USER", "ADMIN"));
        assertFalse(authContext.hasAllRoles(List.of("USER", "ADMIN")));
    }

    @Test
    @WithAnonymousUser()
    void hasAllRoles_anonymous_false() {
        assertFalse(authContext.hasAllRoles("ANONYMOUS"));
        assertFalse(authContext.hasAllRoles(List.of("ANONYMOUS")));
    }

    @Test
    @WithMockUser(roles = { "USER", "ADMIN" })
    void hasAllRoles_hasRoles_true() {
        assertTrue(authContext.hasAllRoles("USER", "ADMIN"));
        assertTrue(authContext.hasAllRoles(List.of("USER", "ADMIN")));
    }

    @Test
    @WithMockUser(roles = { "USER" })
    void hasAllRoles_lacksRole_false() {
        assertFalse(authContext.hasAllRoles("USER", "ADMIN"));
        assertFalse(authContext.hasAllRoles(List.of("USER", "ADMIN")));
    }

    @Test
    @WithMockUser(roles = {})
    void hasAllRoles_noRoles_false() {
        assertFalse(authContext.hasAllRoles("USER", "ADMIN"));
        assertFalse(authContext.hasAllRoles(List.of("USER", "ADMIN")));
    }

    @Test
    void hasAuthority_notAuthenticated_false() {
        assertFalse(authContext.hasAuthority("AUTH_READ"));
    }

    @Test
    @WithAnonymousUser
    void hasAuthority_anonymous_false() {
        assertFalse(authContext.hasAuthority("ROLE_ANONYMOUS"));
    }

    @Test
    @WithMockUser(authorities = { "AUTH_READ", "AUTH_WRITE" })
    void hasAuthority_hasAuthority_true() {
        assertTrue(authContext.hasAuthority("AUTH_READ"));
        assertTrue(authContext.hasAuthority("AUTH_WRITE"));
    }

    @Test
    @WithMockUser(authorities = { "AUTH_READ", "AUTH_WRITE" })
    void hasAuthority_lacksAuthority_false() {
        assertFalse(authContext.hasAuthority("AUTH_MANAGE"));
    }

    @Test
    void hasAnyAuthority_notAuthenticated_false() {
        assertFalse(authContext.hasAnyAuthority("AUTH_READ"));
        assertFalse(authContext.hasAnyAuthority(List.of("AUTH_READ")));
    }

    @Test
    @WithAnonymousUser()
    void hasAnyAuthority_anonymous_false() {
        assertFalse(authContext.hasAnyAuthority("ROLE_ANONYMOUS"));
        assertFalse(authContext.hasAnyAuthority(List.of("ROLE_ANONYMOUS")));
    }

    @Test
    @WithMockUser(authorities = { "AUTH_READ", "AUTH_WRITE" })
    void hasAnyAuthority_hasAuthority_true() {
        assertTrue(authContext.hasAnyAuthority("AUTH_READ"));
        assertTrue(authContext.hasAnyAuthority(List.of("AUTH_WRITE")));

    }

    @Test
    @WithMockUser(authorities = { "AUTH_READ", "AUTH_WRITE" })
    void hasAnyAuthority_hasOneOfTheAuthorities_true() {
        assertTrue(authContext.hasAnyAuthority("AUTH_READ", "AUTH_MANAGE"));
        assertTrue(authContext
                .hasAnyAuthority(List.of("AUTH_WRITE", "AUTH_MANAGE")));
    }

    @Test
    @WithMockUser(authorities = { "AUTH_READ", "AUTH_WRITE" })
    void hasAnyAuthority_lacksAuthority_false() {
        assertFalse(authContext.hasAnyAuthority("AUTH_MANAGE"));
        assertFalse(authContext.hasAnyAuthority(List.of("AUTH_MANAGE")));
    }

    @Test
    @WithMockUser(roles = {})
    void hasAnyAuthority_noAuthorities_false() {
        assertFalse(authContext.hasAnyAuthority("AUTH_READ"));
        assertFalse(authContext.hasAnyAuthority(List.of("AUTH_WRITE")));
    }

    @Test
    void hasAllAuthorities_notAuthenticated_false() {
        assertFalse(authContext.hasAllAuthorities("AUTH_READ", "AUTH_WRITE"));
        assertFalse(authContext
                .hasAllAuthorities(List.of("AUTH_READ", "AUTH_WRITE")));
    }

    @Test
    @WithAnonymousUser()
    void hasAllAuthorities_anonymous_false() {
        assertFalse(authContext.hasAllAuthorities("ROLE_ANONYMOUS"));
        assertFalse(authContext.hasAllAuthorities(List.of("ROLE_ANONYMOUS")));
    }

    @Test
    @WithMockUser(authorities = { "AUTH_READ", "AUTH_WRITE" })
    void hasAllAuthorities_hasAuthorities_true() {
        assertTrue(authContext.hasAllAuthorities("AUTH_READ", "AUTH_WRITE"));
        assertTrue(authContext
                .hasAllAuthorities(List.of("AUTH_READ", "AUTH_WRITE")));
    }

    @Test
    @WithMockUser(authorities = { "AUTH_READ" })
    void hasAllAuthorities_lacksAuthority_false() {
        assertFalse(authContext.hasAllAuthorities("AUTH_READ", "AUTH_WRITE"));
        assertFalse(authContext
                .hasAllAuthorities(List.of("AUTH_READ", "AUTH_WRITE")));
    }

    @Test
    @WithMockUser(roles = {})
    void hasAllAuthorities_noAuthorities_false() {
        assertFalse(authContext.hasAllAuthorities("AUTH_READ", "AUTH_WRITE"));
        assertFalse(authContext
                .hasAllAuthorities(List.of("AUTH_READ", "AUTH_WRITE")));
    }

    @Test
    @WithMockUser()
    void logout_allowNullResponse() {
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
                fail("Should not throw NPE");
            }
        } finally {
            CurrentInstance.clearAll();
        }
    }

    @Test
    @WithMockUser()
    void logout_handlersEngaged() throws Exception {
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
    void logout_pushWithWebsocket_handlersEngaged() throws Exception {
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
                            SerializableConsumer<JsonNode> resultHandler,
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
    void applySecurityConfiguration_logoutHandlerConfigured() throws Exception {
        LogoutSuccessHandler logoutSuccessHandler = Mockito
                .mock(LogoutSuccessHandler.class);
        LogoutHandler handler1 = Mockito.mock(LogoutHandler.class);
        LogoutHandler handler2 = Mockito.mock(LogoutHandler.class);

        HttpSecurity httpSecurity = new HttpSecurity(postProcessor,
                new AuthenticationManagerBuilder(postProcessor),
                Map.of(ApplicationContext.class, appCtx,
                        PathPatternRequestMatcher.Builder.class,
                        requestMatcherBuilder));
        httpSecurity
                .logout(cfg -> cfg.logoutSuccessHandler(logoutSuccessHandler)
                        .addLogoutHandler(handler1).addLogoutHandler(handler2));
        httpSecurity.build();
        AuthenticationContext authCtx = new AuthenticationContext();
        AuthenticationContext.applySecurityConfiguration(httpSecurity, authCtx);

        assertNotNull(authCtx.getLogoutSuccessHandler());
        assertNotNull(authCtx.getLogoutHandler());

        CompositeLogoutHandler composite = authCtx.getLogoutHandler();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        composite.logout(request, response, authentication);
        Mockito.verify(handler2).logout(request, response, authentication);
        Mockito.verify(handler1).logout(request, response, authentication);
    }

    @Test
    void applySecurityConfiguration_unbuiltHttpSecurity_throws()
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

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> AuthenticationContext
                        .applySecurityConfiguration(httpSecurity, authCtx));
        assertEquals("This object has not been built", exception.getMessage());
    }

    @Test
    @WithMockUser(authorities = { "FOO_USER", "FOO_ADMIN" })
    void supportsCustomRolePrefixes() {
        var prefixHolder = new VaadinRolePrefixHolder("FOO_");
        var authContext = new AuthenticationContext();
        authContext.setRolePrefixHolder(prefixHolder);
        assertTrue(authContext.hasAnyRole("USER", "ADMIN"));
        assertTrue(authContext.hasAllRoles("USER", "ADMIN"));
        var roles = authContext.getGrantedRoles();
        assertTrue(roles.contains("USER"));
        assertTrue(roles.contains("ADMIN"));
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
