package com.vaadin.flow.server.auth;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.internal.ErrorStateRenderer;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.DenyAllView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.PermitAllView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.RolesAllowedAdminView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.RolesAllowedUserView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.TestLoginView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationAnonymousAllowedByParentView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationAnonymousAllowedByGrandParentView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationPermitAllByGrandParentView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationDenyAllByGrandParentView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationRolesAllowedUserByGrandParentView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationRolesAllowedAdminByGrandParentView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ViewAccessCheckerTest {

    private enum User {
        USER_NO_ROLES, NORMAL_USER, ADMIN
    };

    private ViewAccessChecker viewAccessChecker;

    @Before
    public void init() {
        this.viewAccessChecker = new ViewAccessChecker();
        this.viewAccessChecker.enable();
        this.viewAccessChecker.setLoginView(TestLoginView.class);
    }

    @Test
    public void cannotUseWithoutServlet() {
        Result request = setupRequest(AnonymousAllowedView.class, null, true);
        CurrentInstance.clearAll();
        this.viewAccessChecker.beforeEnter(request.event);
        Assert.assertEquals(NotFoundException.class, request.getRerouteError());
    }

    @Test
    public void anonymousAccessToAnonymousViewAllowed() {
        Result result = checkAccess(AnonymousAllowedView.class, null);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccessToNoAnnotationViewDenied() {
        Result result = checkAccess(NoAnnotationView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccessToPermitAllViewDenied() {
        Result result = checkAccess(PermitAllView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccessToDenyAllViewDenied() {
        Result result = checkAccess(DenyAllView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccessToRolesAllowedUserViewDenied() {
        Result result = checkAccess(RolesAllowedUserView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccessToRolesAllowedAdminViewDenied() {
        Result result = checkAccess(RolesAllowedAdminView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccessToAnonymousViewAllowed() {
        Result result = checkAccess(AnonymousAllowedView.class,
                User.USER_NO_ROLES);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccessToNoAnnotationViewDenied() {
        Result result = checkAccess(NoAnnotationView.class, User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccessToPermitAllViewAllowed() {
        Result result = checkAccess(PermitAllView.class, User.USER_NO_ROLES);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccessToDenyAllViewDenied() {
        Result result = checkAccess(DenyAllView.class, User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccessToRolesAllowedUserViewDenied() {
        Result result = checkAccess(RolesAllowedUserView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccessToRolesAllowedAdminViewDenied() {
        Result result = checkAccess(RolesAllowedAdminView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccessToAnonymousViewAllowed() {
        Result result = checkAccess(AnonymousAllowedView.class,
                User.NORMAL_USER);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccessToNoAnnotationViewDenied() {
        Result result = checkAccess(NoAnnotationView.class, User.NORMAL_USER);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccessToPermitAllViewAllowed() {
        Result result = checkAccess(PermitAllView.class, User.NORMAL_USER);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccessToDenyAllViewDenied() {
        Result result = checkAccess(DenyAllView.class, User.NORMAL_USER);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccessToRolesAllowedUserViewAllowed() {
        Result result = checkAccess(RolesAllowedUserView.class,
                User.NORMAL_USER);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccessToRolesAllowedAdminViewDenied() {
        Result result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccessToAnonymousViewAllowed() {
        Result result = checkAccess(AnonymousAllowedView.class, User.ADMIN);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccessToNoAnnotationViewDenied() {
        Result result = checkAccess(NoAnnotationView.class, User.ADMIN);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccessToPermitAllViewAllowed() {
        Result result = checkAccess(PermitAllView.class, User.ADMIN);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccessToDenyAllViewDenied() {
        Result result = checkAccess(DenyAllView.class, User.ADMIN);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccessToRolesAllowedUserViewDenied() {
        Result result = checkAccess(RolesAllowedUserView.class, User.ADMIN);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccessToRolesAllowedAdminViewAllowed() {
        Result result = checkAccess(RolesAllowedAdminView.class, User.ADMIN);
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
        Result result = checkAccess(RolesAllowedAdminView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(getRoute(RolesAllowedAdminView.class),
                result.sessionAttributes
                        .get(ViewAccessChecker.SESSION_STORED_REDIRECT));
    }

    @Test
    public void redirectUrlNotStoredForLoggedInUsers() {
        Result result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertNull(result.sessionAttributes
                .get(ViewAccessChecker.SESSION_STORED_REDIRECT));
    }

    @Test
    public void disabledAccessCheckerAlwaysPasses() throws Exception {
        resetCheckerToDisabled();
        Assert.assertTrue(checkAccess(RolesAllowedAdminView.class, null)
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
        Result result = checkAccess(RolesAllowedAdminView.class, null);
        Assert.assertEquals(TestLoginView.class, result.getForwardedTo());
    }

    @Test
    public void openingRestrictedViewRedirectsAnonymousUserToLogin_whenUsingLoginPath()
            throws Exception {
        resetLoginView();
        viewAccessChecker.setLoginView("/log-in");
        Result result = checkAccess(RolesAllowedAdminView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals("/log-in", result.getRedirectUsingPageLocation());
    }

    @Test
    public void openingRestrictedViewShowsNotFoundForLoggedInUser() {
        Result result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER);
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals("", result.getRerouteErrorMessage());
    }

    @Test
    public void openingRestrictedViewShowsReasonInDevelopmentMode() {
        Result result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER, false);
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
        Assert.assertEquals("Access denied", result.getRerouteErrorMessage());
    }

    @Test
    public void redirectWhenNoLoginSet() throws Exception {
        resetLoginView();
        Result result = checkAccess(RolesAllowedAdminView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
        Assert.assertEquals(NotFoundException.class, result.getRerouteError());
    }

    @Test
    public void anonymousAccess_to_noAnnotationAnonymousAllowedByParent_allowed() {
        Result result = checkAccess(
                NoAnnotationAnonymousAllowedByParentView.class,null);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccess_to_noAnnotationAnonymousAllowedByGrandParent_allowed() {
        Result result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class, null);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccess_to_noAnnotationPermitAllByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationDenyAllByGrandParentView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccess_to_noAnnotationRolesAllowedUserByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void anonymousAccess_to_noAnnotationRolesAllowedAdminByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class, null);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationAnonymousAllowedByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationPermitAllByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationDenyAllByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationRolesAllowedUserByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInNoRolesAccess_to_noAnnotationRolesAllowedAdminByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationAnonymousAllowedByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class,
                User.NORMAL_USER);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationPermitAllByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class,
                User.NORMAL_USER);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationDenyAllByGrandParentView.class,
                User.NORMAL_USER);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationRolesAllowedUserByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class,
                User.NORMAL_USER);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInUserRoleAccess_to_noAnnotationRolesAllowedAdminByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class,
                User.NORMAL_USER);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationAnonymousAllowedByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationAnonymousAllowedByGrandParentView.class,
                User.ADMIN);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationPermitAllByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationPermitAllByGrandParentView.class,
                User.ADMIN);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationDenyAllByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationDenyAllByGrandParentView.class,
                User.ADMIN);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccess_to_noAnnotationRolesAllowedUserByGrandParentView_denied() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedUserByGrandParentView.class,
                User.ADMIN);
        Assert.assertFalse(result.wasTargetViewRendered());
    }

    @Test
    public void loggedInAdminRoleAccess_To_noAnnotationRolesAllowedAdminByGrandParentView_allowed() {
        Result result = checkAccess(
                NoAnnotationRolesAllowedAdminByGrandParentView.class,
                User.ADMIN);
        Assert.assertTrue(result.wasTargetViewRendered());
    }

    private void resetLoginView()
            throws NoSuchFieldException, IllegalAccessException {
        Field f = ViewAccessChecker.class.getDeclaredField("loginView");
        f.setAccessible(true);
        f.set(this.viewAccessChecker, null);
    }

    private void resetCheckerToDisabled()
            throws NoSuchFieldException, IllegalAccessException {
        Field f = ViewAccessChecker.class.getDeclaredField("enabled");
        f.setAccessible(true);
        f.set(this.viewAccessChecker, false);
    }

    private static class Result {

        public BeforeEnterEvent event;
        public Map<String, Object> sessionAttributes;
        public String redirectUsingPageLocation;

        public Class<? extends Component> getReroutedTo() {
            if (!event.hasRerouteTarget()
                    || event.getRerouteTarget() instanceof ErrorStateRenderer) {
                return null;
            }
            return event.getRerouteTargetType();
        }

        public Class<? extends Component> getForwardedTo() {
            if (!event.hasForwardTarget()) {
                return null;
            }
            return event.getForwardTargetType();
        }

        public String getRerouteErrorMessage() {
            if (!event.hasRerouteTarget() || !(event
                    .getRerouteTarget() instanceof ErrorStateRenderer)) {
                return null;
            }

            return event.getErrorParameter().getCustomMessage();
        }

        public Class<? extends Exception> getRerouteError() {
            if (!event.hasRerouteTarget() || !(event
                    .getRerouteTarget() instanceof ErrorStateRenderer)) {
                return null;
            }

            return event.getErrorParameter().getException().getClass();
        }

        public String getRedirectUsingPageLocation() {
            return redirectUsingPageLocation;
        }

        public String getRerouteURL() {
            if (event.hasUnknownForward()) {
                return event.getUnknownForward();
            } else {
                return null;
            }
        }

        public boolean wasTargetViewRendered() {
            return getReroutedTo() == null && getForwardedTo() == null
                    && getRerouteError() == null && getRerouteURL() == null;
        }

    }

    private Result checkAccess(Class<?> viewClass, User user) {
        return checkAccess(viewClass, user, true);
    }

    private Result checkAccess(Class<?> viewClass, User user,
            boolean productionMode) {
        Result result = setupRequest(viewClass, user, productionMode);
        BeforeEnterEvent event = result.event;

        this.viewAccessChecker.beforeEnter(event);
        return result;
    }

    private Result setupRequest(Class navigationTarget, User user,
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
        Mockito.doAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            Object value = invocation.getArguments()[1];

            sessionAttributes.put(key, value);

            return null;
        }).when(session).setAttribute(Mockito.anyString(), Mockito.any());

        Result info = new Result();
        info.event = event;
        info.sessionAttributes = sessionAttributes;
        Mockito.doAnswer(invocation -> {
            info.redirectUsingPageLocation = (String) invocation
                    .getArguments()[0];
            return null;
        }).when(page).setLocation(Mockito.anyString());

        return info;
    }

    private String getRoute(Class<?> navigationTarget) {
        Optional<Route> route = AnnotationReader
                .getAnnotationFor(navigationTarget, Route.class);

        return RouteUtil.getRoutePath(navigationTarget, route.get());
    }

}
