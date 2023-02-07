/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
