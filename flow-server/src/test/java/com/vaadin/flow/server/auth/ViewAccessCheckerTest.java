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

package com.vaadin.flow.server.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAccessDeniedError;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.CustomAccessDeniedView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.DenyAllView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationAnonymousAllowedByGrandParentView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationAnonymousAllowedByParentView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationDenyAllAsInterfacesIgnoredView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationDenyAllByGrandParentView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationPermitAllByGrandParentAsInterfacesIgnoredView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationPermitAllByGrandParentView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationRolesAllowedAdminByGrandParentView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationRolesAllowedUserByGrandParentView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.PermitAllView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.RolesAllowedAdminView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.RolesAllowedUserView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.TestLoginView;

import static org.mockito.ArgumentMatchers.anyBoolean;

@Deprecated(forRemoval = true)
public class ViewAccessCheckerTest {

    enum User {
        USER_NO_ROLES, NORMAL_USER, ADMIN
    }

    private ViewAccessChecker viewAccessChecker;

    @Before
    public void init() {
        this.viewAccessChecker = new ViewAccessChecker();
        this.viewAccessChecker.setLoginView(TestLoginView.class);
    }

    @Test
    public void anonymousAccessToAnonymousViewAllowed() {
        TestNavigationResult result = checkAccess(AnonymousAllowedView.class,
                null);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccessToNoAnnotationViewDenied() {
        TestNavigationResult result = checkAccess(NoAnnotationView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccessToPermitAllViewDenied() {
        TestNavigationResult result = checkAccess(PermitAllView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccessToDenyAllViewDenied() {
        TestNavigationResult result = checkAccess(DenyAllView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccessToRolesAllowedUserViewDenied() {
        TestNavigationResult result = checkAccess(RolesAllowedUserView.class,
                null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccessToRolesAllowedAdminViewDenied() {
        TestNavigationResult result = checkAccess(RolesAllowedAdminView.class,
                null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccessToAnonymousViewAllowed() {
        TestNavigationResult result = checkAccess(AnonymousAllowedView.class,
                User.USER_NO_ROLES);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccessToNoAnnotationViewDenied() {
        TestNavigationResult result = checkAccess(NoAnnotationView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccessToPermitAllViewAllowed() {
        TestNavigationResult result = checkAccess(PermitAllView.class,
                User.USER_NO_ROLES);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccessToDenyAllViewDenied() {
        TestNavigationResult result = checkAccess(DenyAllView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccessToRolesAllowedUserViewDenied() {
        TestNavigationResult result = checkAccess(RolesAllowedUserView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccessToRolesAllowedAdminViewDenied() {
        TestNavigationResult result = checkAccess(RolesAllowedAdminView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccessToAnonymousViewAllowed() {
        TestNavigationResult result = checkAccess(AnonymousAllowedView.class,
                User.NORMAL_USER);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccessToNoAnnotationViewDenied() {
        TestNavigationResult result = checkAccess(NoAnnotationView.class,
                User.NORMAL_USER);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccessToPermitAllViewAllowed() {
        TestNavigationResult result = checkAccess(PermitAllView.class,
                User.NORMAL_USER);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccessToDenyAllViewDenied() {
        TestNavigationResult result = checkAccess(DenyAllView.class,
                User.NORMAL_USER);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccessToRolesAllowedUserViewAllowed() {
        TestNavigationResult result = checkAccess(RolesAllowedUserView.class,
                User.NORMAL_USER);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccessToRolesAllowedAdminViewDenied() {
        TestNavigationResult result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccessToAnonymousViewAllowed() {
        TestNavigationResult result = checkAccess(AnonymousAllowedView.class,
                User.ADMIN);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccessToNoAnnotationViewDenied() {
        TestNavigationResult result = checkAccess(NoAnnotationView.class,
                User.ADMIN);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccessToPermitAllViewAllowed() {
        TestNavigationResult result = checkAccess(PermitAllView.class,
                User.ADMIN);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccessToDenyAllViewDenied() {
        TestNavigationResult result = checkAccess(DenyAllView.class,
                User.ADMIN);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccessToRolesAllowedUserViewDenied() {
        TestNavigationResult result = checkAccess(RolesAllowedUserView.class,
                User.ADMIN);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccessToRolesAllowedAdminViewAllowed() {
        TestNavigationResult result = checkAccess(RolesAllowedAdminView.class,
                User.ADMIN);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loginViewAccessAlwaysAllowed() {
        Assert.assertTrue(
                checkAccess(TestLoginView.class, null).wasTargetViewRendered());
        Assert.assertTrue(checkAccess(TestLoginView.class, User.NORMAL_USER)
                .wasTargetViewRendered());
        Assert.assertTrue(checkAccess(TestLoginView.class, User.USER_NO_ROLES)
                .wasTargetViewRendered());
        Assert.assertTrue(checkAccess(TestLoginView.class, User.ADMIN)
                .wasTargetViewRendered());
    }

    @Test
    public void redirectUrlStoredForAnonymousUsers() {
        TestNavigationResult result = checkAccess(RolesAllowedAdminView.class,
                null);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(
                AccessAnnotationCheckerTest.REQUEST_URL
                        + getRoute(RolesAllowedAdminView.class),
                result.sessionAttributes.get(
                        ViewAccessChecker.SESSION_STORED_REDIRECT_ABSOLUTE));
        Assert.assertEquals(getRoute(RolesAllowedAdminView.class),
                result.sessionAttributes
                        .get(ViewAccessChecker.SESSION_STORED_REDIRECT));
    }

    @Test
    public void redirectUrlNotStoredForLoggedInUsers() {
        TestNavigationResult result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertNull(result.sessionAttributes
                .get(ViewAccessChecker.SESSION_STORED_REDIRECT));
        Assert.assertNull(result.sessionAttributes
                .get(ViewAccessChecker.SESSION_STORED_REDIRECT_ABSOLUTE));
    }

    @Test
    public void disabledAccessCheckerAlwaysPasses_rejectsWhenEnabled() {
        viewAccessChecker = new ViewAccessChecker(false);
        Assert.assertTrue(
                "Expected admin view to be accessible for non "
                        + "authenticated users when access checker is disabled",
                checkAccess(RolesAllowedAdminView.class, null)
                        .wasTargetViewRendered());
        viewAccessChecker.enable();
        Assert.assertFalse(
                "Expected admin view to be not accessible for non "
                        + "authenticated users when access checker is enabled",
                checkAccess(RolesAllowedAdminView.class, null)
                        .wasTargetViewRendered());
    }

    @Test(expected = IllegalStateException.class)
    public void setLoginViewStringCannotBeCalledAfterSettingClass()
            throws Exception {
        resetLoginView();
        this.viewAccessChecker.setLoginView(TestLoginView.class);
        this.viewAccessChecker.setLoginView("/foo");
    }

    @Test(expected = IllegalStateException.class)
    public void setLoginViewClassCannotBeCalledAfterSettingClass()
            throws Exception {
        resetLoginView();
        this.viewAccessChecker.setLoginView(TestLoginView.class);
        this.viewAccessChecker.setLoginView(TestLoginView.class);
    }

    @Test(expected = IllegalStateException.class)
    public void setLoginViewStringCannotBeCalledAfterSettingString()
            throws Exception {
        resetLoginView();
        this.viewAccessChecker.setLoginView("/foo");
        this.viewAccessChecker.setLoginView("/foo");
    }

    @Test(expected = IllegalStateException.class)
    public void setLoginViewClassCannotBeCalledAfterSettingString()
            throws Exception {
        resetLoginView();
        this.viewAccessChecker.setLoginView("/foo");
        this.viewAccessChecker.setLoginView(TestLoginView.class);
    }

    @Test
    public void openingRestrictedViewRedirectsAnonymousUserToLogin() {
        TestNavigationResult result = checkAccess(RolesAllowedAdminView.class,
                null);
        Assert.assertEquals(TestLoginView.class, result.getForwardedTo());
    }

    @Test
    public void openingRestrictedViewRedirectsAnonymousUserToLogin_whenUsingLoginPath()
            throws Exception {
        resetLoginView();
        viewAccessChecker.setLoginView("/log-in");
        TestNavigationResult result = checkAccess(RolesAllowedAdminView.class,
                null);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals("/log-in", result.getExternalForwardUrl());
    }

    @Test
    public void openingRestrictedViewShowsNotFoundForLoggedInUser() {
        TestNavigationResult result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER);
        Assert.assertEquals(AccessDeniedException.class,
                result.getRerouteError());
        Assert.assertEquals("", result.getRerouteErrorMessage());
    }

    @Test
    public void openingRestrictedViewShowsReasonInDevelopmentMode() {
        TestNavigationResult result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER, false);
        Assert.assertEquals(AccessDeniedException.class,
                result.getRerouteError());
        Assert.assertEquals("Access denied", result.getRerouteErrorMessage());
    }

    @Test
    public void openingNoAnnotationViewShowsReasonAndHintInDevelopmentMode() {
        TestNavigationResult result = checkAccess(NoAnnotationView.class,
                User.NORMAL_USER, false);
        Assert.assertEquals(AccessDeniedException.class,
                result.getRerouteError());
        Assert.assertEquals(
                "Access denied. Consider adding one of the following annotations "
                        + "to make the view accessible: @AnonymousAllowed, "
                        + "@PermitAll, @RolesAllowed.",
                result.getRerouteErrorMessage());
    }

    @Test
    public void openingCustomAccessDeniedViewShowsReasonAndHintInDevelopmentMode() {
        TestNavigationResult result = checkAccess(CustomAccessDeniedView.class,
                User.NORMAL_USER, false);
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals(
                "Access denied. Consider adding one of the following annotations "
                        + "to make the view accessible: @AnonymousAllowed, "
                        + "@PermitAll, @RolesAllowed.",
                result.getRerouteErrorMessage());
    }

    @Test
    public void openingNoAnnotationViewDoesNotShowAnyReasonAndHintInProductionMode() {
        TestNavigationResult result = checkAccess(NoAnnotationView.class,
                User.NORMAL_USER, true);
        Assert.assertEquals(AccessDeniedException.class,
                result.getRerouteError());
        Assert.assertEquals("", result.getRerouteErrorMessage());
    }

    @Test
    public void openingCustomAccessDeniedViewDoesNotShowAnyReasonAndHintInProductionMode() {
        TestNavigationResult result = checkAccess(CustomAccessDeniedView.class,
                User.NORMAL_USER, true);
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals("", result.getRerouteErrorMessage());
    }

    @Test
    public void redirectWhenNoLoginSet() throws Exception {
        resetLoginView();
        TestNavigationResult result = checkAccess(RolesAllowedAdminView.class,
                null);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
    }

    @Test
    public void anonymousAccess_to_noAnnotationAnonymousAllowedByParent_allowed() {
        TestNavigationResult result = checkAccess(
                NoAnnotationAnonymousAllowedByParentView.class, null);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccess_to_noAnnotationAnonymousAllowedByGrandParent_allowed() {
        TestNavigationResult result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class, null);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccess_to_noAnnotationPermitAllByGrandParentView_denied() {
        TestNavigationResult result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        TestNavigationResult result = checkAccess(
                NoAnnotationDenyAllByGrandParentView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccess_to_noAnnotationRolesAllowedUserByGrandParentView_denied() {
        TestNavigationResult result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccess_to_noAnnotationRolesAllowedAdminByGrandParentView_denied() {
        TestNavigationResult result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationAnonymousAllowedByGrandParentView_allowed() {
        TestNavigationResult result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationPermitAllByGrandParentView_allowed() {
        TestNavigationResult result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        TestNavigationResult result = checkAccess(
                NoAnnotationDenyAllByGrandParentView.class, User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationRolesAllowedUserByGrandParentView_denied() {
        TestNavigationResult result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationRolesAllowedAdminByGrandParentView_denied() {
        TestNavigationResult result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationAnonymousAllowedByGrandParentView_allowed() {
        TestNavigationResult result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class,
                User.NORMAL_USER);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationPermitAllByGrandParentView_allowed() {
        TestNavigationResult result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class, User.NORMAL_USER);
        Assert.assertTrue("Target view should have been rendered",
                result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        TestNavigationResult result = checkAccess(
                NoAnnotationDenyAllByGrandParentView.class, User.NORMAL_USER);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(AccessDeniedException.class,
                result.getRerouteError());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationRolesAllowedUserByGrandParentView_allowed() {
        TestNavigationResult result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class,
                User.NORMAL_USER);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationRolesAllowedAdminByGrandParentView_denied() {
        TestNavigationResult result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class,
                User.NORMAL_USER);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(AccessDeniedException.class,
                result.getRerouteError());
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationAnonymousAllowedByGrandParentView_allowed() {
        TestNavigationResult result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class,
                User.ADMIN);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationPermitAllByGrandParentView_allowed() {
        TestNavigationResult result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class, User.ADMIN);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        TestNavigationResult result = checkAccess(
                NoAnnotationDenyAllByGrandParentView.class, User.ADMIN);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(AccessDeniedException.class,
                result.getRerouteError());
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationRolesAllowedUserByGrandParentView_denied() {
        TestNavigationResult result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class,
                User.ADMIN);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
    }

    @Test
    public void loggedInAdminRoleAccess_To_noAnnotationRolesAllowedAdminByGrandParentView_allowed() {
        TestNavigationResult result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class,
                User.ADMIN);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void anyAccess_to_noAnnotationDenyAllAsInterfacesIgnoredView_denied() {
        Assert.assertFalse(
                checkAccess(NoAnnotationDenyAllAsInterfacesIgnoredView.class,
                        null).wasTargetViewRendered());

        Assert.assertFalse(
                checkAccess(NoAnnotationDenyAllAsInterfacesIgnoredView.class,
                        User.USER_NO_ROLES).wasTargetViewRendered());

        Assert.assertFalse(
                checkAccess(NoAnnotationDenyAllAsInterfacesIgnoredView.class,
                        User.NORMAL_USER).wasTargetViewRendered());

        Assert.assertFalse(
                checkAccess(NoAnnotationDenyAllAsInterfacesIgnoredView.class,
                        User.ADMIN).wasTargetViewRendered());
    }

    @Test
    public void anonymousAccess_to_noAnnotationPermitAllByGrandParentAsInterfacesIgnoredView_denied() {
        TestNavigationResult result = checkAccess(
                NoAnnotationPermitAllByGrandParentAsInterfacesIgnoredView.class,
                null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationPermitAllByGrandParentAsInterfacesIgnoredView_allowed() {
        TestNavigationResult result = checkAccess(
                NoAnnotationPermitAllByGrandParentAsInterfacesIgnoredView.class,
                User.USER_NO_ROLES);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    private void resetLoginView()
            throws NoSuchFieldException, IllegalAccessException {
        Field f = ViewAccessChecker.class.getDeclaredField("loginView");
        f.setAccessible(true);
        f.set(this.viewAccessChecker, null);
    }

    private TestNavigationResult checkAccess(Class<?> viewClass, User user) {
        return checkAccess(viewClass, user, true);
    }

    private TestNavigationResult checkAccess(Class<?> viewClass, User user,
            boolean productionMode) {
        TestNavigationResult result = setupRequest(viewClass, user,
                productionMode);
        BeforeEnterEvent event = result.event;

        this.viewAccessChecker.beforeEnter(event);
        return result;
    }

    static TestNavigationResult setupRequest(Class navigationTarget, User user,
            boolean productionMode) {
        CurrentInstance.clearAll();

        Principal principal;
        String[] roles;

        if (user == User.USER_NO_ROLES) {
            principal = AccessAnnotationCheckerTest.USER_PRINCIPAL;
            roles = new String[0];
        } else if (user == User.NORMAL_USER) {
            principal = AccessAnnotationCheckerTest.USER_PRINCIPAL;
            roles = new String[] { "user" };
        } else if (user == User.ADMIN) {
            principal = AccessAnnotationCheckerTest.USER_PRINCIPAL;
            roles = new String[] { "admin" };
        } else {
            principal = null;
            roles = new String[0];
        }

        VaadinServletRequest vaadinServletRequest = Mockito
                .mock(VaadinServletRequest.class);
        HttpServletRequest httpServletRequest = AccessAnnotationCheckerTest
                .createRequest(principal, roles);
        Mockito.when(vaadinServletRequest.getHttpServletRequest())
                .thenReturn(httpServletRequest);
        Mockito.when(vaadinServletRequest.getUserPrincipal())
                .thenAnswer(answer -> httpServletRequest.getUserPrincipal());
        Mockito.when(vaadinServletRequest.getSession())
                .thenAnswer(answer -> httpServletRequest.getSession());
        Mockito.when(vaadinServletRequest.getSession(anyBoolean())).thenAnswer(
                answer -> httpServletRequest.getSession(answer.getArgument(0)));
        Mockito.when(vaadinServletRequest.isUserInRole(Mockito.any()))
                .thenAnswer(answer -> httpServletRequest
                        .isUserInRole(answer.getArgument(0)));
        Mockito.when(vaadinServletRequest.getRequestURL()).thenReturn(
                new StringBuffer(AccessAnnotationCheckerTest.REQUEST_URL));

        Mockito.when(vaadinServletRequest.getWrappedSession())
                .thenCallRealMethod();
        Mockito.when(vaadinServletRequest.getWrappedSession(anyBoolean()))
                .thenCallRealMethod();

        CurrentInstance.set(VaadinRequest.class, vaadinServletRequest);

        Router router = Mockito.mock(Router.class);
        UI ui = Mockito.mock(UI.class);
        Page page = Mockito.mock(Page.class);
        Mockito.when(ui.getPage()).thenReturn(page);
        VaadinSession vaadinSession = Mockito.mock(VaadinSession.class);
        Mockito.when(ui.getSession()).thenReturn(vaadinSession);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(vaadinSession.getConfiguration())
                .thenReturn(configuration);
        Mockito.when(configuration.isProductionMode())
                .thenReturn(productionMode);

        UIInternals uiInternals = Mockito.mock(UIInternals.class);
        Mockito.when(ui.getInternals()).thenReturn(uiInternals);
        Mockito.when(uiInternals.getRouter()).thenReturn(router);

        Mockito.when(router.getErrorNavigationTarget(Mockito.any()))
                .thenAnswer(invocation -> {
                    Class<?> exceptionClass = invocation.getArguments()[0]
                            .getClass();
                    if (exceptionClass == NotFoundException.class) {
                        return Optional.of(
                                new ErrorTargetEntry(RouteNotFoundError.class,
                                        NotFoundException.class));
                    } else if (exceptionClass == AccessDeniedException.class) {
                        return Optional.of(new ErrorTargetEntry(
                                RouteAccessDeniedError.class,
                                AccessDeniedException.class));
                    } else {
                        return Optional.empty();
                    }

                });
        Location location = new Location(getRoute(navigationTarget));
        NavigationEvent navigationEvent = new NavigationEvent(router, location,
                ui, NavigationTrigger.ROUTER_LINK);
        BeforeEnterEvent event = new BeforeEnterEvent(navigationEvent,
                navigationTarget, new ArrayList<>());

        RouteRegistry routeRegistry = Mockito.mock(RouteRegistry.class);
        Mockito.when(router.getRegistry()).thenReturn(routeRegistry);
        Mockito.when(routeRegistry.getNavigationTarget(Mockito.anyString()))
                .thenAnswer(invocation -> {
                    String url = (String) invocation.getArguments()[0];
                    if (location.getPath().equals(url)) {
                        return Optional.of(navigationTarget);
                    } else {
                        return Optional.empty();
                    }
                });

        HttpSession session = Mockito.mock(HttpSession.class);
        Map<String, Object> sessionAttributes = new HashMap<>();
        Mockito.when(httpServletRequest.getSession()).thenReturn(session);
        Mockito.when(httpServletRequest.getSession(anyBoolean()))
                .thenReturn(session);
        Mockito.doAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            Object value = invocation.getArguments()[1];

            sessionAttributes.put(key, value);

            return null;
        }).when(session).setAttribute(Mockito.anyString(), Mockito.any());

        TestNavigationResult info = new TestNavigationResult();
        info.event = event;
        info.sessionAttributes = sessionAttributes;

        return info;
    }

    static String getRoute(Class<?> navigationTarget) {
        Optional<Route> route = AnnotationReader
                .getAnnotationFor(navigationTarget, Route.class);

        return RouteUtil.getRoutePath(new MockVaadinContext(),
                navigationTarget);
    }

}
