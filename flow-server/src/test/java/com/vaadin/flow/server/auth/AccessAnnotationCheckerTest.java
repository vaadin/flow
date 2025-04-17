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

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI.ClientViewPlaceholder;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AccessControlTestClasses.AnonymousAllowedClass;
import com.vaadin.flow.server.auth.AccessControlTestClasses.DenyAllClass;
import com.vaadin.flow.server.auth.AccessControlTestClasses.NoAnnotationClass;
import com.vaadin.flow.server.auth.AccessControlTestClasses.PermitAllClass;
import com.vaadin.flow.server.auth.AccessControlTestClasses.RolesAllowedAdminClass;
import com.vaadin.flow.server.auth.AccessControlTestClasses.RolesAllowedUserClass;

import static org.junit.Assert.assertEquals;

public class AccessAnnotationCheckerTest {
    public static final Class<?>[] ENDPOINT_CLASSES = new Class<?>[] {
            AccessControlTestClasses.AnonymousAllowedClass.class,
            AccessControlTestClasses.DenyAllClass.class,
            AccessControlTestClasses.NoAnnotationClass.class,
            AccessControlTestClasses.PermitAllClass.class,
            AccessControlTestClasses.RolesAllowedAdminClass.class,
            AccessControlTestClasses.RolesAllowedUserClass.class };

    public static final String[] ENDPOINT_METHODS = new String[] {
            "noAnnotation", "anonymousAllowed", "permitAll", "denyAll",
            "rolesAllowedUser", "rolesAllowedAdmin", "rolesAllowedUserAdmin" };

    public static final String[] ENDPOINT_NAMES = Stream.of(ENDPOINT_CLASSES)
            .map(cls -> cls.getSimpleName().toLowerCase(Locale.ENGLISH))
            .toArray(String[]::new);

    static final Principal USER_PRINCIPAL = new Principal() {
        @Override
        public String getName() {
            return "John Doe";
        }
    };
    static final String REQUEST_URL = "http://localhost:8080/myapp/";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private AccessAnnotationChecker accessAnnotationChecker;

    @Before
    public void before() {
        accessAnnotationChecker = new AccessAnnotationChecker();
    }

    @Test
    public void should_Throw_When_PrivateMethodIsPassed() throws Exception {
        class Test {
            private void test() {
            }
        }

        Method method = Test.class.getDeclaredMethod("test");
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(method.toString());
        accessAnnotationChecker.getSecurityTarget(method);
    }

    @Test
    public void should_ReturnEnclosingClassAsSecurityTarget_When_NoSecurityAnnotationsPresent()
            throws Exception {
        class Test {
            public void test() {
            }
        }
        assertEquals(Test.class, accessAnnotationChecker
                .getSecurityTarget(Test.class.getMethod("test")));
    }

    @Test
    public void should_ReturnEnclosingClassAsSecurityTarget_When_OnlyClassHasSecurityAnnotations()
            throws Exception {
        @AnonymousAllowed
        class Test {
            public void test() {
            }
        }
        assertEquals(Test.class, accessAnnotationChecker
                .getSecurityTarget(Test.class.getMethod("test")));
    }

    @Test
    public void should_ReturnMethodAsSecurityTarget_When_OnlyMethodHasSecurityAnnotations()
            throws Exception {
        class Test {
            @AnonymousAllowed
            public void test() {
            }
        }
        Method securityMethod = Test.class.getMethod("test");
        assertEquals(securityMethod,
                accessAnnotationChecker.getSecurityTarget(securityMethod));
    }

    @Test
    public void should_ReturnMethodAsSecurityTarget_When_BothClassAndMethodHaveSecurityAnnotations()
            throws Exception {
        @AnonymousAllowed
        class Test {
            @AnonymousAllowed
            public void test() {
            }
        }
        Method securityMethod = Test.class.getMethod("test");
        assertEquals(securityMethod,
                accessAnnotationChecker.getSecurityTarget(securityMethod));
    }

