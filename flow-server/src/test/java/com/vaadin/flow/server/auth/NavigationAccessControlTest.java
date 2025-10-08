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

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorNavigationEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.NotFoundException;
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
import com.vaadin.flow.server.auth.AccessControlTestClasses.PermitAllView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.TestLoginView;

import static org.mockito.ArgumentMatchers.anyBoolean;

public class NavigationAccessControlTest {

    enum User {
        USER_NO_ROLES, NORMAL_USER, ADMIN
    }

    NavigationAccessControl accessControl;

    NavigationAccessChecker checker1;
    NavigationAccessChecker checker2;
    NavigationAccessChecker checker3;

    @Before
    public void setUp() throws Exception {
        checker1 = Mockito.mock(NavigationAccessChecker.class);
        checker2 = Mockito.mock(NavigationAccessChecker.class);
        checker3 = Mockito.mock(NavigationAccessChecker.class);
        accessControl = new NavigationAccessControl(
                List.of(checker1, checker2, checker3));
    }

    @Test
    public void beforeEnter_principalAndRoleCheckerProvidedToCheckers() {
        mockCheckerResult(checker2, AccessCheckDecision.ALLOW);
        mockCheckerResult(checker3, AccessCheckDecision.ALLOW);

        ArgumentCaptor<NavigationContext> captor = ArgumentCaptor
                .forClass(NavigationContext.class);
        Mockito.when(checker1.check(captor.capture()))
                .then(i -> i.getArgument(0, NavigationContext.class).allow());

        checkAccess(true, false);

        NavigationContext navigationContext = captor.getValue();
        Assert.assertNotNull(
                "Expected Principal to be provided to checkers, but was null",
                navigationContext.getPrincipal());
        Assert.assertTrue("Wrong role checker provided to checkers",
                navigationContext.hasRole("user"));

    }

    @Test
    public void beforeEnter_anonymous_allCheckersAllowAccess_allowNavigation() {
        mockCheckerResult(checker1, AccessCheckDecision.ALLOW);
        mockCheckerResult(checker2, AccessCheckDecision.ALLOW);
        mockCheckerResult(checker3, AccessCheckDecision.ALLOW);
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void beforeEnter_anonymous_allowAndNeutralCheckers_allowNavigation() {
        mockCheckerResult(checker1, AccessCheckDecision.ALLOW);
        mockCheckerResult(checker2, AccessCheckDecision.NEUTRAL);
        mockCheckerResult(checker3, AccessCheckDecision.NEUTRAL);
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void beforeEnter_anonymous_allCheckersDenyAccess_rerouteToNotFound() {
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.DENY);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals(String.join(System.lineSeparator(),
                accessDeniedReason(checker1), accessDeniedReason(checker2),
                accessDeniedReason(checker3)), result.getRerouteErrorMessage());
    }

    @Test
    public void beforeEnter_anonymous_denyAndNeutralCheckers_allowNavigation() {
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.NEUTRAL);
        mockCheckerResult(checker3, AccessCheckDecision.NEUTRAL);
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals(
                String.join(System.lineSeparator(),
                        accessDeniedReason(checker1)),
                result.getRerouteErrorMessage());
    }

    @Test
    public void beforeEnter_anonymous_allCheckersNeutral_rerouteToNotFound() {
        mockCheckerResult(checker1, AccessCheckDecision.NEUTRAL);
        mockCheckerResult(checker2, AccessCheckDecision.NEUTRAL);
        mockCheckerResult(checker3, AccessCheckDecision.NEUTRAL);
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals(
                "Access denied because navigation checkers did not take any decision.",
                result.getRerouteErrorMessage());
    }

    @Test
    public void beforeEnter_developmentMode_anonymous_mixedCheckersConsensus_exceptionThrown() {
        mockCheckerResult(checker1, AccessCheckDecision.ALLOW);
        mockCheckerResult(checker2, AccessCheckDecision.NEUTRAL);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);

        IllegalStateException failure = Assert.assertThrows(
                IllegalStateException.class, () -> checkAccess(false, false));
        Assert.assertTrue(
                "Expected exception because of mixed consensus, but reason was "
                        + failure.getMessage(),
                failure.getMessage().startsWith(
                        "Mixed consensus from navigation checkers"));
        Assert.assertTrue(
                "Expected exception message to contain deny reasons"
                        + failure.getMessage(),
                failure.getMessage().contains(accessDeniedReason(checker3)));

