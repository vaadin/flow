package com.vaadin.flow.server.auth;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.DenyAllView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.PermitAllView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.RolesAllowedAdminView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.RolesAllowedUserView;
import com.vaadin.flow.server.auth.AccessControlTestClasses.TestLoginView;

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
        this.viewAccessChecker.setEnabled(true);
        this.viewAccessChecker.setLoginView(TestLoginView.class);
    }

    @Test
    public void cannotUseWithoutServlet() {
        Result request = setupRequest(AnonymousAllowedView.class, null);
        CurrentInstance.clearAll();
        this.viewAccessChecker.beforeEnter(request.event);
        Assert.assertEquals(NotFoundException.class,
                request.rerouteToError.get());
    }

    @Test
    public void anonymousAccessToAnonymousViewAllowed() {
        Result result = checkAccess(AnonymousAllowedView.class, null);
        Assert.assertTrue(result.wasAccessGranted());
    }

    @Test
    public void anonymousAccessToNoAnnotationViewDenied() {
        Result result = checkAccess(NoAnnotationView.class, null);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void anonymousAccessToPemitAllViewDenied() {
        Result result = checkAccess(PermitAllView.class, null);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void anonymousAccessToDenyAllViewDenied() {
        Result result = checkAccess(DenyAllView.class, null);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void anonymousAccessToRolesAllowedUserViewDenied() {
        Result result = checkAccess(RolesAllowedUserView.class, null);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void anonymousAccessToRolesAllowedAdminViewDenied() {
        Result result = checkAccess(RolesAllowedAdminView.class, null);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void loggedInNoRolesAccessToAnonymousViewAllowed() {
        Result result = checkAccess(AnonymousAllowedView.class,
                User.USER_NO_ROLES);
        Assert.assertTrue(result.wasAccessGranted());
    }

    @Test
    public void loggedInNoRolesAccessToNoAnnotationViewDenied() {
        Result result = checkAccess(NoAnnotationView.class, User.USER_NO_ROLES);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void loggedInNoRolesAccessToPemitAllViewAllowed() {
        Result result = checkAccess(PermitAllView.class, User.USER_NO_ROLES);
        Assert.assertTrue(result.wasAccessGranted());
    }

    @Test
    public void loggedInNoRolesAccessToDenyAllViewDenied() {
        Result result = checkAccess(DenyAllView.class, User.USER_NO_ROLES);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void loggedInNoRolesAccessToRolesAllowedUserViewDenied() {
        Result result = checkAccess(RolesAllowedUserView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void loggedInNoRolesAccessToRolesAllowedAdminViewDenied() {
        Result result = checkAccess(RolesAllowedAdminView.class,
                User.USER_NO_ROLES);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void loggedInUserRoleAccessToAnonymousViewAllowed() {
        Result result = checkAccess(AnonymousAllowedView.class,
                User.NORMAL_USER);
        Assert.assertTrue(result.wasAccessGranted());
    }

    @Test
    public void loggedInUserRoleAccessToNoAnnotationViewDenied() {
        Result result = checkAccess(NoAnnotationView.class, User.NORMAL_USER);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void loggedInUserRoleAccessToPemitAllViewAllowed() {
        Result result = checkAccess(PermitAllView.class, User.NORMAL_USER);
        Assert.assertTrue(result.wasAccessGranted());
    }

    @Test
    public void loggedInUserRoleAccessToDenyAllViewDenied() {
        Result result = checkAccess(DenyAllView.class, User.NORMAL_USER);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void loggedInUserRoleAccessToRolesAllowedUserViewAllowed() {
        Result result = checkAccess(RolesAllowedUserView.class,
                User.NORMAL_USER);
        Assert.assertTrue(result.wasAccessGranted());
    }

    @Test
    public void loggedInUserRoleAccessToRolesAllowedAdminViewDenied() {
        Result result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void loggedInAdminRoleAccessToAnonymousViewAllowed() {
        Result result = checkAccess(AnonymousAllowedView.class, User.ADMIN);
        Assert.assertTrue(result.wasAccessGranted());
    }

    @Test
    public void loggedInAdminRoleAccessToNoAnnotationViewDenied() {
        Result result = checkAccess(NoAnnotationView.class, User.ADMIN);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void loggedInAdminRoleAccessToPemitAllViewAllowed() {
        Result result = checkAccess(PermitAllView.class, User.ADMIN);
        Assert.assertTrue(result.wasAccessGranted());
    }

    @Test
    public void loggedInAdminRoleAccessToDenyAllViewDenied() {
        Result result = checkAccess(DenyAllView.class, User.ADMIN);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void loggedInAdminRoleAccessToRolesAllowedUserViewDenied() {
        Result result = checkAccess(RolesAllowedUserView.class, User.ADMIN);
        Assert.assertFalse(result.wasAccessGranted());
    }

    @Test
    public void loggedInAdminRoleAccessToRolesAllowedAdminViewAllowed() {
        Result result = checkAccess(RolesAllowedAdminView.class, User.ADMIN);
        Assert.assertTrue(result.wasAccessGranted());
    }

    @Test
    public void loginViewAccessAlwaysAllowed() {
        Assert.assertTrue(
                checkAccess(TestLoginView.class, null).wasAccessGranted());
        Assert.assertTrue(checkAccess(TestLoginView.class, User.NORMAL_USER)
                .wasAccessGranted());
        Assert.assertTrue(checkAccess(TestLoginView.class, User.USER_NO_ROLES)
                .wasAccessGranted());
        Assert.assertTrue(checkAccess(TestLoginView.class, User.ADMIN)
                .wasAccessGranted());
    }

    @Test
    public void redirectUrlStoredForAnonymousUsers() {
        Result result = checkAccess(RolesAllowedAdminView.class, null);
        Assert.assertFalse(result.wasAccessGranted());
        Assert.assertEquals(getRoute(RolesAllowedAdminView.class),
                result.sessionAttributes
                        .get(ViewAccessChecker.SESSION_STORED_REDIRECT));
    }

    @Test
    public void redirectUrlNotStoredForLoggedInUsers() {
        Result result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER);
        Assert.assertFalse(result.wasAccessGranted());
        Assert.assertNull(result.sessionAttributes
                .get(ViewAccessChecker.SESSION_STORED_REDIRECT));
    }

    @Test
    public void disabledAccessCheckerAlwaysPasses() {
        this.viewAccessChecker.setEnabled(false);
        Assert.assertTrue(checkAccess(RolesAllowedAdminView.class, null)
                .wasAccessGranted());
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
        Assert.assertEquals(TestLoginView.class, result.forwarded.get());
    }

    @Test
    public void openingRestrictedViewRedirectsAnonymousUserToLogin_whenUsingLoginPath()
            throws Exception {
        resetLoginView();
        viewAccessChecker.setLoginView("/log-in");
        Result result = checkAccess(RolesAllowedAdminView.class, null);
        Assert.assertEquals("/log-in", result.rerouteToURL.get());
    }

    private void resetLoginView()
            throws NoSuchFieldException, IllegalAccessException {
        Field f = ViewAccessChecker.class.getDeclaredField("loginView");
        f.setAccessible(true);
        f.set(this.viewAccessChecker, null);
    }

    @Test
    public void openingRestrictedViewShowsNotFoundForLoggedInUser() {
        Result result = checkAccess(RolesAllowedAdminView.class,
                User.NORMAL_USER);
        Assert.assertEquals(NotFoundException.class,
                result.rerouteToError.get());
    }

    private static class Result {

        public BeforeEnterEvent event;
        public AtomicReference<Class> rerouted;
        public AtomicReference<Class> forwarded;
        public AtomicReference<Class> rerouteToError;
        public AtomicReference<String> rerouteToURL;
        public Map<String, Object> sessionAttributes;

        public boolean wasReroutedTo(Class<?> target) {
            return rerouted.get() == target;
        }

        public boolean wasForwardedTo(Class<?> target) {
            return forwarded.get() == target;
        }

        public boolean wasReroutedToError(Class<?> target) {
            return rerouteToError.get() == target;
        }

        public boolean wasReroutedToURL(String url) {
            return url.equals(rerouteToURL.get());
        }

        public boolean wasAccessGranted() {
            return rerouted.get() == null && forwarded.get() == null
                    && rerouteToError.get() == null
                    && rerouteToURL.get() == null;
        }

    }

    private Result checkAccess(Class<?> viewClass, User user) {
        Result result = setupRequest(viewClass, user);
        this.viewAccessChecker.beforeEnter(result.event);
        return result;
    }

    private Result setupRequest(Class navigationTarget, User user) {
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

        BeforeEnterEvent event = Mockito.mock(BeforeEnterEvent.class);
        Mockito.when(event.getNavigationTarget()).thenReturn(navigationTarget);
        Mockito.when(event.getLocation())
                .thenReturn(new Location(getRoute(navigationTarget)));

        AtomicReference<Class> rerouted = new AtomicReference<>(null);
        AtomicReference<Class> forwarded = new AtomicReference<>(null);
        AtomicReference<Class> rerouteToError = new AtomicReference<>(null);
        AtomicReference<String> rerouteToURL = new AtomicReference<>(null);

        Mockito.doAnswer(invocation -> {
            rerouted.set((Class) invocation.getArguments()[0]);
            return null;
        }).when(event).rerouteToError(Mockito.any());
        Mockito.doAnswer(invocation -> {
            forwarded.set((Class) invocation.getArguments()[0]);
            return null;
        }).when(event).forwardTo((Class) Mockito.any());
        Mockito.doAnswer(invocation -> {
            rerouteToError.set((Class) invocation.getArguments()[0]);
            return null;
        }).when(event).rerouteToError((Class) Mockito.any());
        Mockito.doAnswer(invocation -> {
            rerouteToError.set((Class) invocation.getArguments()[0]);
            return null;
        }).when(event).rerouteToError((Class) Mockito.any());

        UI ui = Mockito.mock(UI.class);
        Page page = Mockito.mock(Page.class);
        Mockito.when(event.getUI()).thenReturn(ui);
        Mockito.when(ui.getPage()).thenReturn(page);

        Mockito.doAnswer(invocation -> {
            rerouteToURL.set((String) invocation.getArguments()[0]);
            return null;
        }).when(page).setLocation(Mockito.anyString());

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
        info.rerouted = rerouted;
        info.forwarded = forwarded;
        info.rerouteToError = rerouteToError;
        info.rerouteToURL = rerouteToURL;
        info.sessionAttributes = sessionAttributes;

        return info;
    }

    private String getRoute(Class navigationTarget) {
        Optional<Route> route = AnnotationReader
                .getAnnotationFor(navigationTarget, Route.class);

        return RouteUtil.getRoutePath(navigationTarget, route.get());
    }

}