    @Test
    public void specialViewsMustBeAccessible() {
        CurrentInstance.set(VaadinRequest.class,
                new VaadinServletRequest(createRequest(null), null));
        Assert.assertTrue(
                accessAnnotationChecker.hasAccess(ClientViewPlaceholder.class));
        Assert.assertTrue(
                accessAnnotationChecker.hasAccess(InternalServerError.class));
        Assert.assertTrue(
                accessAnnotationChecker.hasAccess(RouteNotFoundError.class));
    }

    @Test
    public void anonymousAccessAllowed() throws Exception {
        HttpServletRequest anonRequest = createRequest(null);

        verifyMethodAccessAllowed(AnonymousAllowedClass.class, anonRequest,
                "noAnnotation", "anonymousAllowed");
        verifyMethodAccessAllowed(DenyAllClass.class, anonRequest,
                "anonymousAllowed");
        verifyMethodAccessAllowed(NoAnnotationClass.class, anonRequest,
                "anonymousAllowed");
        verifyMethodAccessAllowed(PermitAllClass.class, anonRequest,
                "anonymousAllowed");
        verifyMethodAccessAllowed(RolesAllowedAdminClass.class, anonRequest,
                "anonymousAllowed");
        verifyMethodAccessAllowed(RolesAllowedUserClass.class, anonRequest,
                "anonymousAllowed");
        // Class level access
        verifyClassAccessAllowed(AnonymousAllowedClass.class, anonRequest,
                true);
        verifyClassAccessAllowed(DenyAllClass.class, anonRequest, false);
        verifyClassAccessAllowed(NoAnnotationClass.class, anonRequest, false);
        verifyClassAccessAllowed(PermitAllClass.class, anonRequest, false);
        verifyClassAccessAllowed(RolesAllowedAdminClass.class, anonRequest,
                false);
        verifyClassAccessAllowed(RolesAllowedUserClass.class, anonRequest,
                false);
    }

    @Test
    public void loggedInUserAccessAllowed() throws Exception {
        HttpServletRequest loggedInURequest = createRequest(USER_PRINCIPAL);

        verifyMethodAccessAllowed(AnonymousAllowedClass.class, loggedInURequest,
                "noAnnotation", "anonymousAllowed", "permitAll");
        verifyMethodAccessAllowed(DenyAllClass.class, loggedInURequest,
                "anonymousAllowed", "permitAll");
        verifyMethodAccessAllowed(NoAnnotationClass.class, loggedInURequest,
                "anonymousAllowed", "permitAll");
        verifyMethodAccessAllowed(PermitAllClass.class, loggedInURequest,
                "noAnnotation", "anonymousAllowed", "permitAll");
        verifyMethodAccessAllowed(RolesAllowedAdminClass.class,
                loggedInURequest, "anonymousAllowed", "permitAll");
        verifyMethodAccessAllowed(RolesAllowedUserClass.class, loggedInURequest,
                "anonymousAllowed", "permitAll");
        // Class level access
        verifyClassAccessAllowed(AnonymousAllowedClass.class, loggedInURequest,
                true);
        verifyClassAccessAllowed(DenyAllClass.class, loggedInURequest, false);
        verifyClassAccessAllowed(NoAnnotationClass.class, loggedInURequest,
                false);
        verifyClassAccessAllowed(PermitAllClass.class, loggedInURequest, true);
        verifyClassAccessAllowed(RolesAllowedAdminClass.class, loggedInURequest,
                false);
        verifyClassAccessAllowed(RolesAllowedUserClass.class, loggedInURequest,
                false);
    }

