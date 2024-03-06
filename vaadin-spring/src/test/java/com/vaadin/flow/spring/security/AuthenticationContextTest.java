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
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.test.context.junit4.SpringRunner;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;

@RunWith(SpringRunner.class)
// @ContextConfiguration
public class AuthenticationContextTest {

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
    public void logout_handlersEngaged() throws Exception {
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

        UI ui = Mockito.mock(UI.class);
        Mockito.doAnswer(i -> {
            i.<Command> getArgument(0).execute();
            return null;
        }).when(ui).accessSynchronously(ArgumentMatchers.any());

        try {
            CurrentInstance.set(VaadinRequest.class, vaadinRequest);
            CurrentInstance.set(VaadinResponse.class, vaadinResponse);
            UI.setCurrent(ui);
            authContext.logout();

            Mockito.verify(successHandler).onLogoutSuccess(request, response,
                    authentication);
            Mockito.verify(handler2).logout(request, response, authentication);
            Mockito.verify(handler1).logout(request, response, authentication);
        } finally {
            CurrentInstance.clearAll();
        }
    }
}
