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
package com.vaadin.flow.server.auth;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinContext;
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
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnotatedViewAccessCheckerTest {

    enum User {
        USER_NO_ROLES, NORMAL_USER, ADMIN
    }

    private final AnnotatedViewAccessChecker viewAccessChecker = new AnnotatedViewAccessChecker();

    @Test
    public void anonymousAccessToAnonymousViewAllowed() {
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                null);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void anonymousAccessToNoAnnotationViewDenied() {
        AccessCheckResult result = checkAccess(NoAnnotationView.class, null);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void anonymousAccessToPermitAllViewDenied() {
        AccessCheckResult result = checkAccess(PermitAllView.class, null);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void anonymousAccessToDenyAllViewDenied() {
        AccessCheckResult result = checkAccess(DenyAllView.class, null);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void anonymousAccessToRolesAllowedUserViewDenied() {
        AccessCheckResult result = checkAccess(RolesAllowedUserView.class,
                null);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void anonymousAccessToRolesAllowedAdminViewDenied() {
        AccessCheckResult result = checkAccess(RolesAllowedAdminView.class,
                null);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccessToAnonymousViewAllowed() {
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                User.USER_NO_ROLES);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInNoRolesAccessToNoAnnotationViewDenied() {
        AccessCheckResult result = checkAccess(NoAnnotationView.class,
                User.USER_NO_ROLES);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccessToPermitAllViewAllowed() {
        AccessCheckResult result = checkAccess(PermitAllView.class,
                User.USER_NO_ROLES);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInNoRolesAccessToPermitAllViewWithNonAnnotatedParentDenied() {
        NavigationContext context = setupNavigationContext(
                AccessControlTestClasses.PermitAllWithEmptyParentView.class,
                User.USER_NO_ROLES);

        RouteData data = new RouteData(
                Collections.singletonList(
                        AccessControlTestClasses.NoPermitParent.class),
                "permitall", Collections.emptyMap(),
                AccessControlTestClasses.PermitAllWithEmptyParentView.class,
                Collections.emptyList());
        Router router = context.getRouter();
        RouteRegistry registry = router.getRegistry();
        Mockito.when(registry.getRegisteredRoutes())
                .thenReturn(Collections.singletonList(data));

        AccessCheckResult result = this.viewAccessChecker.check(context);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccessToDenyAllViewDenied() {
        AccessCheckResult result = checkAccess(DenyAllView.class,
                User.USER_NO_ROLES);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccessToRolesAllowedUserViewDenied() {
        AccessCheckResult result = checkAccess(RolesAllowedUserView.class,
                User.USER_NO_ROLES);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccessToRolesAllowedAdminViewDenied() {
        AccessCheckResult result = checkAccess(RolesAllowedAdminView.class,
                User.USER_NO_ROLES);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInUserRoleAccessToAnonymousViewAllowed() {
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                User.NORMAL_USER);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInUserRoleAccessToNoAnnotationViewDenied() {
        AccessCheckResult result = checkAccess(NoAnnotationView.class,
                User.NORMAL_USER);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInUserRoleAccessToPermitAllViewAllowed() {
        AccessCheckResult result = checkAccess(PermitAllView.class,
                User.NORMAL_USER);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInUserRoleAccessToDenyAllViewDenied() {
        AccessCheckResult result = checkAccess(DenyAllView.class,
                User.NORMAL_USER);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInUserRoleAccessToRolesAllowedUserViewAllowed() {
        AccessCheckResult result = checkAccess(RolesAllowedUserView.class,
                User.NORMAL_USER);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInUserRoleAccessToRolesAllowedAdminViewDenied() {
        AccessCheckResult result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccessToAnonymousViewAllowed() {
        AccessCheckResult result = checkAccess(AnonymousAllowedView.class,
                User.ADMIN);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInAdminRoleAccessToNoAnnotationViewDenied() {
        AccessCheckResult result = checkAccess(NoAnnotationView.class,
                User.ADMIN);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccessToPermitAllViewAllowed() {
        AccessCheckResult result = checkAccess(PermitAllView.class, User.ADMIN);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInAdminRoleAccessToDenyAllViewDenied() {
        AccessCheckResult result = checkAccess(DenyAllView.class, User.ADMIN);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccessToRolesAllowedUserViewDenied() {
        AccessCheckResult result = checkAccess(RolesAllowedUserView.class,
                User.ADMIN);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccessToRolesAllowedAdminViewAllowed() {
        AccessCheckResult result = checkAccess(RolesAllowedAdminView.class,
                User.ADMIN);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void openingNoAnnotationViewShowsReasonAndHint() {
        AccessCheckResult result = checkAccess(NoAnnotationView.class,
                User.NORMAL_USER);
        assertEquals(AccessCheckResult
                .deny("Consider adding one of the following annotations "
                        + "to make the view accessible: @AnonymousAllowed, "
                        + "@PermitAll, @RolesAllowed."),
                result);
    }

    @Test
    public void anonymousAccess_to_noAnnotationAnonymousAllowedByParent_allowed() {
        AccessCheckResult result = checkAccess(
                NoAnnotationAnonymousAllowedByParentView.class, null);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void anonymousAccess_to_noAnnotationAnonymousAllowedByGrandParent_allowed() {
        AccessCheckResult result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class, null);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void anonymousAccess_to_noAnnotationPermitAllByGrandParentView_denied() {
        AccessCheckResult result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class, null);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void anonymousAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        AccessCheckResult result = checkAccess(
                NoAnnotationDenyAllByGrandParentView.class, null);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void anonymousAccess_to_noAnnotationRolesAllowedUserByGrandParentView_denied() {
        AccessCheckResult result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class, null);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void anonymousAccess_to_noAnnotationRolesAllowedAdminByGrandParentView_denied() {
        AccessCheckResult result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class, null);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationAnonymousAllowedByGrandParentView_allowed() {
        AccessCheckResult result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class,
                User.USER_NO_ROLES);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationPermitAllByGrandParentView_allowed() {
        AccessCheckResult result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class,
                User.USER_NO_ROLES);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        AccessCheckResult result = checkAccess(
                NoAnnotationDenyAllByGrandParentView.class, User.USER_NO_ROLES);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationRolesAllowedUserByGrandParentView_denied() {
        AccessCheckResult result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class,
                User.USER_NO_ROLES);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationRolesAllowedAdminByGrandParentView_denied() {
        AccessCheckResult result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class,
                User.USER_NO_ROLES);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationAnonymousAllowedByGrandParentView_allowed() {
        AccessCheckResult result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class,
                User.NORMAL_USER);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationPermitAllByGrandParentView_allowed() {
        AccessCheckResult result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class, User.NORMAL_USER);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        AccessCheckResult result = checkAccess(
                NoAnnotationDenyAllByGrandParentView.class, User.NORMAL_USER);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationRolesAllowedUserByGrandParentView_allowed() {
        AccessCheckResult result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class,
                User.NORMAL_USER);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationRolesAllowedAdminByGrandParentView_denied() {
        AccessCheckResult result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class,
                User.NORMAL_USER);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationAnonymousAllowedByGrandParentView_allowed() {
        AccessCheckResult result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class,
                User.ADMIN);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationPermitAllByGrandParentView_allowed() {
        AccessCheckResult result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class, User.ADMIN);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        AccessCheckResult result = checkAccess(
                NoAnnotationDenyAllByGrandParentView.class, User.ADMIN);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationRolesAllowedUserByGrandParentView_denied() {
        AccessCheckResult result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class,
                User.ADMIN);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInAdminRoleAccess_To_noAnnotationRolesAllowedAdminByGrandParentView_allowed() {
        AccessCheckResult result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class,
                User.ADMIN);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void anyAccess_to_noAnnotationDenyAllAsInterfacesIgnoredView_denied() {
        assertEquals(AccessCheckDecision.DENY,
                checkAccess(NoAnnotationDenyAllAsInterfacesIgnoredView.class,
                        null).decision());

        assertEquals(AccessCheckDecision.DENY,
                checkAccess(NoAnnotationDenyAllAsInterfacesIgnoredView.class,
                        User.USER_NO_ROLES).decision());

        assertEquals(AccessCheckDecision.DENY,
                checkAccess(NoAnnotationDenyAllAsInterfacesIgnoredView.class,
                        User.NORMAL_USER).decision());

        assertEquals(AccessCheckDecision.DENY,
                checkAccess(NoAnnotationDenyAllAsInterfacesIgnoredView.class,
                        User.ADMIN).decision());
    }

    @Test
    public void anonymousAccess_to_noAnnotationPermitAllByGrandParentAsInterfacesIgnoredView_denied() {
        AccessCheckResult result = checkAccess(
                NoAnnotationPermitAllByGrandParentAsInterfacesIgnoredView.class,
                null);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationPermitAllByGrandParentAsInterfacesIgnoredView_allowed() {
        AccessCheckResult result = checkAccess(
                NoAnnotationPermitAllByGrandParentAsInterfacesIgnoredView.class,
                User.USER_NO_ROLES);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void rerouteToError_defaultErrorHandler_allowed() {
        AccessCheckResult result = checkAccess(RouteNotFoundError.class, null);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void rerouteToError_customAnonymousErrorHandler_allowed() {
        AccessCheckResult result = checkAccess(
                AccessControlTestClasses.CustomErrorView.class, null);
        assertEquals(AccessCheckResult.allow(), result);
    }

    @Test
    public void rerouteToError_customNotAnnotatedErrorHandler_deny() {
        AccessCheckResult result = checkAccess(
                AccessControlTestClasses.NotAnnotatedCustomErrorView.class,
                null);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void routeWithNoAnnotationLayout_deny() {
        NavigationContext context = setupNavigationContext(
                AccessControlTestClasses.AnonymousAllowedView.class, null);
        RouteRegistry registry = context.getRouter().getRegistry();
        Mockito.when(registry.hasLayout("anon")).thenReturn(true);
        Mockito.when(registry.getLayout("anon")).thenAnswer(
                invocation -> AccessControlTestClasses.NoAuthLayout.class);
        AccessCheckResult result = this.viewAccessChecker.check(context);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    @Test
    public void routeWithNoAnnotationsAllowedLayout_allowed() {
        NavigationContext context = setupNavigationContext(
                AccessControlTestClasses.AnonymousAllowedView.class, null);
        RouteRegistry registry = context.getRouter().getRegistry();
        Mockito.when(registry.hasLayout("anon")).thenReturn(true);
        Mockito.when(registry.getLayout("anon")).thenAnswer(
                invocation -> AccessControlTestClasses.AnonymousAllowedLayout.class);
        AccessCheckResult result = this.viewAccessChecker.check(context);
        assertEquals(AccessCheckDecision.ALLOW, result.decision());
    }

    @Test
    public void routeWithNoAnnotationsAllowed_LayoutWithAllowed_denied() {
        NavigationContext context = setupNavigationContext(
                AccessControlTestClasses.NoAnnotationView.class, null);
        RouteRegistry registry = context.getRouter().getRegistry();
        Mockito.when(registry.hasLayout("noannotation")).thenReturn(true);
        Mockito.when(registry.getLayout("noannotation")).thenAnswer(
                invocation -> AccessControlTestClasses.AnonymousAllowedLayout.class);
        AccessCheckResult result = this.viewAccessChecker.check(context);
        assertEquals(AccessCheckDecision.DENY, result.decision());
    }

    private AccessCheckResult checkAccess(Class<? extends Component> viewClass,
            User user) {
        NavigationContext context = setupNavigationContext(viewClass, user);
        return this.viewAccessChecker.check(context);
    }

    static NavigationContext setupNavigationContext(
            Class<? extends Component> navigationTarget, User user) {
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

    @Test
    public void layoutDenial_viewBroaderThanLayout_logsWarn() {
        // AnonymousAllowed view (level 3) + PermitAll layout (level 2)
        // = misconfiguration → WARN with "configuration error" message
        String logOutput = captureLogOutput(() -> {
            NavigationContext context = setupLayoutDenialContext(
                    AnonymousAllowedView.class, null,
                    AccessControlTestClasses.PermitAllLayout.class, true,
                    false);
            viewAccessChecker.check(context);
        });
        Assert.assertTrue(
                "Expected WARN log for misconfiguration, got: " + logOutput,
                logOutput.contains("WARN"));
        Assert.assertTrue(
                "Expected 'configuration error' in log, got: " + logOutput,
                logOutput.contains("configuration error"));
    }

    @Test
    public void layoutDenial_sameAccessLevel_navigating_devMode_logsInfo() {
        // PermitAll view + PermitAll layout, navigating=true, devMode=true
        // anonymous user → layout denies, view not broader → INFO
        String logOutput = captureLogOutput(() -> {
            NavigationContext context = setupLayoutDenialContext(
                    PermitAllView.class, null,
                    AccessControlTestClasses.PermitAllLayout.class, true,
                    false);
            viewAccessChecker.check(context);
        });
        Assert.assertTrue(
                "Expected INFO log for dev-mode navigation denial, got: "
                        + logOutput,
                logOutput.contains("INFO"));
        Assert.assertFalse(
                "Should not log WARN when access levels are equal, got: "
                        + logOutput,
                logOutput.contains("WARN")
                        && logOutput.contains("configuration error"));
    }

    @Test
    public void layoutDenial_securityProbe_notNavigating_noWarnOrInfo() {
        // PermitAll view + PermitAll layout, navigating=false (security probe)
        // → DEBUG (not visible at default INFO level)
        String logOutput = captureLogOutput(() -> {
            NavigationContext context = setupLayoutDenialContext(
                    PermitAllView.class, null,
                    AccessControlTestClasses.PermitAllLayout.class, false,
                    false);
            viewAccessChecker.check(context);
        });
        Assert.assertFalse(
                "Security probe should not produce WARN, got: " + logOutput,
                logOutput.contains("WARN") && logOutput.contains("layout"));
        Assert.assertFalse(
                "Security probe should not produce INFO about layout, got: "
                        + logOutput,
                logOutput.contains("INFO") && logOutput.contains("layout"));
    }

    @Test
    public void layoutDenial_navigating_productionMode_noWarnOrInfo() {
        // PermitAll view + PermitAll layout, navigating=true, prodMode=true
        // → DEBUG (not visible at default INFO level)
        String logOutput = captureLogOutput(() -> {
            NavigationContext context = setupLayoutDenialContext(
                    PermitAllView.class, null,
                    AccessControlTestClasses.PermitAllLayout.class, true, true);
            viewAccessChecker.check(context);
        });
        Assert.assertFalse(
                "Production mode should not produce WARN, got: " + logOutput,
                logOutput.contains("WARN") && logOutput.contains("layout"));
        Assert.assertFalse(
                "Production mode should not produce INFO about layout, got: "
                        + logOutput,
                logOutput.contains("INFO") && logOutput.contains("layout"));
    }

    @Test
    public void layoutDenial_parentLayout_viewBroaderThanLayout_logsWarn() {
        // PermitAll view (level 2) with no-annotation parent layout (level 0)
        // = misconfiguration → WARN
        String logOutput = captureLogOutput(() -> {
            NavigationContext context = setupParentLayoutDenialContext(
                    AccessControlTestClasses.PermitAllWithEmptyParentView.class,
                    null, AccessControlTestClasses.NoPermitParent.class, true,
                    false);
            viewAccessChecker.check(context);
        });
        Assert.assertTrue(
                "Expected WARN log for misconfiguration, got: " + logOutput,
                logOutput.contains("WARN"));
        Assert.assertTrue(
                "Expected 'configuration error' in log, got: " + logOutput,
                logOutput.contains("configuration error"));
    }

    /**
     * Captures stderr output produced during the given action. slf4j-simple
     * writes log output to System.err.
     */
    private String captureLogOutput(Runnable action) {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream captureStream = new PrintStream(baos);
        System.setErr(captureStream);
        try {
            action.run();
        } finally {
            System.setErr(originalErr);
        }
        return baos.toString();
    }

    /**
     * Sets up a NavigationContext that triggers the autolayout-based layout
     * denial path for testing log levels.
     */
    private NavigationContext setupLayoutDenialContext(
            Class<? extends Component> viewClass, User user,
            Class<? extends Component> layoutClass, boolean navigating,
            boolean productionMode) {
        CurrentInstance.clearAll();

        Principal principal = getPrincipal(user);
        Set<String> roles = getRoles(user);

        Router router = Mockito.mock(Router.class);
        RouteRegistry routeRegistry = Mockito.mock(RouteRegistry.class);
        Mockito.when(router.getRegistry()).thenReturn(routeRegistry);

        VaadinContext vaadinContext = Mockito.mock(VaadinContext.class);
        Mockito.when(routeRegistry.getContext()).thenReturn(vaadinContext);

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).thenReturn(productionMode);
        Mockito.when(vaadinContext.getAttribute(
                Mockito.eq(ApplicationConfiguration.class), Mockito.any()))
                .thenReturn(appConfig);
        Mockito.when(vaadinContext.getAttribute(ApplicationConfiguration.class))
                .thenReturn(appConfig);

        Mockito.when(routeRegistry.getRegisteredRoutes())
                .thenReturn(Collections.emptyList());

        String route = getRoute(viewClass);
        Mockito.when(routeRegistry.hasLayout(route)).thenReturn(true);
        Mockito.when(routeRegistry.getLayout(route))
                .thenAnswer(invocation -> layoutClass);

        Location location = new Location(route);
        return new NavigationContext(router, viewClass, location,
                RouteParameters.empty(), principal, roles::contains, false,
                navigating);
    }

    /**
     * Sets up a NavigationContext that triggers the parent-layout-based denial
     * path for testing log levels.
     */
    @SuppressWarnings("unchecked")
    private NavigationContext setupParentLayoutDenialContext(
            Class<? extends Component> viewClass, User user,
            Class<? extends Component> parentLayoutClass, boolean navigating,
            boolean productionMode) {
        CurrentInstance.clearAll();

        Principal principal = getPrincipal(user);
        Set<String> roles = getRoles(user);

        Router router = Mockito.mock(Router.class);
        RouteRegistry routeRegistry = Mockito.mock(RouteRegistry.class);
        Mockito.when(router.getRegistry()).thenReturn(routeRegistry);

        VaadinContext vaadinContext = Mockito.mock(VaadinContext.class);
        Mockito.when(routeRegistry.getContext()).thenReturn(vaadinContext);

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).thenReturn(productionMode);
        Mockito.when(vaadinContext.getAttribute(
                Mockito.eq(ApplicationConfiguration.class), Mockito.any()))
                .thenReturn(appConfig);
        Mockito.when(vaadinContext.getAttribute(ApplicationConfiguration.class))
                .thenReturn(appConfig);

        RouteData data = new RouteData(
                Collections.singletonList((Class) parentLayoutClass),
                getRoute(viewClass), Collections.emptyMap(), viewClass,
                Collections.emptyList());
        Mockito.when(routeRegistry.getRegisteredRoutes())
                .thenReturn(Collections.singletonList(data));

        String route = getRoute(viewClass);
        Location location = new Location(route);
        return new NavigationContext(router, viewClass, location,
                RouteParameters.empty(), principal, roles::contains, false,
                navigating);
    }

    private static Principal getPrincipal(User user) {
        if (user == null) {
            return null;
        }
        return AccessAnnotationCheckerTest.USER_PRINCIPAL;
    }

    private static Set<String> getRoles(User user) {
        Set<String> roles = new HashSet<>();
        if (user == User.NORMAL_USER) {
            roles.add("user");
        } else if (user == User.ADMIN) {
            roles.add("admin");
        }
        return roles;
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
