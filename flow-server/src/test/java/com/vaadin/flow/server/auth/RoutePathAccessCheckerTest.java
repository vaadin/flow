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

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorNavigationEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedTemplateView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedUrlParameterView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedWildcardView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedWithParent;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.PermitAllView;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

public class RoutePathAccessCheckerTest {

    private enum User {
        USER_NO_ROLES, NORMAL_USER, ADMIN
    }

    private AccessPathChecker accessPathChecker;
    private RoutePathAccessChecker routePathAccessChecker;
    private Function<Class<?>, Pair<String, RouteParameters>> eventDataFactory = this::getRouteData;

    @Before
    public void init() {
        this.accessPathChecker = Mockito.mock(AccessPathChecker.class);
        this.routePathAccessChecker = new RoutePathAccessChecker(
                accessPathChecker);
    }

    @Test
    public void permittedPath_anonymousAccessToAnonymousViewMainPath_accessAllowed() {
        Mockito.when(accessPathChecker.hasAccess(eq("anon"), any(), any()))
                .thenReturn(true);
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                null);
        Assert.assertEquals(AccessCheckResult.allow(), result);
        Mockito.verify(accessPathChecker).hasAccess(eq("anon"), any(), any());
    }

    @Test
    public void permittedPath_loggedInNoRolesAccessToAnonymousViewMainPath_accessAllowed() {
        Mockito.when(accessPathChecker.hasAccess(eq("anon"), any(), any()))
                .thenReturn(true);
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(AccessCheckResult.allow(), result);
        Mockito.verify(accessPathChecker).hasAccess(eq("anon"), any(), any());
    }

    @Test
    public void permittedPath_loggedInNoRolesAccessToAnnotatedView_accessAllowed() {
        Mockito.when(accessPathChecker.hasAccess(eq("permitall"), any(), any()))
                .thenReturn(true);
        AccessCheckResult result = checkAccess(PermitAllView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(AccessCheckResult.allow(), result);
        Mockito.verify(accessPathChecker).hasAccess(eq("permitall"), any(),
                any());
    }

    @Test
    public void permittedPath_accessToViewWithoutAliases_accessAllowed() {
        Mockito.when(
                accessPathChecker.hasAccess(eq("noannotation"), any(), any()))
                .thenReturn(true);
        AccessCheckResult result = checkAccess(NoAnnotationView.class, null);
        Assert.assertEquals(AccessCheckResult.allow(), result);
        Mockito.verify(accessPathChecker).hasAccess(eq("noannotation"), any(),
                any());
        Mockito.verifyNoMoreInteractions(accessPathChecker);
    }

    @Test
    public void forbiddenPath_anonymousAccessToAnonymousViewMainPath_accessDenied() {
        Mockito.when(accessPathChecker.hasAccess(any(), any(), any()))
                .thenReturn(false);
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                null);
        Assert.assertEquals(AccessCheckDecision.DENY, result.decision());
        Assert.assertEquals(AccessCheckDecision.DENY, result.decision());
        Mockito.verify(accessPathChecker).hasAccess(eq("anon"), any(), any());
    }

    @Test
    public void forbiddenPath_anonymousAccessAnnotatedView_accessDenied() {
        Mockito.when(accessPathChecker.hasAccess(any(), any(), any()))
                .thenReturn(false);
        AccessCheckResult result = checkAccess(PermitAllView.class, null);
        Assert.assertEquals(AccessCheckDecision.DENY, result.decision());
        Mockito.verify(accessPathChecker).hasAccess(eq("permitall"), any(),
                any());
    }

    @Test
    public void forbiddenPath_loggedInNoRolesAccessToAnonymousViewMainPath_accessDenied() {
        Mockito.when(accessPathChecker.hasAccess(any(), any(), any()))
                .thenReturn(false);
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(AccessCheckDecision.DENY, result.decision());
        Mockito.verify(accessPathChecker).hasAccess(eq("anon"), any(), any());
    }

    @Test
    public void forbiddenPath_loggedInNoRolesAccessToAnnotatedView_accessDenied() {
        Mockito.when(accessPathChecker.hasAccess(any(), any(), any()))
                .thenReturn(false);
        AccessCheckResult result = checkAccess(PermitAllView.class,
                User.USER_NO_ROLES);
        Assert.assertEquals(AccessCheckDecision.DENY, result.decision());
        Mockito.verify(accessPathChecker).hasAccess(eq("permitall"), any(),
                any());
    }

    @Test
    public void forbiddenPath_accessToViewWithoutAliases_accessDenied() {
        Mockito.when(
                accessPathChecker.hasAccess(eq("noannotation"), any(), any()))
                .thenReturn(false);
        AccessCheckResult result = checkAccess(NoAnnotationView.class, null);
        Assert.assertEquals(AccessCheckDecision.DENY, result.decision());
        Mockito.verify(accessPathChecker).hasAccess(eq("noannotation"), any(),
                any());
        Mockito.verifyNoMoreInteractions(accessPathChecker);
    }

    @Test
    public void openingForbiddenPath_showsReasonAndHintInDevelopmentMode() {
        Mockito.when(accessPathChecker.hasAccess(any(), any(), any()))
                .thenReturn(false);
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                User.NORMAL_USER);
        Assert.assertEquals(
                AccessCheckResult
                        .deny("Access to 'anon' is denied by security rules."),
                result);
    }

    @Test
    public void pathAlias_aliasPathAllowed_accessGranted() {
        eventDataFactory = target -> new Pair<>("anon-alias",
                RouteParameters.empty());
        Mockito.when(accessPathChecker.hasAccess(anyString(), any(), any()))
                .then(i -> i.getArgument(0, String.class).equals("anon-alias"));
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                null);
        Assert.assertEquals(AccessCheckResult.allow(), result);
        Mockito.verify(accessPathChecker, Mockito.never()).hasAccess(eq("anon"),
                any(), any());
        Mockito.verify(accessPathChecker).hasAccess(eq("anon-alias"), any(),
                any());
    }

    @Test
    public void pathAliasWithParent_aliasPathAllowed_accessGranted() {
        eventDataFactory = target -> new Pair<>("parent/alias-with-parent",
                RouteParameters.empty());
        Mockito.when(accessPathChecker.hasAccess(anyString(), any(), any()))
                .then(i -> i.getArgument(0, String.class)
                        .equals("parent/alias-with-parent"));
        AccessCheckResult result = checkAccess(AnonymousAllowedWithParent.class,
                null);
        Assert.assertEquals(AccessCheckResult.allow(), result);
        Mockito.verify(accessPathChecker, Mockito.never())
                .hasAccess(eq("parent/anon-with-parent"), any(), any());
        Mockito.verify(accessPathChecker)
                .hasAccess(eq("parent/alias-with-parent"), any(), any());
    }

    @Test
    public void wildcardPathAlias_aliasPathAllowed_accessGranted() {
        eventDataFactory = target -> new Pair<>("anon-alias-wildcard/a/b/c",
                new RouteParameters("path", "a/b/c"));
        Mockito.when(accessPathChecker.hasAccess(anyString(), any(), any()))
                .then(i -> i.getArgument(0, String.class)
                        .equals("anon-alias-wildcard/a/b/c"));
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                null);
        Assert.assertEquals(AccessCheckResult.allow(), result);
        Mockito.verify(accessPathChecker, Mockito.never()).hasAccess(eq("anon"),
                any(), any());
        Mockito.verify(accessPathChecker)
                .hasAccess(eq("anon-alias-wildcard/a/b/c"), any(), any());
    }

    @Test
    public void templatePathAlias_aliasPathAllowed_accessGranted() {
        eventDataFactory = target -> new Pair<>(
                "anon-alias-template/ID-123/C0/resource/12345",
                new RouteParameters(Map.of("identifier", "ID-123", "category",
                        "C0", "id", "12345")));
        Mockito.when(accessPathChecker.hasAccess(anyString(), any(), any()))
                .then(i -> i.getArgument(0, String.class).equals(
                        "anon-alias-template/ID-123/C0/resource/12345"));
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                null);
        Assert.assertEquals(AccessCheckResult.allow(), result);
        Mockito.verify(accessPathChecker, Mockito.never()).hasAccess(eq("anon"),
                any(), any());
        Mockito.verify(accessPathChecker).hasAccess(
                eq("anon-alias-template/ID-123/C0/resource/12345"), any(),
                any());
    }

    @Test
    public void pathAlias_aliasPathForbidden_accessDenied() {
        eventDataFactory = target -> new Pair<>("anon-alias",
                RouteParameters.empty());
        Mockito.when(accessPathChecker.hasAccess(anyString(), any(), any()))
                .then(i -> !i.getArgument(0, String.class)
                        .equals("anon-alias"));
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                null);
        Assert.assertEquals("Expected alias path not to be allowed",
                AccessCheckDecision.DENY, result.decision());
        Mockito.verify(accessPathChecker, Mockito.never()).hasAccess(eq("anon"),
                any(), any());
        Mockito.verify(accessPathChecker).hasAccess(eq("anon-alias"), any(),
                any());
    }

    @Test
    public void wildcardPathAlias_aliasPathForbidden_accessDenied() {
        eventDataFactory = target -> new Pair<>("anon-alias-wildcard/a/b/c",
                new RouteParameters("path", "a/b/c"));

        Mockito.when(accessPathChecker.hasAccess(anyString(), any(), any()))
                .then(i -> !i.getArgument(0, String.class)
                        .equals("anon-alias-wildcard/a/b/c"));

        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                null);
        Assert.assertEquals("Expected alias path not to be allowed",
                AccessCheckDecision.DENY, result.decision());
        Mockito.verify(accessPathChecker, Mockito.never()).hasAccess(eq("anon"),
                any(), any());
        Mockito.verify(accessPathChecker)
                .hasAccess(eq("anon-alias-wildcard/a/b/c"), any(), any());
    }

    @Test
    public void pathAliasWithParent_aliasPathForbidden_accessDenied() {
        eventDataFactory = target -> new Pair<>("parent/alias-with-parent",
                RouteParameters.empty());

        Mockito.when(accessPathChecker.hasAccess(anyString(), any(), any()))
                .then(i -> !i.getArgument(0, String.class)
                        .equals("parent/alias-with-parent"));

        AccessCheckResult result = checkAccess(AnonymousAllowedWithParent.class,
                null);
        Assert.assertEquals("Expected alias path not to be allowed",
                AccessCheckDecision.DENY, result.decision());
        Mockito.verify(accessPathChecker, Mockito.never())
                .hasAccess(eq("parent/anon-with-parent"), any(), any());
        Mockito.verify(accessPathChecker)
                .hasAccess(eq("parent/alias-with-parent"), any(), any());
    }

    @Test
    public void templatePathAlias_aliasPathForbidden_accessDenied() {
        eventDataFactory = target -> new Pair<>(
                "anon-alias-template/ID-123/C0/resource/12345",
                new RouteParameters(Map.of("identifier", "ID-123", "category",
                        "C0", "id", "12345")));
        Mockito.when(accessPathChecker.hasAccess(anyString(), any(), any()))
                .then(i -> !i.getArgument(0, String.class).equals(
                        "anon-alias-template/ID-123/C0/resource/12345"));
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                null);
        Assert.assertEquals("Expected alias path not to be allowed",
                AccessCheckDecision.DENY, result.decision());
        Mockito.verify(accessPathChecker, Mockito.never()).hasAccess(eq("anon"),
                any(), any());
        Mockito.verify(accessPathChecker).hasAccess(
                eq("anon-alias-template/ID-123/C0/resource/12345"), any(),
                any());
    }

    @Test
    public void templateRoutePath_locationPathIsChecked() {
        eventDataFactory = target -> new Pair<>(
                "anon-template/ID-123/C0/resource/12345",
                new RouteParameters(Map.of("identifier", "ID-123", "category",
                        "C0", "id", "12345")));
        Mockito.when(accessPathChecker.hasAccess(
                eq("anon-template/ID-123/C0/resource/12345"), any(), any()))
                .thenReturn(true);
        AccessCheckResult result = checkAccess(
                AnonymousAllowedTemplateView.class, null);
        Assert.assertEquals(AccessCheckResult.allow(), result);
        Mockito.verify(accessPathChecker).hasAccess(
                eq("anon-template/ID-123/C0/resource/12345"), any(), any());
    }

    @Test
    public void wildcardRoutePath_locationPathIsChecked() {
        eventDataFactory = target -> new Pair<>("anon-wildcard/a/b/c",
                new RouteParameters("path", "a/b/c"));
        Mockito.when(accessPathChecker.hasAccess(eq("anon-wildcard/a/b/c"),
                any(), any())).thenReturn(true);
        AccessCheckResult result = checkAccess(
                AnonymousAllowedWildcardView.class, null);
        Assert.assertEquals(AccessCheckResult.allow(), result);
        Mockito.verify(accessPathChecker).hasAccess(eq("anon-wildcard/a/b/c"),
                any(), any());
    }

    @Test
    public void urlParameterPath_locationPathIsChecked() {
        eventDataFactory = target -> new Pair<>("anon-url-parameter/a/b/c/d",
                RouteParameters.empty());
        Mockito.when(accessPathChecker
                .hasAccess(eq("anon-url-parameter/a/b/c/d"), any(), any()))
                .thenReturn(true);
        AccessCheckResult result = checkAccess(
                AnonymousAllowedUrlParameterView.class, null);
        Assert.assertEquals(AccessCheckResult.allow(), result);
        Mockito.verify(accessPathChecker)
                .hasAccess(eq("anon-url-parameter/a/b/c/d"), any(), any());
    }

    @Test
    public void rerouteToError_neutral() {
        eventDataFactory = target -> new Pair<>("some-path",
                RouteParameters.empty());
        AccessCheckResult result = checkAccess(RouteNotFoundError.class, null);
        Assert.assertEquals(AccessCheckResult.neutral(), result);
        Mockito.verifyNoInteractions(accessPathChecker);
    }

    private AccessCheckResult checkAccess(Class<?> viewClass, User user) {
        NavigationContext context = setupNavigationContext(viewClass, user);
        return this.routePathAccessChecker.check(context);
    }

    private NavigationContext setupNavigationContext(Class<?> navigationTarget,
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
        Pair<String, RouteParameters> eventData = eventDataFactory
                .apply(navigationTarget);
        Location location = new Location(eventData.getFirst());
        NavigationEvent navigationEvent;
        if (HasErrorParameter.class.isAssignableFrom(navigationTarget)) {
            navigationEvent = new ErrorNavigationEvent(router, location, ui,
                    NavigationTrigger.ROUTER_LINK,
                    new ErrorParameter<>(Exception.class, new Exception()));
        } else {
            navigationEvent = new NavigationEvent(router, location, ui,
                    NavigationTrigger.ROUTER_LINK);
        }
        BeforeEnterEvent event = new BeforeEnterEvent(navigationEvent,
                navigationTarget, eventData.getSecond(), new ArrayList<>());

        RouteRegistry routeRegistry = Mockito.mock(RouteRegistry.class);
        Mockito.when(router.getRegistry()).thenReturn(routeRegistry);
        return new NavigationContext(event, principal, roles::contains);

    }

    private Pair<String, RouteParameters> getRouteData(
            Class<?> navigationTarget) {
        return new Pair<>(RouteUtil.getRoutePath(new MockVaadinContext(),
                navigationTarget), RouteParameters.empty());
    }

}