    @Test
    public void userRoleAccessAllowed() throws Exception {
        HttpServletRequest userRoleRequest = createRequest(USER_PRINCIPAL,
                "user");

        verifyMethodAccessAllowed(AnonymousAllowedClass.class, userRoleRequest,
                "noAnnotation", "anonymousAllowed", "permitAll",
                "rolesAllowedUser", "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(DenyAllClass.class, userRoleRequest,
                "anonymousAllowed", "permitAll", "rolesAllowedUser",
                "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(NoAnnotationClass.class, userRoleRequest,
                "anonymousAllowed", "permitAll", "rolesAllowedUser",
                "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(PermitAllClass.class, userRoleRequest,
                "noAnnotation", "anonymousAllowed", "permitAll",
                "rolesAllowedUser", "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(RolesAllowedAdminClass.class, userRoleRequest,
                "anonymousAllowed", "permitAll", "rolesAllowedUser",
                "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(RolesAllowedUserClass.class, userRoleRequest,
                "noAnnotation", "anonymousAllowed", "permitAll",
                "rolesAllowedUser", "rolesAllowedUserAdmin");
        // Class level access
        verifyClassAccessAllowed(AnonymousAllowedClass.class, userRoleRequest,
                true);
        verifyClassAccessAllowed(DenyAllClass.class, userRoleRequest, false);
        verifyClassAccessAllowed(NoAnnotationClass.class, userRoleRequest,
                false);
        verifyClassAccessAllowed(PermitAllClass.class, userRoleRequest, true);
        verifyClassAccessAllowed(RolesAllowedAdminClass.class, userRoleRequest,
                false);
        verifyClassAccessAllowed(RolesAllowedUserClass.class, userRoleRequest,
                true);
    }

    @Test
    public void userAndAdminRoleAccessAllowed() throws Exception {
        HttpServletRequest adminRoleRequest = createRequest(USER_PRINCIPAL,
                "user", "admin");

        // Method level access

        verifyMethodAccessAllowed(AnonymousAllowedClass.class, adminRoleRequest,
                "noAnnotation", "anonymousAllowed", "permitAll",
                "rolesAllowedUser", "rolesAllowedAdmin",
                "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(DenyAllClass.class, adminRoleRequest,
                "anonymousAllowed", "permitAll", "rolesAllowedUser",
                "rolesAllowedAdmin", "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(NoAnnotationClass.class, adminRoleRequest,
                "anonymousAllowed", "permitAll", "rolesAllowedUser",
                "rolesAllowedAdmin", "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(PermitAllClass.class, adminRoleRequest,
                "noAnnotation", "anonymousAllowed", "permitAll",
                "rolesAllowedUser", "rolesAllowedAdmin",
                "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(RolesAllowedAdminClass.class,
                adminRoleRequest, "noAnnotation", "anonymousAllowed",
                "permitAll", "rolesAllowedUser", "rolesAllowedAdmin",
                "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(RolesAllowedUserClass.class, adminRoleRequest,
                "noAnnotation", "anonymousAllowed", "permitAll",
                "rolesAllowedUser", "rolesAllowedAdmin",
                "rolesAllowedUserAdmin");

        // Class level access
        verifyClassAccessAllowed(AnonymousAllowedClass.class, adminRoleRequest,
                true);
        verifyClassAccessAllowed(DenyAllClass.class, adminRoleRequest, false);
        verifyClassAccessAllowed(NoAnnotationClass.class, adminRoleRequest,
                false);
        verifyClassAccessAllowed(PermitAllClass.class, adminRoleRequest, true);
        verifyClassAccessAllowed(RolesAllowedAdminClass.class, adminRoleRequest,
                true);
        verifyClassAccessAllowed(RolesAllowedUserClass.class, adminRoleRequest,
                true);
    }