        // Result order does not matter
        Mockito.reset(checker1, checker2, checker3);
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.ALLOW);
        mockCheckerResult(checker3, AccessCheckDecision.NEUTRAL);
        failure = Assert.assertThrows(IllegalStateException.class,
                () -> checkAccess(false, false));
        Assert.assertTrue(
                "Expected exception because of mixed consensus, but reason was "
                        + failure.getMessage(),
                failure.getMessage().startsWith(
                        "Mixed consensus from navigation checkers"));
        Assert.assertTrue(
                "Expected exception message to contain deny reasons"
                        + failure.getMessage(),
                failure.getMessage().contains(accessDeniedReason(checker1)));
    }

    @Test
    public void beforeEnter_productionMode_mixedCheckersConsensus_routeNotFoundWithNoReasonsExposed() {
        mockCheckerResult(checker1, AccessCheckDecision.ALLOW);
        mockCheckerResult(checker2, AccessCheckDecision.NEUTRAL);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        TestNavigationResult result = checkAccess(false, true);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals("", result.getRerouteErrorMessage());

        // Result order does not matter
        Mockito.reset(checker1, checker2, checker3);
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.ALLOW);
        mockCheckerResult(checker3, AccessCheckDecision.NEUTRAL);
        result = checkAccess(false, true);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals("", result.getRerouteErrorMessage());
    }

    @Test
    public void beforeEnter_productionMode_allCheckersDenyAccess_routeNotFoundWithNoReasonsExposed() {
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.DENY);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        TestNavigationResult result = checkAccess(false, true);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals("", result.getRerouteErrorMessage());
    }

    @Test
    public void beforeEnter_productionMode_allCheckersNeutral_routeNotFoundWithNoReasonsExposed() {
        mockCheckerResult(checker1, AccessCheckDecision.NEUTRAL);
        mockCheckerResult(checker2, AccessCheckDecision.NEUTRAL);
        mockCheckerResult(checker3, AccessCheckDecision.NEUTRAL);
        TestNavigationResult result = checkAccess(false, true);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals("", result.getRerouteErrorMessage());
    }

    @Test
    public void beforeEnter_errorHandlingViewReroute_allCheckersNeutral_allowNavigation() {
        mockCheckerResult(checker1, AccessCheckDecision.NEUTRAL);
        mockCheckerResult(checker2, AccessCheckDecision.NEUTRAL);
        mockCheckerResult(checker3, AccessCheckDecision.NEUTRAL);
        TestNavigationResult result = checkAccess(RouteAccessDeniedError.class,
                false, false, true);
        Assert.assertTrue(result.wasTargetViewRendered());
        Assert.assertNull(result.getRerouteError());
        Assert.assertNull("", result.getRerouteErrorMessage());
    }

    @Test(expected = IllegalStateException.class)
    public void setLoginViewStringCannotBeCalledAfterSettingClass() {
        accessControl.setLoginView(TestLoginView.class);
        accessControl.setLoginView("/foo");
    }

    @Test(expected = IllegalStateException.class)
    public void setLoginViewClassCannotBeCalledAfterSettingClass() {
        accessControl.setLoginView(TestLoginView.class);
        accessControl
                .setLoginView(AccessControlTestClasses.NoAnnotationView.class);
    }

    @Test(expected = IllegalStateException.class)
    public void setLoginViewStringCannotBeCalledAfterSettingString() {
        accessControl.setLoginView("/foo");
        accessControl.setLoginView("/bar");
    }

    @Test(expected = IllegalStateException.class)
    public void setLoginViewClassCannotBeCalledAfterSettingString() {
        accessControl.setLoginView("/foo");
        accessControl.setLoginView(TestLoginView.class);
    }

    @Test
    public void setLoginViewStringShouldNotThrowWithSameString() {
        accessControl.setLoginView("/foo");
        accessControl.setLoginView("/foo");
        accessControl.setLoginView(new String("/foo"));
        Assert.assertEquals("/foo", accessControl.getLoginUrl());
    }

    @Test
    public void beforeEnter_loginView_accessToLoginViewAlwaysAllowed() {
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.DENY);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        accessControl.setLoginView(TestLoginView.class);
        TestNavigationResult result = checkAccess(TestLoginView.class, false,
                true, false);
        Assert.assertTrue(
                "Expecting navigation to login view to be allowed, but was not",
                result.wasTargetViewRendered());

        result = checkAccess(TestLoginView.class, true, true, false);
        Assert.assertTrue(
                "Expecting navigation to login view to be allowed, but was not",
                result.wasTargetViewRendered());
    }

    @Test
    public void beforeEnter_loginUrl_accessToLoginUrlAlwaysAllowed() {
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.DENY);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        accessControl.setLoginView("/login");
        TestNavigationResult result = checkAccess(TestLoginView.class, false,
                true, false);
        Assert.assertTrue(
                "Expecting navigation to login view to be allowed, but was not",
                result.wasTargetViewRendered());

        result = checkAccess(TestLoginView.class, true, true, false);
        Assert.assertTrue(
                "Expecting navigation to login view to be allowed, but was not",
                result.wasTargetViewRendered());
    }

    @Test
    public void beforeEnter_loginView_anonymousUser_accessDenied_forwardToLoginView() {
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.DENY);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        accessControl.setLoginView(TestLoginView.class);
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(TestLoginView.class, result.getForwardedTo());
    }

    @Test
    public void beforeEnter_loginUrl_anonymousUser_accessDenied_forwardToLoginUrl() {
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.DENY);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        accessControl.setLoginView("/log-in");
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals("/log-in", result.getExternalForwardUrl());
    }

    @Test
    public void beforeEnter_loginView_authenticatedUser_accessDenied() {
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.DENY);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        accessControl.setLoginView(TestLoginView.class);
        TestNavigationResult result = checkAccess(true, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(AccessDeniedException.class,
                result.getRerouteError());
    }

    @Test
    public void beforeEnter_loginUrl_authenticatedUser_accessDenied() {
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.DENY);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        accessControl.setLoginView("/log-in");
        TestNavigationResult result = checkAccess(true, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(AccessDeniedException.class,
                result.getRerouteError());
    }

    @Test
    public void beforeEnter_redirectUrlStoredForAnonymousUsers() {
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.DENY);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        accessControl.setLoginView(TestLoginView.class);

        TestNavigationResult result = checkAccess(PermitAllView.class, false,
                false, true);

        String routePath = RouteUtil.getRoutePath(new MockVaadinContext(),
                PermitAllView.class);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(AccessAnnotationCheckerTest.REQUEST_URL + routePath,
                result.sessionAttributes.get(
                        NavigationAccessControl.SESSION_STORED_REDIRECT_ABSOLUTE));
        Assert.assertEquals(routePath, result.sessionAttributes
                .get(NavigationAccessControl.SESSION_STORED_REDIRECT));

    }

    @Test
    public void beforeEnter_redirectUrlNotStoredForLoggedInUsers() {
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.DENY);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        accessControl.setLoginView(TestLoginView.class);

        TestNavigationResult result = checkAccess(PermitAllView.class, true,
                false, true);

        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertNull(result.sessionAttributes
                .get(NavigationAccessControl.SESSION_STORED_REDIRECT));
        Assert.assertNull(result.sessionAttributes
                .get(NavigationAccessControl.SESSION_STORED_REDIRECT_ABSOLUTE));

    }

    @Test
    public void beforeEnter_disabledNavigationControl_alwaysPasses_rejectsWhenEnabled() {
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.DENY);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        accessControl.setLoginView(TestLoginView.class);
        accessControl.setEnabled(false);

        TestNavigationResult result = checkAccess(PermitAllView.class, false,
                false, false);
        Assert.assertTrue(
                "Expected navigation to target view to be allowed when access control is disabled",
                result.wasTargetViewRendered());

        Mockito.reset(checker1, checker2, checker3);
        mockCheckerResult(checker1, AccessCheckDecision.DENY);
        mockCheckerResult(checker2, AccessCheckDecision.DENY);
        mockCheckerResult(checker3, AccessCheckDecision.DENY);
        accessControl.setEnabled(true);
        result = checkAccess(PermitAllView.class, false, false, true);
        Assert.assertFalse(
                "Expected navigation to target view to be denied when access control is enabled",
                result.wasTargetViewRendered());

    }

    @Test
    public void beforeEnter_noCheckersConfigured_alwaysPasses() {
        accessControl = new NavigationAccessControl(List.of());
        TestNavigationResult result = checkAccess(AnonymousAllowedView.class,
                false, false, false);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    private TestNavigationResult checkAccess(boolean authenticated,
            boolean productionMode) {
        return checkAccess(AnonymousAllowedView.class, authenticated,
                productionMode, true);
    }

    private TestNavigationResult checkAccess(Class<?> navigationTarget,
            boolean authenticated, boolean productionMode,
            boolean expectCheckersUsed) {
        TestNavigationResult result = setupRequest(navigationTarget,
                authenticated ? User.NORMAL_USER : null, productionMode);
        accessControl.beforeEnter(result.event);
        verifyAllCheckersUsed(expectCheckersUsed);
        return result;
    }

    private void verifyAllCheckersUsed(boolean used) {
        if (used) {
            Mockito.verify(checker1).check(ArgumentMatchers.any());
            Mockito.verify(checker2).check(ArgumentMatchers.any());
            Mockito.verify(checker3).check(ArgumentMatchers.any());
        } else {
            Mockito.verifyNoInteractions(checker1, checker2, checker3);
        }
    }

    private void mockCheckerResult(NavigationAccessChecker checker,
            AccessCheckDecision decision) {
        String denyReason = accessDeniedReason(checker);
        Mockito.when(checker.check(ArgumentMatchers.any())).then(i -> {
            NavigationContext ctx = i.getArgument(0, NavigationContext.class);
            return switch (decision) {
            case ALLOW -> ctx.allow();
            case NEUTRAL -> ctx.neutral();
            case DENY -> ctx.deny(denyReason);
            case REJECT -> ctx.reject(denyReason);
            };
        });
    }

    private static String accessDeniedReason(NavigationAccessChecker checker) {
        return "Access denied by " + checker;
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
        if (HasErrorParameter.class.isAssignableFrom(navigationTarget)) {
            return "some-path";
        }
        return RouteUtil.getRoutePath(new MockVaadinContext(),
                navigationTarget);
    }

}
