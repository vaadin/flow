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

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.PermitAllView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.TestLoginView;
import com.vaadin.flow.server.auth.NavigationAccessChecker.Decision;
import com.vaadin.flow.server.auth.NavigationAccessChecker.NavigationContext;

public class NavigationAccessControlTest {

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
        mockCheckerResult(checker2, Decision.ALLOW);
        mockCheckerResult(checker3, Decision.ALLOW);

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
        mockCheckerResult(checker1, Decision.ALLOW);
        mockCheckerResult(checker2, Decision.ALLOW);
        mockCheckerResult(checker3, Decision.ALLOW);
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void beforeEnter_anonymous_allowAndNeutralCheckers_allowNavigation() {
        mockCheckerResult(checker1, Decision.ALLOW);
        mockCheckerResult(checker2, Decision.NEUTRAL);
        mockCheckerResult(checker3, Decision.NEUTRAL);
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void beforeEnter_anonymous_allCheckersDenyAccess_rerouteToNotFound() {
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.DENY);
        mockCheckerResult(checker3, Decision.DENY);
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals(String.join(System.lineSeparator(),
                accessDeniedReason(checker1), accessDeniedReason(checker2),
                accessDeniedReason(checker3)), result.getRerouteErrorMessage());
    }

    @Test
    public void beforeEnter_anonymous_denyAndNeutralCheckers_allowNavigation() {
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.NEUTRAL);
        mockCheckerResult(checker3, Decision.NEUTRAL);
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
        mockCheckerResult(checker1, Decision.NEUTRAL);
        mockCheckerResult(checker2, Decision.NEUTRAL);
        mockCheckerResult(checker3, Decision.NEUTRAL);
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals(
                "Access denied because navigation checkers did not take any decision.",
                result.getRerouteErrorMessage());
    }

    @Test
    public void beforeEnter_developmentMode_anonymous_mixedCheckersConsensus_exceptionThrown() {
        mockCheckerResult(checker1, Decision.ALLOW);
        mockCheckerResult(checker2, Decision.NEUTRAL);
        mockCheckerResult(checker3, Decision.DENY);

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
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.ALLOW);
        mockCheckerResult(checker3, Decision.NEUTRAL);
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
        mockCheckerResult(checker1, Decision.ALLOW);
        mockCheckerResult(checker2, Decision.NEUTRAL);
        mockCheckerResult(checker3, Decision.DENY);
        TestNavigationResult result = checkAccess(false, true);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals("", result.getRerouteErrorMessage());

        // Result order does not matter
        Mockito.reset(checker1, checker2, checker3);
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.ALLOW);
        mockCheckerResult(checker3, Decision.NEUTRAL);
        result = checkAccess(false, true);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals("", result.getRerouteErrorMessage());
    }

    @Test
    public void beforeEnter_productionMode_allCheckersDenyAccess_routeNotFoundWithNoReasonsExposed() {
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.DENY);
        mockCheckerResult(checker3, Decision.DENY);
        TestNavigationResult result = checkAccess(false, true);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals("", result.getRerouteErrorMessage());
    }

    @Test
    public void beforeEnter_productionMode_allCheckersNeutral_routeNotFoundWithNoReasonsExposed() {
        mockCheckerResult(checker1, Decision.NEUTRAL);
        mockCheckerResult(checker2, Decision.NEUTRAL);
        mockCheckerResult(checker3, Decision.NEUTRAL);
        TestNavigationResult result = checkAccess(false, true);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals("", result.getRerouteErrorMessage());
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
    public void beforeEnter_loginView_accessToLoginViewAlwaysAllowed() {
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.DENY);
        mockCheckerResult(checker3, Decision.DENY);
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
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.DENY);
        mockCheckerResult(checker3, Decision.DENY);
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
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.DENY);
        mockCheckerResult(checker3, Decision.DENY);
        accessControl.setLoginView(TestLoginView.class);
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(TestLoginView.class, result.getForwardedTo());
    }

    @Test
    public void beforeEnter_loginUrl_anonymousUser_accessDenied_forwardToLoginUrl() {
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.DENY);
        mockCheckerResult(checker3, Decision.DENY);
        accessControl.setLoginView("/log-in");
        TestNavigationResult result = checkAccess(false, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals("/log-in", result.getExternalForwardUrl());
    }

    @Test
    public void beforeEnter_loginView_authenticatedUser_accessDenied() {
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.DENY);
        mockCheckerResult(checker3, Decision.DENY);
        accessControl.setLoginView(TestLoginView.class);
        TestNavigationResult result = checkAccess(true, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(AccessDeniedException.class,
                result.getRerouteError());
    }

    @Test
    public void beforeEnter_loginUrl_authenticatedUser_accessDenied() {
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.DENY);
        mockCheckerResult(checker3, Decision.DENY);
        accessControl.setLoginView("/log-in");
        TestNavigationResult result = checkAccess(true, false);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(AccessDeniedException.class,
                result.getRerouteError());
    }

    @Test
    public void beforeEnter_redirectUrlStoredForAnonymousUsers() {
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.DENY);
        mockCheckerResult(checker3, Decision.DENY);
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
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.DENY);
        mockCheckerResult(checker3, Decision.DENY);
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
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.DENY);
        mockCheckerResult(checker3, Decision.DENY);
        accessControl.setLoginView(TestLoginView.class);
        accessControl.setEnabled(false);

        TestNavigationResult result = checkAccess(PermitAllView.class, false,
                false, false);
        Assert.assertTrue(
                "Expected navigation to target view to be allowed when access control is disabled",
                result.wasTargetViewRendered());

        Mockito.reset(checker1, checker2, checker3);
        mockCheckerResult(checker1, Decision.DENY);
        mockCheckerResult(checker2, Decision.DENY);
        mockCheckerResult(checker3, Decision.DENY);
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
        TestNavigationResult result = ViewAccessCheckerTest.setupRequest(
                navigationTarget,
                authenticated ? ViewAccessCheckerTest.User.NORMAL_USER : null,
                productionMode);
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
            Decision decision) {
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

}