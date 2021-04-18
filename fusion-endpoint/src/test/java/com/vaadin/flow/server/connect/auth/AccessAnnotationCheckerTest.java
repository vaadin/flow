package com.vaadin.flow.server.connect.auth;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AccessAnnotationCheckerTest {
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

}
