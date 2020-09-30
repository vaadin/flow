package com.vaadin.flow.server.connect.auth;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.security.Principal;

import com.vaadin.flow.server.VaadinService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class VaadinConnectAccessCheckerTest {
    private static final String ROLE_USER = "ROLE_USER";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private VaadinConnectAccessChecker checker;
    private HttpServletRequest requestMock;
    private HttpSession sessionMock;

    @Before
    public void before() {
        checker = new VaadinConnectAccessChecker();
        requestMock = mock(HttpServletRequest.class);
        sessionMock = mock(HttpSession.class);
        when(sessionMock.getAttribute(VaadinService.getCsrfTokenAttributeName()))
                .thenReturn("Vaadin CCDM");
        when(requestMock.getSession(false)).thenReturn(sessionMock);
        when(requestMock.getUserPrincipal()).thenReturn(mock(Principal.class));
        when(requestMock.getHeader("X-CSRF-Token"))
                .thenReturn("Vaadin CCDM");
        when(requestMock.isUserInRole("ROLE_USER")).thenReturn(true);
    }

    private void createAnonymousContext() {
        when(requestMock.getUserPrincipal()).thenReturn(null);
    }

    private void createDifferentSessionToken() {
        when(sessionMock.getAttribute(VaadinService.getCsrfTokenAttributeName()))
                .thenReturn("CCDM Token");
    }

    private void createNullTokenContextInHeaderRequest() {
        when(requestMock.getHeader("X-CSRF-Token"))
                .thenReturn(null);
    }

    private void createNullTokenSession() {
        when(sessionMock.getAttribute(VaadinService.getCsrfTokenAttributeName())).thenReturn(null);
    }

    private void createNullSession() {
        when(requestMock.getSession(false)).thenReturn(null);
        when(requestMock.getSession()).thenReturn(null);
    }

    private void shouldPass(Class<?> test) throws Exception {
        Method method = test.getMethod("test");
        assertNull(checker.check(method, requestMock));
    }

    private void shouldFail(Class<?> test) throws Exception {
        Method method = test.getMethod("test");
        assertNotNull(checker.check(method, requestMock));
    }

    @Test
    public void should_fail_When_not_having_token_in_headerRequest() throws Exception {
        class Test {
            public void test() {
            }
        }
        createNullTokenContextInHeaderRequest();
        shouldFail(Test.class);
    }

    @Test
    public void should_fail_When_not_having_token_in_session_but_have_token_in_request_header() throws Exception {
        class Test {
            public void test() {
            }
        }
        createNullTokenSession();
        shouldFail(Test.class);
    }

    @Test
    public void should_fail_When_not_having_token_in_session_but_have_token_in_request_header_And_AnonymousAllowed() throws Exception {
        @AnonymousAllowed
        class Test {
            public void test() {
            }
        }
        createNullTokenSession();
        shouldFail(Test.class);
    }

    @Test
    public void should_pass_When_not_having_session_And_not_having_token_in_request_header() throws Exception {
        class Test {
            public void test() {
            }
        }
        createNullSession();
        createNullTokenContextInHeaderRequest();
        shouldPass(Test.class);
    }

    @Test
    public void should_pass_When_not_having_session_And_not_having_token_in_request_header_And_AnonymousAllowed() throws Exception {
        @AnonymousAllowed
        class Test {
            public void test() {
            }
        }
        createNullSession();
        createNullTokenContextInHeaderRequest();
        shouldPass(Test.class);
    }

    @Test
    public void should_pass_When_csrf_disabled() throws Exception {
        class Test {
            public void test() {
            }
        }
        createNullTokenContextInHeaderRequest();
        checker.enableCsrf(false);
        shouldPass(Test.class);
    }

    @Test
    public void should_fail_When_having_different_token_between_session_and_headerRequest() throws Exception {
        class Test {
            public void test() {
            }
        }
        createDifferentSessionToken();
        shouldFail(Test.class);
    }

    @Test
    public void should_fail_When_having_different_token_between_session_and_headerRequest_and_NoAuthentication_AnonymousAllowed() throws Exception {
        class Test {
            @AnonymousAllowed
            public void test() {
            }
        }
        createAnonymousContext();
        createDifferentSessionToken();
        shouldFail(Test.class);
    }

    @Test
    public void should_Fail_When_NoAuthentication() throws Exception {
        class Test {
            public void test() {
            }
        }
        createAnonymousContext();
        shouldFail(Test.class);
    }

    @Test
    public void should_Pass_When_Authentication_And_matching_token() throws Exception {
        class Test {
            public void test() {
            }
        }
        shouldPass(Test.class);
    }

    @Test
    public void should_Fail_When_DenyAllClass() throws Exception {
        @DenyAll
        class Test {
            public void test() {
            }
        }
        shouldFail(Test.class);
    }

    @Test()
    public void should_Pass_When_DenyAllClass_ValidRoleMethod()
            throws Exception {
        @DenyAll
        class Test {
            @RolesAllowed(ROLE_USER)
            public void test() {
            }
        }
        shouldPass(Test.class);
    }

    @Test()
    public void should_Pass_When_DenyAllClass_PermitAllMethod()
            throws Exception {
        @DenyAll
        class Test {
            @PermitAll
            public void test() {
            }
        }
        shouldPass(Test.class);
    }

    @Test()
    public void should_Fail_When_InvalidRoleClass() throws Exception {
        @RolesAllowed({ "ROLE_ADMIN" })
        class Test {
            public void test() {
            }
        }
        shouldFail(Test.class);
    }

    @Test()
    public void should_Pass_When_InvalidRoleClass_ValidRoleMethod()
            throws Exception {
        @RolesAllowed({ "ROLE_ADMIN" })
        class Test {
            @RolesAllowed(ROLE_USER)
            public void test() {
            }
        }
        shouldPass(Test.class);
    }

    @Test()
    public void should_Pass_When_InvalidRoleClass_PermitAllMethod()
            throws Exception {
        @RolesAllowed({ "ROLE_ADMIN" })
        class Test {
            @PermitAll
            public void test() {
            }
        }
        shouldPass(Test.class);
    }

    @Test()
    public void should_Pass_When_ValidRoleClass() throws Exception {
        @RolesAllowed(ROLE_USER)
        class Test {
            public void test() {
            }
        }
        shouldPass(Test.class);
    }

    @Test
    public void should_AllowAnonymousAccess_When_ClassIsAnnotated()
            throws Exception {
        @AnonymousAllowed
        class Test {
            public void test() {
            }
        }

        createAnonymousContext();
        shouldPass(Test.class);
    }

    @Test
    public void should_AllowAnonymousAccess_When_MethodIsAnnotated()
            throws Exception {
        class Test {
            @AnonymousAllowed
            public void test() {
            }
        }
        createAnonymousContext();
        shouldPass(Test.class);
    }

    @Test
    public void should_NotAllowAnonymousAccess_When_NoAnnotationsPresent()
            throws Exception {
        class Test {
            public void test() {
            }
        }
        createAnonymousContext();
        shouldFail(Test.class);
    }

    @Test
    public void should_AllowAnyAuthenticatedAccess_When_PermitAllAndAnonymousAllowed()
            throws Exception {
        class Test {
            @PermitAll
            @AnonymousAllowed
            public void test() {
            }
        }
        shouldPass(Test.class);
    }

    @Test
    public void should_AllowAnonymousAccess_When_PermitAllAndAnonymousAllowed()
            throws Exception {
        class Test {
            @PermitAll
            @AnonymousAllowed
            public void test() {
            }
        }

        createAnonymousContext();
        shouldPass(Test.class);
    }

    @Test
    public void should_AllowAnyAuthenticatedAccess_When_RolesAllowedAndAnonymousAllowed()
            throws Exception {
        class Test {
            @RolesAllowed("ADMIN")
            @AnonymousAllowed
            public void test() {
            }
        }
        shouldPass(Test.class);
    }

    @Test
    public void should_AllowAnonymousAccess_When_RolesAllowedAndAnonymousAllowed()
            throws Exception {
        class Test {
            @RolesAllowed("ADMIN")
            @AnonymousAllowed
            public void test() {
            }
        }
        createAnonymousContext();
        shouldPass(Test.class);
    }

    @Test
    public void should_DisallowAnyAuthenticatedAccess_When_DenyAllAndAnonymousAllowed()
            throws Exception {
        class Test {
            @DenyAll
            @AnonymousAllowed
            public void test() {
            }
        }
        shouldFail(Test.class);
    }

    @Test
    public void should_DisallowNotMatchingRoleAccess_When_RolesAllowedAndPermitAll()
            throws Exception {
        class Test {
            @RolesAllowed("ADMIN")
            @PermitAll
            public void test() {
            }
        }
        shouldFail(Test.class);
    }

    @Test
    public void should_AllowSpecificRoleAccess_When_RolesAllowedAndPermitAll()
            throws Exception {
        class Test {
            @RolesAllowed(ROLE_USER)
            @PermitAll
            public void test() {
            }
        }
        shouldPass(Test.class);
    }

    @Test
    public void should_DisallowAnonymousAccess_When_DenyAllAndAnonymousAllowed()
            throws Exception {
        class Test {
            @DenyAll
            @AnonymousAllowed
            public void test() {
            }
        }
        createAnonymousContext();
        shouldFail(Test.class);
    }

    @Test
    public void should_DisallowAnonymousAccess_When_AnonymousAllowedIsOverriddenWithDenyAll()
            throws Exception {
        @AnonymousAllowed
        class Test {
            @DenyAll
            public void test() {
            }
        }

        createAnonymousContext();
        shouldFail(Test.class);
    }

    @Test
    public void should_DisallowAnonymousAccess_When_AnonymousAllowedIsOverriddenWithRolesAllowed()
            throws Exception {
        @AnonymousAllowed
        class Test {
            @RolesAllowed(ROLE_USER)
            public void test() {
            }
        }

        createAnonymousContext();
        shouldFail(Test.class);
    }

    @Test
    public void should_DisallowAnonymousAccess_When_AnonymousAllowedIsOverriddenWithPermitAll()
            throws Exception {
        @AnonymousAllowed
        class Test {
            @PermitAll
            public void test() {
            }
        }

        createAnonymousContext();
        shouldFail(Test.class);
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
        checker.getSecurityTarget(method);
    }

    @Test
    public void should_ReturnEnclosingClassAsSecurityTarget_When_NoSecurityAnnotationsPresent()
            throws Exception {
        class Test {
            public void test() {
            }
        }
        assertEquals(Test.class,
                checker.getSecurityTarget(Test.class.getMethod("test")));
    }

    @Test
    public void should_ReturnEnclosingClassAsSecurityTarget_When_OnlyClassHasSecurityAnnotations()
            throws Exception {
        @AnonymousAllowed
        class Test {
            public void test() {
            }
        }
        assertEquals(Test.class,
                checker.getSecurityTarget(Test.class.getMethod("test")));
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
        assertEquals(securityMethod, checker.getSecurityTarget(securityMethod));
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
        assertEquals(securityMethod, checker.getSecurityTarget(securityMethod));
    }
}
