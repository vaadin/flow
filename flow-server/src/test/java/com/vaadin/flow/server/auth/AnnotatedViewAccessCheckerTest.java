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

package com.vaadin.flow.server.auth;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedView;
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
import com.vaadin.flow.server.auth.NavigationAccessChecker.Decision;
import com.vaadin.flow.server.auth.NavigationAccessChecker.NavigationContext;
import com.vaadin.flow.server.auth.NavigationAccessChecker.Result;

public class AnnotatedViewAccessCheckerTest {

    enum User {
        USER_NO_ROLES, NORMAL_USER, ADMIN
    }

    private final AnnotatedViewAccessChecker viewAccessChecker = new AnnotatedViewAccessChecker();

    @Test
    public void anonymousAccessToAnonymousViewAllowed() {
        Result result = checkAccess(AnonymousAllowedView.class, null);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void anonymousAccessToNoAnnotationViewDenied() {
        Result result = checkAccess(NoAnnotationView.class, null);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void anonymousAccessToPermitAllViewDenied() {
        Result result = checkAccess(PermitAllView.class, null);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void anonymousAccessToDenyAllViewDenied() {
        Result result = checkAccess(DenyAllView.class, null);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void anonymousAccessToRolesAllowedUserViewDenied() {
        Result result = checkAccess(RolesAllowedUserView.class, null);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void anonymousAccessToRolesAllowedAdminViewDenied() {
        Result result = checkAccess(RolesAllowedAdminView.class, null);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccessToAnonymousViewAllowed() {
        Result result = checkAccess(AnonymousAllowedView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInNoRolesAccessToNoAnnotationViewDenied() {
        Result result = checkAccess(NoAnnotationView.class, User.USER_NO_ROLES);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccessToPermitAllViewAllowed() {
        Result result = checkAccess(PermitAllView.class, User.USER_NO_ROLES);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInNoRolesAccessToDenyAllViewDenied() {
        Result result = checkAccess(DenyAllView.class, User.USER_NO_ROLES);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccessToRolesAllowedUserViewDenied() {
        Result result = checkAccess(RolesAllowedUserView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccessToRolesAllowedAdminViewDenied() {
        Result result = checkAccess(RolesAllowedAdminView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInUserRoleAccessToAnonymousViewAllowed() {
        Result result = checkAccess(AnonymousAllowedView.class,
                User.NORMAL_USER);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInUserRoleAccessToNoAnnotationViewDenied() {
        Result result = checkAccess(NoAnnotationView.class, User.NORMAL_USER);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInUserRoleAccessToPermitAllViewAllowed() {
        Result result = checkAccess(PermitAllView.class, User.NORMAL_USER);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInUserRoleAccessToDenyAllViewDenied() {
        Result result = checkAccess(DenyAllView.class, User.NORMAL_USER);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInUserRoleAccessToRolesAllowedUserViewAllowed() {
        Result result = checkAccess(RolesAllowedUserView.class,
                User.NORMAL_USER);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInUserRoleAccessToRolesAllowedAdminViewDenied() {
        Result result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccessToAnonymousViewAllowed() {
        Result result = checkAccess(AnonymousAllowedView.class, User.ADMIN);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInAdminRoleAccessToNoAnnotationViewDenied() {
        Result result = checkAccess(NoAnnotationView.class, User.ADMIN);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccessToPermitAllViewAllowed() {
        Result result = checkAccess(PermitAllView.class, User.ADMIN);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInAdminRoleAccessToDenyAllViewDenied() {
        Result result = checkAccess(DenyAllView.class, User.ADMIN);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccessToRolesAllowedUserViewDenied() {
        Result result = checkAccess(RolesAllowedUserView.class, User.ADMIN);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccessToRolesAllowedAdminViewAllowed() {
        Result result = checkAccess(RolesAllowedAdminView.class, User.ADMIN);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void openingNoAnnotationViewShowsReasonAndHint() {
        Result result = checkAccess(NoAnnotationView.class, User.NORMAL_USER);
        Assert.assertEquals(new Result(Decision.DENY,
                "Consider adding one of the following annotations "
                        + "to make the view accessible: @AnonymousAllowed, "
                        + "@PermitAll, @RolesAllowed."),
                result);
    }

    @Test
    public void anonymousAccess_to_noAnnotationAnonymousAllowedByParent_allowed() {
        Result result = checkAccess(
                NoAnnotationAnonymousAllowedByParentView.class, null);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void anonymousAccess_to_noAnnotationAnonymousAllowedByGrandParent_allowed() {
        Result result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class, null);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void anonymousAccess_to_noAnnotationPermitAllByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class, null);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void anonymousAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        Result result = checkAccess(NoAnnotationDenyAllByGrandParentView.class,
                null);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void anonymousAccess_to_noAnnotationRolesAllowedUserByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class, null);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void anonymousAccess_to_noAnnotationRolesAllowedAdminByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class, null);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationAnonymousAllowedByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationPermitAllByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        Result result = checkAccess(NoAnnotationDenyAllByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationRolesAllowedUserByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationRolesAllowedAdminByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationAnonymousAllowedByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class,
                User.NORMAL_USER);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationPermitAllByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class, User.NORMAL_USER);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        Result result = checkAccess(NoAnnotationDenyAllByGrandParentView.class,
                User.NORMAL_USER);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationRolesAllowedUserByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class,
                User.NORMAL_USER);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationRolesAllowedAdminByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class,
                User.NORMAL_USER);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationAnonymousAllowedByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class,
                User.ADMIN);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationPermitAllByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class, User.ADMIN);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        Result result = checkAccess(NoAnnotationDenyAllByGrandParentView.class,
                User.ADMIN);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationRolesAllowedUserByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class,
                User.ADMIN);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccess_To_noAnnotationRolesAllowedAdminByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class,
                User.ADMIN);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void anyAccess_to_noAnnotationDenyAllAsInterfacesIgnoredView_denied() {
        Assert.assertEquals(Decision.DENY,
                checkAccess(NoAnnotationDenyAllAsInterfacesIgnoredView.class,
                        null).decision());

        Assert.assertEquals(Decision.DENY,
                checkAccess(NoAnnotationDenyAllAsInterfacesIgnoredView.class,
                        User.USER_NO_ROLES).decision());

        Assert.assertEquals(Decision.DENY,
                checkAccess(NoAnnotationDenyAllAsInterfacesIgnoredView.class,
                        User.NORMAL_USER).decision());

        Assert.assertEquals(Decision.DENY,
                checkAccess(NoAnnotationDenyAllAsInterfacesIgnoredView.class,
                        User.ADMIN).decision());
    }

    @Test
    public void anonymousAccess_to_noAnnotationPermitAllByGrandParentAsInterfacesIgnoredView_denied() {
        Result result = checkAccess(
                NoAnnotationPermitAllByGrandParentAsInterfacesIgnoredView.class,
                null);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationPermitAllByGrandParentAsInterfacesIgnoredView_allowed() {
        Result result = checkAccess(
                NoAnnotationPermitAllByGrandParentAsInterfacesIgnoredView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void rerouteToError_defaultErrorHandler_allowed() {
        Result result = checkAccess(RouteNotFoundError.class, null);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void rerouteToError_customAnonymousErrorHandler_allowed() {
        Result result = checkAccess(
                AccessControlTestClasses.CustomErrorView.class, null);
        Assert.assertEquals(Result.ALLOW, result);
    }

    @Test
    public void rerouteToError_customNotAnnotatedErrorHandler_deny() {
        Result result = checkAccess(
                AccessControlTestClasses.NotAnnotatedCustomErrorView.class,
                null);
        Assert.assertEquals(Decision.DENY, result.decision());
    }

    private Result checkAccess(Class<?> viewClass, User user) {
        NavigationContext context = setupNavigationContext(viewClass, user);
        return this.viewAccessChecker.check(context);
    }

    static NavigationContext setupNavigationContext(Class<?> navigationTarget,
            User user) {
        CurrentInstance.clearAll();

        Principal principal;
        Set<String> roles = new HashSet<>();

        if (user == User.USER_NO_ROLES) {
            principal = AccessAnnotationCheckerTest.USER_PRINCIPAL;
        } else if (user == User.NORMAL_USER) {
            principal = AccessAnnotationCheckerTest.USER_PRINCIPAL;
            roles.add("user");
        } else if (user == User.ADMIN) {
            principal = AccessAnnotationCheckerTest.USER_PRINCIPAL;
            roles.add("admin");
        } else {
            principal = null;
        }

        Router router = Mockito.mock(Router.class);
        UI ui = Mockito.mock(UI.class);
        Location location = new Location(getRoute(navigationTarget));
        NavigationEvent navigationEvent = new NavigationEvent(router, location,
                ui, NavigationTrigger.ROUTER_LINK);
        BeforeEnterEvent event = new BeforeEnterEvent(navigationEvent,
                navigationTarget, new ArrayList<>());

        RouteRegistry routeRegistry = Mockito.mock(RouteRegistry.class);
        Mockito.when(router.getRegistry()).thenReturn(routeRegistry);
        return new NavigationContext(event, principal, roles::contains);
    }

    static String getRoute(Class<?> navigationTarget) {
        if (navigationTarget.getAnnotation(Route.class) == null
                && HasErrorParameter.class.isAssignableFrom(navigationTarget)) {
            return "mock-path";
        }
        return RouteUtil.getRoutePath(new MockVaadinContext(),
                navigationTarget);
    }

}