    @Test
    public void adminRoleAccessAllowed() throws Exception {
        HttpServletRequest adminRoleRequest = createRequest(USER_PRINCIPAL,
                "admin");

        verifyMethodAccessAllowed(AnonymousAllowedClass.class, adminRoleRequest,
                "noAnnotation", "anonymousAllowed", "permitAll",
                "rolesAllowedAdmin", "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(DenyAllClass.class, adminRoleRequest,
                "anonymousAllowed", "permitAll", "rolesAllowedAdmin",
                "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(NoAnnotationClass.class, adminRoleRequest,
                "anonymousAllowed", "permitAll", "rolesAllowedAdmin",
                "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(PermitAllClass.class, adminRoleRequest,
                "noAnnotation", "anonymousAllowed", "permitAll",
                "rolesAllowedAdmin", "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(RolesAllowedAdminClass.class,
                adminRoleRequest, "noAnnotation", "anonymousAllowed",
                "permitAll", "rolesAllowedAdmin", "rolesAllowedUserAdmin");
        verifyMethodAccessAllowed(RolesAllowedUserClass.class, adminRoleRequest,
                "anonymousAllowed", "permitAll", "rolesAllowedAdmin",
                "rolesAllowedUserAdmin");

        // Class level access
        verifyClassAccessAllowed(AnonymousAllowedClass.class, adminRoleRequest,
                true);
        verifyClassAccessAllowed(DenyAllClass.class, adminRoleRequest, false);
        verifyClassAccessAllowed(NoAnnotationClass.class, adminRoleRequest,
                false);
        verifyClassAccessAllowed(PermitAllClass.class, adminRoleRequest, true);
        verifyClassAccessAllowed(RolesAllowedAdminClass.class, adminRoleRequest,
                true);
        verifyClassAccessAllowed(RolesAllowedUserClass.class, adminRoleRequest,
                false);
    }

    @Test(expected = IllegalStateException.class)
    public void hasClassAccessNoCurrentRequest() {
        CurrentInstance.clearAll();
        accessAnnotationChecker.hasAccess(AnonymousAllowedClass.class);
    }

    @Test(expected = IllegalStateException.class)
    public void hasMethodAccessNoCurrentRequest() throws Exception {
        CurrentInstance.clearAll();
        accessAnnotationChecker
                .hasAccess(AnonymousAllowedClass.class.getMethod("permitAll"));
    }

    @Test
    public void hasClassAccessUsingCurrentRequest() {
        try {
            CurrentInstance.set(VaadinRequest.class, new VaadinServletRequest(
                    createRequest(USER_PRINCIPAL), null));
            Assert.assertTrue(
                    accessAnnotationChecker.hasAccess(PermitAllClass.class));
        } finally {
            CurrentInstance.clearAll();
        }
    }

    @Test
    public void hasMethodAccessUsingCurrentRequest() throws Exception {
        try {
            CurrentInstance.set(VaadinRequest.class, new VaadinServletRequest(
                    createRequest(USER_PRINCIPAL), null));
            Assert.assertTrue(accessAnnotationChecker
                    .hasAccess(PermitAllClass.class.getMethod("permitAll")));
        } finally {
            CurrentInstance.clearAll();
        }
    }

    static HttpServletRequest createRequest(Principal userPrincipal,
            String... roles) {
        Set<String> roleSet = new HashSet<>();
        Collections.addAll(roleSet, roles);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getUserPrincipal()).thenReturn(userPrincipal);
        Mockito.when(request.isUserInRole(Mockito.anyString()))
                .thenAnswer(query -> {
                    return roleSet.contains(query.getArguments()[0]);
                });
        Mockito.when(request.getRequestURL())
                .thenReturn(new StringBuffer(REQUEST_URL));
        return request;
    }

    private void verifyMethodAccessAllowed(Class<?> endpointClass,
            HttpServletRequest request, String... expectedAccessibleMethods)
            throws Exception {
        List<String> expectedAnonList = Arrays
                .asList(expectedAccessibleMethods);
        for (String endpointMethod : ENDPOINT_METHODS) {
            boolean expectedResult = expectedAnonList.contains(endpointMethod);
            Method method = endpointClass.getMethod(endpointMethod);
            Assert.assertEquals("Expected " + endpointClass.getSimpleName()
                    + "." + endpointMethod + " to "
                    + (expectedResult ? "be" : "NOT to be") + " accessible",
                    expectedResult,
                    accessAnnotationChecker.hasAccess(method, request));
        }
    }

    private void verifyClassAccessAllowed(Class<?> cls,
            HttpServletRequest request, boolean expectedResult)
            throws Exception {
        Assert.assertEquals(
                "Expected " + cls.getSimpleName() + " to "
                        + (expectedResult ? "be" : "NOT to be") + " accessible",
                expectedResult,
                accessAnnotationChecker.hasAccess(cls, request));
    }

}
