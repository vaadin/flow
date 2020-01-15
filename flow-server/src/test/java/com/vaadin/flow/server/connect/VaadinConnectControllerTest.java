package com.vaadin.flow.server.connect;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.vaadin.flow.server.connect.auth.AnonymousAllowed;
import com.vaadin.flow.server.connect.auth.VaadinConnectAccessChecker;
import com.vaadin.flow.server.connect.exception.VaadinConnectException;
import com.vaadin.flow.server.connect.exception.VaadinConnectValidationException;
import com.vaadin.flow.server.connect.testservice.BridgeMethodTestService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VaadinConnectControllerTest {
    private static final TestClass TEST_SERVICE = new TestClass();
    private static final String TEST_SERVICE_NAME = TEST_SERVICE.getClass()
            .getSimpleName();
    private static final Method TEST_METHOD;
    private static final Method TEST_VALIDATION_METHOD;
    private HttpServletRequest requestMock;

    static {
        TEST_METHOD = Stream.of(TEST_SERVICE.getClass().getDeclaredMethods())
                .filter(method -> "testMethod".equals(method.getName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "Failed to find a test service method"));
        TEST_VALIDATION_METHOD = Stream
                .of(TEST_SERVICE.getClass().getDeclaredMethods())
                .filter(method -> "testValidationMethod"
                        .equals(method.getName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "Failed to find a test validation service method"));
    }

    private static class TestValidationParameter {
        @Min(10)
        private final int count;

        public TestValidationParameter(@JsonProperty("count") int count) {
            this.count = count;
        }
    }

    @VaadinService
    public static class TestClass {
        public String testMethod(int parameter) {
            return parameter + "-test";
        }

        public void testValidationMethod(
                @NotNull TestValidationParameter parameter) {
            // no op
        }

        public void testMethodWithMultipleParameter(int number, String text,
                Date date) {
            // no op
        }

        @AnonymousAllowed
        public String testAnonymousMethod() {
            return "Hello, anonymous user!";
        }

        @PermitAll
        @RolesAllowed({"FOO_ROLE", "BAR_ROLE"})
        public String testRoleAllowed() {
            return "Hello, user in role!";
        }

        @DenyAll
        @AnonymousAllowed
        public void denyAll() {
        }

        @RolesAllowed("FOO_ROLE")
        @AnonymousAllowed
        public String anonymousOverrides() {
            return "Hello, no user!";
        }
    }

    @VaadinService("CustomService")
    public static class TestClassWithCustomServiceName {
        public String testMethod(int parameter) {
            return parameter + "-test";
        }
    }

    @VaadinService("my service")
    public static class TestClassWithIllegalServiceName {
        public String testMethod(int parameter) {
            return parameter + "-test";
        }
    }

    @VaadinService
    public static class NullCheckerTestClass {
        public static final String OK_RESPONSE = "ok";

        public String testOkMethod() {
            return OK_RESPONSE;
        }

        public String testNullMethod() {
            return null;
        }
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        requestMock = mock(HttpServletRequest.class);
        when(requestMock.getUserPrincipal()).thenReturn(mock(Principal.class));
        when(requestMock.getHeader("X-CSRF-Token")).thenReturn("Vaadin CCDM");

        HttpSession sessionMock = mock(HttpSession.class);
        when(sessionMock.getAttribute(com.vaadin.flow.server.VaadinService.getCsrfTokenAttributeName()))
                .thenReturn("Vaadin CCDM");
        when(requestMock.getSession()).thenReturn(sessionMock);
    }

    @Test
    public void should_ThrowException_When_NoServiceNameCanBeReceived() {
        TestClass anonymousClass = new TestClass() {
        };
        assertEquals("Service to test should have no name",
                anonymousClass.getClass().getSimpleName(), "");

        exception.expect(IllegalStateException.class);
        exception.expectMessage("anonymous");
        exception.expectMessage(anonymousClass.getClass().getName());
        createVaadinController(anonymousClass);
    }

    @Test
    public void should_ThrowException_When_IncorrectServiceNameProvided() {
        TestClassWithIllegalServiceName serviceWithIllegalName = new TestClassWithIllegalServiceName();
        String incorrectName = serviceWithIllegalName.getClass()
                .getAnnotation(VaadinService.class).value();
        VaadinServiceNameChecker nameChecker = new VaadinServiceNameChecker();
        String expectedCheckerMessage = nameChecker.check(incorrectName);
        assertNotNull(expectedCheckerMessage);

        exception.expect(IllegalStateException.class);
        exception.expectMessage(incorrectName);
        exception.expectMessage(expectedCheckerMessage);

        createVaadinController(serviceWithIllegalName, mock(ObjectMapper.class),
                null, nameChecker, null);
    }

    @Test
    public void should_Return404_When_ServiceNotFound() {
        String missingServiceName = "whatever";
        assertNotEquals(missingServiceName, TEST_SERVICE_NAME);

        ResponseEntity<?> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(missingServiceName, null, null, requestMock);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void should_Return404_When_MethodNotFound() {
        String missingServiceMethod = "whatever";
        assertNotEquals(TEST_METHOD.getName(), missingServiceMethod);

        ResponseEntity<?> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(TEST_SERVICE_NAME, missingServiceMethod,
                        null, requestMock);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void should_Return404_When_IllegalAccessToMethodIsPerformed() {
        String accessErrorMessage = "Access error";

        VaadinConnectAccessChecker restrictingCheckerMock = mock(
                VaadinConnectAccessChecker.class);
        when(restrictingCheckerMock.check(TEST_METHOD, requestMock))
                .thenReturn(accessErrorMessage);

        VaadinServiceNameChecker nameCheckerMock = mock(
                VaadinServiceNameChecker.class);
        when(nameCheckerMock.check(TEST_SERVICE_NAME)).thenReturn(null);

        ExplicitNullableTypeChecker explicitNullableTypeCheckerMock = mock(
                ExplicitNullableTypeChecker.class);

        ResponseEntity<String> response = createVaadinController(TEST_SERVICE,
                new ObjectMapper(), restrictingCheckerMock, nameCheckerMock,
                explicitNullableTypeCheckerMock)
                        .serveVaadinService(TEST_SERVICE_NAME,
                                TEST_METHOD.getName(), null, requestMock);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        String responseBody = response.getBody();
        assertServiceInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(accessErrorMessage));

        verify(restrictingCheckerMock, only()).check(TEST_METHOD, requestMock);
        verify(restrictingCheckerMock, times(1)).check(TEST_METHOD, requestMock);
    }

    @Test
    public void should_Return400_When_LessParametersSpecified1() {
        ResponseEntity<String> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(TEST_SERVICE_NAME, TEST_METHOD.getName(),
                        null, requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertServiceInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains("0"));
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(
                        Integer.toString(TEST_METHOD.getParameterCount())));
    }

    @Test
    public void should_Return400_When_MoreParametersSpecified() {
        ResponseEntity<String> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(TEST_SERVICE_NAME, TEST_METHOD.getName(),
                        createRequestParameters(
                                "{\"value1\": 222, \"value2\": 333}"), requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertServiceInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains("2"));
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(
                        Integer.toString(TEST_METHOD.getParameterCount())));
    }

    @Test
    public void should_Return400_When_IncorrectParameterTypesAreProvided() {
        ResponseEntity<String> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(TEST_SERVICE_NAME, TEST_METHOD.getName(),
                        createRequestParameters("{\"value\": [222]}"), requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertServiceInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(
                        TEST_METHOD.getParameterTypes()[0].getSimpleName()));
    }

    @Test
    public void should_NotCallMethod_When_UserPrincipalIsNull() {
        VaadinConnectController vaadinController = createVaadinControllerWithoutPrincipal();
        ResponseEntity<String> response = vaadinController.serveVaadinService(
                TEST_SERVICE_NAME, TEST_METHOD.getName(),
                createRequestParameters("{\"value\": 222}"), requestMock);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        String responseBody = response.getBody();
        assertNotNull("Response body should not be null", responseBody);
        assertTrue("Should return unauthorized error",
                responseBody.contains("Anonymous access is not allowed"));
    }

    @Test
    public void should_CallMethodAnonymously_When_UserPrincipalIsNullAndAnonymousAllowed() {
        VaadinConnectController vaadinController = createVaadinControllerWithoutPrincipal();
        ResponseEntity<String> response = vaadinController.serveVaadinService(
                TEST_SERVICE_NAME, "testAnonymousMethod",
                createRequestParameters("{}"), requestMock);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String responseBody = response.getBody();
        assertEquals("Should return message when calling anonymously",
                "\"Hello, anonymous user!\"", responseBody);
    }

    @Test
    public void should_NotCallMethod_When_a_CSRF_request() {
        when(requestMock.getHeader("X-CSRF-Token")).thenReturn(null);

        VaadinConnectController vaadinController = createVaadinControllerWithoutPrincipal();
        ResponseEntity<String> response = vaadinController.serveVaadinService(
                TEST_SERVICE_NAME, "testAnonymousMethod",
                createRequestParameters("{}"), requestMock);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        String responseBody = response.getBody();
        assertNotNull("Response body should not be null", responseBody);
        assertTrue("Should return unauthorized error",
                responseBody.contains("Anonymous access is not allowed"));
    }

    @Test
    public void should_NotCallMethodAnonymously_When_UserPrincipalIsNotInRole() {
        VaadinConnectController vaadinController = createVaadinController(
                TEST_SERVICE, new VaadinConnectAccessChecker());

        ResponseEntity<String> response = vaadinController.serveVaadinService(
                TEST_SERVICE_NAME, "testRoleAllowed",
                createRequestParameters("{}"), requestMock);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("Unauthorized access to vaadin service"));
    }

    @Test
    public void should_CallMethodAnonymously_When_UserPrincipalIsInRole() {
        when(requestMock.isUserInRole("FOO_ROLE")).thenReturn(true);

        VaadinConnectController vaadinController = createVaadinController(
                TEST_SERVICE, new VaadinConnectAccessChecker());

        ResponseEntity<String> response = vaadinController.serveVaadinService(
                TEST_SERVICE_NAME, "testRoleAllowed",
                createRequestParameters("{}"), requestMock);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals("\"Hello, user in role!\"", response.getBody());
    }


    @Test
    public void should_CallMethodAnonymously_When_AnonymousOverridesRoles() {
        VaadinConnectController vaadinController = createVaadinController(
                TEST_SERVICE, new VaadinConnectAccessChecker());

        ResponseEntity<String> response = vaadinController.serveVaadinService(
                TEST_SERVICE_NAME, "anonymousOverrides",
                createRequestParameters("{}"), requestMock);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("\"Hello, no user!\"", response.getBody());
    }

    @Test
    public void should_NotCallMethod_When_DenyAll() {
        VaadinConnectController vaadinController = createVaadinControllerWithoutPrincipal();
        ResponseEntity<String> response = vaadinController.serveVaadinService(
                TEST_SERVICE_NAME, "denyAll",
                createRequestParameters("{}"), requestMock);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("Anonymous access is not allowed"));
    }

    @Test
    @Ignore("requires mockito version with plugin for final classes")
    public void should_Return400_When_ServiceMethodThrowsIllegalArgumentException()
            throws Exception {
        int inputValue = 222;

        Method serviceMethodMock = createServiceMethodMockThatThrows(inputValue,
                new IllegalArgumentException("OOPS"));

        VaadinConnectController controller = createVaadinController(
                TEST_SERVICE);
        controller.vaadinServices.get(TEST_SERVICE_NAME.toLowerCase()).methods
                .put(TEST_METHOD.getName().toLowerCase(), serviceMethodMock);

        ResponseEntity<String> response = controller.serveVaadinService(
                TEST_SERVICE_NAME, TEST_METHOD.getName(),
                createRequestParameters(
                        String.format("{\"value\": %s}", inputValue)), requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertServiceInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(
                        TEST_METHOD.getParameterTypes()[0].getSimpleName()));

        verify(serviceMethodMock, times(1)).invoke(TEST_SERVICE, inputValue);
        verify(serviceMethodMock, times(1)).getParameters();
    }

    @Test
    @Ignore("requires mockito version with plugin for final classes")
    public void should_Return500_When_ServiceMethodThrowsIllegalAccessException()
            throws Exception {
        int inputValue = 222;

        Method serviceMethodMock = createServiceMethodMockThatThrows(inputValue,
                new IllegalAccessException("OOPS"));

        VaadinConnectController controller = createVaadinController(
                TEST_SERVICE);
        controller.vaadinServices.get(TEST_SERVICE_NAME.toLowerCase()).methods
                .put(TEST_METHOD.getName().toLowerCase(), serviceMethodMock);

        ResponseEntity<String> response = controller.serveVaadinService(
                TEST_SERVICE_NAME, TEST_METHOD.getName(),
                createRequestParameters(
                        String.format("{\"value\": %s}", inputValue)), requestMock);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());
        String responseBody = response.getBody();
        assertServiceInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains("access failure"));

        verify(serviceMethodMock, times(1)).invoke(TEST_SERVICE, inputValue);
        verify(serviceMethodMock, times(1)).getParameters();
    }

    @Test
    @Ignore("requires mockito version with plugin for final classes")
    public void should_Return500_When_ServiceMethodThrowsInvocationTargetException()
            throws Exception {
        int inputValue = 222;

        Method serviceMethodMock = createServiceMethodMockThatThrows(inputValue,
                new InvocationTargetException(
                        new IllegalStateException("OOPS")));

        VaadinConnectController controller = createVaadinController(
                TEST_SERVICE);
        controller.vaadinServices.get(TEST_SERVICE_NAME.toLowerCase()).methods
                .put(TEST_METHOD.getName().toLowerCase(), serviceMethodMock);

        ResponseEntity<String> response = controller.serveVaadinService(
                TEST_SERVICE_NAME, TEST_METHOD.getName(),
                createRequestParameters(
                        String.format("{\"value\": %s}", inputValue)), requestMock);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());
        String responseBody = response.getBody();
        assertServiceInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains("execution failure"));

        verify(serviceMethodMock, times(1)).invoke(TEST_SERVICE, inputValue);
        verify(serviceMethodMock, times(1)).getParameters();
    }

    @Test
    @Ignore("requires mockito version with plugin for final classes")
    public void should_Return400_When_ServiceMethodThrowsVaadinConnectException()
            throws Exception {
        int inputValue = 222;
        String expectedMessage = "OOPS";

        Method serviceMethodMock = createServiceMethodMockThatThrows(inputValue,
                new InvocationTargetException(
                        new VaadinConnectException(expectedMessage)));

        VaadinConnectController controller = createVaadinController(
                TEST_SERVICE);
        controller.vaadinServices.get(TEST_SERVICE_NAME.toLowerCase()).methods
                .put(TEST_METHOD.getName().toLowerCase(), serviceMethodMock);

        ResponseEntity<String> response = controller.serveVaadinService(
                TEST_SERVICE_NAME, TEST_METHOD.getName(),
                createRequestParameters(
                        String.format("{\"value\": %s}", inputValue)), requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(VaadinConnectException.class.getName()));
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(expectedMessage));

        verify(serviceMethodMock, times(1)).invoke(TEST_SERVICE, inputValue);
        verify(serviceMethodMock, times(1)).getParameters();
    }

    @Test
    @Ignore("requires mockito version with plugin for final classes")
    public void should_Return400_When_ServiceMethodThrowsVaadinConnectExceptionSubclass()
            throws Exception {
        int inputValue = 222;
        String expectedMessage = "OOPS";

        class MyCustomException extends VaadinConnectException {
            public MyCustomException() {
                super(expectedMessage);
            }
        }

        Method serviceMethodMock = createServiceMethodMockThatThrows(inputValue,
                new InvocationTargetException(new MyCustomException()));

        VaadinConnectController controller = createVaadinController(
                TEST_SERVICE);
        controller.vaadinServices.get(TEST_SERVICE_NAME.toLowerCase()).methods
                .put(TEST_METHOD.getName().toLowerCase(), serviceMethodMock);

        ResponseEntity<String> response = controller.serveVaadinService(
                TEST_SERVICE_NAME, TEST_METHOD.getName(),
                createRequestParameters(
                        String.format("{\"value\": %s}", inputValue)), requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(MyCustomException.class.getName()));
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(expectedMessage));

        verify(serviceMethodMock, times(1)).invoke(TEST_SERVICE, inputValue);
        verify(serviceMethodMock, times(1)).getParameters();
    }

    @Test
    @Ignore("requires mockito version with plugin for final classes")
    public void should_Return500_When_MapperFailsToSerializeResponse()
            throws Exception {
        ObjectMapper mapperMock = mock(ObjectMapper.class);
        TypeFactory typeFactory = mock(TypeFactory.class);
        when(mapperMock.getTypeFactory()).thenReturn(typeFactory);
        when(typeFactory.constructType(int.class))
                .thenReturn(SimpleType.constructUnsafe(int.class));
        when(mapperMock.readerFor(SimpleType.constructUnsafe(int.class)))
                .thenReturn(new ObjectMapper()
                        .readerFor(SimpleType.constructUnsafe(int.class)));

        ArgumentCaptor<Object> serializingErrorsCapture = ArgumentCaptor
                .forClass(Object.class);
        String expectedError = "expected_error";
        when(mapperMock.writeValueAsString(serializingErrorsCapture.capture()))
                .thenThrow(new JsonMappingException(null, "sss"))
                .thenReturn(expectedError);

        ResponseEntity<String> response = createVaadinController(TEST_SERVICE,
                mapperMock).serveVaadinService(TEST_SERVICE_NAME,
                        TEST_METHOD.getName(),
                        createRequestParameters("{\"value\": 222}"), requestMock);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());
        String responseBody = response.getBody();
        assertEquals(expectedError, responseBody);

        List<Object> passedErrors = serializingErrorsCapture.getAllValues();
        assertEquals(2, passedErrors.size());
        String lastError = passedErrors.get(1).toString();
        assertServiceInfoPresent(lastError);
        assertTrue(String.format("Invalid response body: '%s'", lastError),
                lastError.contains(
                        VaadinConnectController.VAADIN_SERVICE_MAPPER_BEAN_QUALIFIER));

        verify(mapperMock, times(1))
                .readerFor(SimpleType.constructUnsafe(int.class));
        verify(mapperMock, times(2)).writeValueAsString(Mockito.isNotNull());
    }

    @Test
    @Ignore("requires mockito version with plugin for final classes")
    public void should_ThrowException_When_MapperFailsToSerializeEverything()
            throws Exception {
        ObjectMapper mapperMock = mock(ObjectMapper.class);
        TypeFactory typeFactory = mock(TypeFactory.class);
        when(mapperMock.getTypeFactory()).thenReturn(typeFactory);
        when(typeFactory.constructType(int.class))
                .thenReturn(SimpleType.constructUnsafe(int.class));
        when(mapperMock.readerFor(SimpleType.constructUnsafe(int.class)))
                .thenReturn(new ObjectMapper()
                        .readerFor(SimpleType.constructUnsafe(int.class)));
        when(mapperMock.writeValueAsString(Mockito.isNotNull()))
                .thenThrow(new JsonMappingException(null, "sss"));

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Unexpected");
        createVaadinController(TEST_SERVICE, mapperMock).serveVaadinService(
                TEST_SERVICE_NAME, TEST_METHOD.getName(),
                createRequestParameters("{\"value\": 222}"), requestMock);
    }

    @Test
    public void should_ReturnCorrectResponse_When_EverythingIsCorrect() {
        int inputValue = 222;
        String expectedOutput = TEST_SERVICE.testMethod(inputValue);

        ResponseEntity<String> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(TEST_SERVICE_NAME, TEST_METHOD.getName(),
                        createRequestParameters(
                                String.format("{\"value\": %s}", inputValue)), requestMock);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(String.format("\"%s\"", expectedOutput),
                response.getBody());
    }

    @Test
    public void should_ReturnCorrectResponse_When_ServiceClassIsProxied() {

        ApplicationContext contextMock = mock(ApplicationContext.class);
        TestClass service = new TestClass();
        TestClass proxy = mock(TestClass.class, CALLS_REAL_METHODS);
        when(contextMock.getBeansWithAnnotation(VaadinService.class))
                .thenReturn(Collections.singletonMap(
                        service.getClass().getSimpleName(), proxy));

        VaadinConnectController vaadinConnectController = new VaadinConnectController(
                new ObjectMapper(), mock(VaadinConnectAccessChecker.class),
                mock(VaadinServiceNameChecker.class),
                mock(ExplicitNullableTypeChecker.class), contextMock);

        int inputValue = 222;
        String expectedOutput = service.testMethod(inputValue);

        ResponseEntity<String> response = vaadinConnectController
                .serveVaadinService("TestClass", "testMethod",
                        createRequestParameters(
                                String.format("{\"value\": %s}", inputValue)),
                        requestMock);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(String.format("\"%s\"", expectedOutput),
                response.getBody());
    }

    @Test
    public void should_NotUseBridgeMethod_When_ServiceHasBridgeMethodFromInterface() {
        String inputId = "2222";
        String expectedResult = String.format("{\"id\":\"%s\"}", inputId);
        BridgeMethodTestService.InheritedClass testService = new BridgeMethodTestService.InheritedClass();
        String testMethodName = "testMethodFromInterface";
        ResponseEntity<String> response = createVaadinController(testService)
                .serveVaadinService(testService.getClass().getSimpleName(),
                        testMethodName, createRequestParameters(String.format(
                                "{\"value\": {\"id\": \"%s\"}}", inputId)), requestMock);
        assertEquals(expectedResult, response.getBody());
    }

    @Test
    public void should_NotUseBridgeMethod_When_ServiceHasBridgeMethodFromParentClass() {
        String inputId = "2222";
        BridgeMethodTestService.InheritedClass testService = new BridgeMethodTestService.InheritedClass();
        String testMethodName = "testMethodFromClass";

        ResponseEntity<String> response = createVaadinController(testService)
                .serveVaadinService(testService.getClass().getSimpleName(),
                        testMethodName, createRequestParameters(
                                String.format("{\"value\": %s}", inputId)), requestMock);
        assertEquals(inputId, response.getBody());
    }

    @Test
    public void should_ReturnCorrectResponse_When_CallingNormalOverriddenMethod() {
        String inputId = "2222";
        BridgeMethodTestService.InheritedClass testService = new BridgeMethodTestService.InheritedClass();
        String testMethodName = "testNormalMethod";

        ResponseEntity<String> response = createVaadinController(testService)
                .serveVaadinService(testService.getClass().getSimpleName(),
                        testMethodName, createRequestParameters(
                                String.format("{\"value\": %s}", inputId)), requestMock);
        assertEquals(inputId, response.getBody());
    }

    @Test
    public void should_UseCustomServiceName_When_ItIsDefined() {
        int input = 111;
        String expectedOutput = new TestClassWithCustomServiceName()
                .testMethod(input);
        String beanName = TestClassWithCustomServiceName.class.getSimpleName();

        ApplicationContext contextMock = mock(ApplicationContext.class);
        when(contextMock.getBeansWithAnnotation(VaadinService.class))
                .thenReturn(Collections.singletonMap(beanName,
                        new TestClassWithCustomServiceName()));

        VaadinConnectController vaadinConnectController = new VaadinConnectController(
                new ObjectMapper(), mock(VaadinConnectAccessChecker.class),
                mock(VaadinServiceNameChecker.class),
                mock(ExplicitNullableTypeChecker.class), contextMock);
        ResponseEntity<String> response = vaadinConnectController
                .serveVaadinService("CustomService", "testMethod",
                        createRequestParameters(
                                String.format("{\"value\": %s}", input)), requestMock);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(String.format("\"%s\"", expectedOutput),
                response.getBody());
    }

    @Test
    public void should_UseCustomServiceName_When_ServiceClassIsProxied() {

        ApplicationContext contextMock = mock(ApplicationContext.class);
        TestClassWithCustomServiceName service = new TestClassWithCustomServiceName();
        TestClassWithCustomServiceName proxy = mock(
                TestClassWithCustomServiceName.class, CALLS_REAL_METHODS);
        when(contextMock.getBeansWithAnnotation(VaadinService.class))
                .thenReturn(Collections.singletonMap(
                        service.getClass().getSimpleName(), proxy));

        VaadinConnectController vaadinConnectController = new VaadinConnectController(
                new ObjectMapper(), mock(VaadinConnectAccessChecker.class),
                mock(VaadinServiceNameChecker.class),
                mock(ExplicitNullableTypeChecker.class), contextMock);

        int input = 111;
        String expectedOutput = service.testMethod(input);

        ResponseEntity<String> response = vaadinConnectController
                .serveVaadinService("CustomService", "testMethod",
                        createRequestParameters(
                                String.format("{\"value\": %s}", input)),
                        requestMock);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(String.format("\"%s\"", expectedOutput),
                response.getBody());
    }

    @Test
    public void should_UseDefaultObjectMapper_When_NoneIsProvided() {
        ApplicationContext contextMock = mock(ApplicationContext.class);
        ObjectMapper mockDefaultObjectMapper = mock(ObjectMapper.class);
        JacksonProperties mockJacksonProperties = mock(JacksonProperties.class);
        when(contextMock.getBean(ObjectMapper.class))
                .thenReturn(mockDefaultObjectMapper);
        when(contextMock.getBean(JacksonProperties.class))
                .thenReturn(mockJacksonProperties);
        when(mockJacksonProperties.getVisibility())
                .thenReturn(Collections.emptyMap());
        new VaadinConnectController(null,
                mock(VaadinConnectAccessChecker.class),
                mock(VaadinServiceNameChecker.class),
                mock(ExplicitNullableTypeChecker.class), contextMock);

        verify(contextMock, times(1)).getBean(ObjectMapper.class);
        verify(mockDefaultObjectMapper, times(1)).setVisibility(
                PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        verify(contextMock, times(1)).getBean(JacksonProperties.class);
    }

    @Test
    public void should_NotOverrideVisibility_When_JacksonPropertiesProvideVisibility() {
        ApplicationContext contextMock = mock(ApplicationContext.class);
        ObjectMapper mockDefaultObjectMapper = mock(ObjectMapper.class);
        JacksonProperties mockJacksonProperties = mock(JacksonProperties.class);
        when(contextMock.getBean(ObjectMapper.class))
                .thenReturn(mockDefaultObjectMapper);
        when(contextMock.getBean(JacksonProperties.class))
                .thenReturn(mockJacksonProperties);
        when(mockJacksonProperties.getVisibility())
                .thenReturn(Collections.singletonMap(PropertyAccessor.ALL,
                        JsonAutoDetect.Visibility.PUBLIC_ONLY));
        new VaadinConnectController(null,
                mock(VaadinConnectAccessChecker.class),
                mock(VaadinServiceNameChecker.class),
                mock(ExplicitNullableTypeChecker.class), contextMock);

        verify(contextMock, times(1)).getBean(ObjectMapper.class);
        verify(mockDefaultObjectMapper, times(0)).setVisibility(
                PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        verify(contextMock, times(1)).getBean(JacksonProperties.class);
    }

    @Test
    public void should_ThrowError_When_DefaultObjectMapperIsNotFound() {
        ApplicationContext contextMock = mock(ApplicationContext.class);
        when(contextMock.getBean(ObjectMapper.class))
                .thenThrow(new NoSuchBeanDefinitionException("Bean not found"));

        exception.expect(IllegalStateException.class);
        exception.expectMessage("object mapper");

        new VaadinConnectController(null,
                mock(VaadinConnectAccessChecker.class),
                mock(VaadinServiceNameChecker.class),
                mock(ExplicitNullableTypeChecker.class), contextMock);
    }

    @Test
    public void should_ReturnValidationError_When_DeserializationFails()
            throws IOException {
        String inputValue = "\"string\"";
        String expectedErrorMessage = String.format(
                "Validation error in service '%s' method '%s'",
                TEST_SERVICE_NAME, TEST_METHOD.getName());
        ResponseEntity<String> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(TEST_SERVICE_NAME, TEST_METHOD.getName(),
                        createRequestParameters(
                                String.format("{\"value\": %s}", inputValue)), requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ObjectNode jsonNodes = new ObjectMapper().readValue(response.getBody(),
                ObjectNode.class);

        assertEquals(VaadinConnectValidationException.class.getName(),
                jsonNodes.get("type").asText());
        assertEquals(expectedErrorMessage, jsonNodes.get("message").asText());
        assertEquals(1, jsonNodes.get("validationErrorData").size());

        JsonNode validationErrorData = jsonNodes.get("validationErrorData")
                .get(0);
        assertEquals("value",
                validationErrorData.get("parameterName").asText());
        assertTrue(
                validationErrorData.get("message").asText().contains("'int'"));
    }

    @Test
    public void should_ReturnAllValidationErrors_When_DeserializationFailsForMultipleParameters()
            throws IOException {
        String inputValue = String.format(
                "{\"number\": %s, \"text\": %s, \"date\": %s}",
                "\"NotANumber\"", "\"ValidText\"", "\"NotADate\"");
        String testMethodName = "testMethodWithMultipleParameter";
        String expectedErrorMessage = String.format(
                "Validation error in service '%s' method '%s'",
                TEST_SERVICE_NAME, testMethodName);
        ResponseEntity<String> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(TEST_SERVICE_NAME, testMethodName,
                        createRequestParameters(inputValue), requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ObjectNode jsonNodes = new ObjectMapper().readValue(response.getBody(),
                ObjectNode.class);
        assertNotNull(jsonNodes);

        assertEquals(VaadinConnectValidationException.class.getName(),
                jsonNodes.get("type").asText());
        assertEquals(expectedErrorMessage, jsonNodes.get("message").asText());
        assertEquals(2, jsonNodes.get("validationErrorData").size());

        List<String> parameterNames = jsonNodes.get("validationErrorData")
                .findValuesAsText("parameterName");
        assertEquals(2, parameterNames.size());
        assertTrue(parameterNames.contains("date"));
        assertTrue(parameterNames.contains("number"));
    }

    @Test
    public void should_ReturnValidationError_When_ServiceMethodParameterIsInvalid()
            throws IOException {
        String expectedErrorMessage = String.format(
                "Validation error in service '%s' method '%s'",
                TEST_SERVICE_NAME, TEST_VALIDATION_METHOD.getName());

        ResponseEntity<String> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(TEST_SERVICE_NAME,
                        TEST_VALIDATION_METHOD.getName(),
                        createRequestParameters("{\"parameter\": null}"), requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ObjectNode jsonNodes = new ObjectMapper().readValue(response.getBody(),
                ObjectNode.class);

        assertEquals(VaadinConnectValidationException.class.getName(),
                jsonNodes.get("type").asText());
        assertEquals(expectedErrorMessage, jsonNodes.get("message").asText());
        assertEquals(1, jsonNodes.get("validationErrorData").size());

        JsonNode validationErrorData = jsonNodes.get("validationErrorData")
                .get(0);
        assertTrue(validationErrorData.get("parameterName").asText()
                .contains(TEST_VALIDATION_METHOD.getName()));
        String validationErrorMessage = validationErrorData.get("message")
                .asText();
        assertTrue(validationErrorMessage
                .contains(TEST_VALIDATION_METHOD.getName()));
        assertTrue(validationErrorMessage
                .contains(TEST_SERVICE.getClass().toString()));
        assertTrue(validationErrorMessage.contains("null"));
    }

    @Test
    public void should_ReturnValidationError_When_ServiceMethodBeanIsInvalid()
            throws IOException {
        int invalidPropertyValue = 5;
        String propertyName = "count";
        String expectedErrorMessage = String.format(
                "Validation error in service '%s' method '%s'",
                TEST_SERVICE_NAME, TEST_VALIDATION_METHOD.getName());

        ResponseEntity<String> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(TEST_SERVICE_NAME,
                        TEST_VALIDATION_METHOD.getName(),
                        createRequestParameters(String.format(
                                "{\"parameter\": {\"count\": %d}}",
                                invalidPropertyValue)), requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ObjectNode jsonNodes = new ObjectMapper().readValue(response.getBody(),
                ObjectNode.class);

        assertEquals(VaadinConnectValidationException.class.getName(),
                jsonNodes.get("type").asText());
        assertEquals(expectedErrorMessage, jsonNodes.get("message").asText());
        assertEquals(1, jsonNodes.get("validationErrorData").size());

        JsonNode validationErrorData = jsonNodes.get("validationErrorData")
                .get(0);
        assertTrue(validationErrorData.get("parameterName").asText()
                .contains(propertyName));
        String validationErrorMessage = validationErrorData.get("message")
                .asText();
        assertTrue(validationErrorMessage.contains(propertyName));
        assertTrue(validationErrorMessage
                .contains(Integer.toString(invalidPropertyValue)));
        assertTrue(validationErrorMessage.contains(
                TEST_VALIDATION_METHOD.getParameterTypes()[0].toString()));
    }

    @Test
    public void should_Invoke_ExplicitNullableTypeChecker()
            throws NoSuchMethodException {
        ExplicitNullableTypeChecker explicitNullableTypeChecker = mock(
                ExplicitNullableTypeChecker.class);

        when(explicitNullableTypeChecker.checkValueForType(
                NullCheckerTestClass.OK_RESPONSE, String.class))
                        .thenReturn(null);

        String testOkMethod = "testOkMethod";
        ResponseEntity<String> response = createVaadinController(
                new NullCheckerTestClass(), null, null, null,
                explicitNullableTypeChecker).serveVaadinService(
                        NullCheckerTestClass.class.getSimpleName(),
                        testOkMethod, createRequestParameters("{}"),
                        requestMock);

        verify(explicitNullableTypeChecker).checkValueForAnnotatedElement(
                NullCheckerTestClass.OK_RESPONSE,
                NullCheckerTestClass.class.getMethod(testOkMethod));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("\"" + NullCheckerTestClass.OK_RESPONSE + "\"",
                response.getBody());
    }

    @Test
    public void should_ReturnException_When_ExplicitNullableTypeChecker_ReturnsError()
            throws IOException, NoSuchMethodException {
        final String errorMessage = "Got null";

        ExplicitNullableTypeChecker explicitNullableTypeChecker = mock(
                ExplicitNullableTypeChecker.class);
        String testNullMethodName = "testNullMethod";
        Method testNullMethod = NullCheckerTestClass.class
                .getMethod(testNullMethodName);
        when(explicitNullableTypeChecker.checkValueForAnnotatedElement(null,
                testNullMethod))
                .thenReturn(errorMessage);

        ResponseEntity<String> response = createVaadinController(
                new NullCheckerTestClass(), null, null, null,
                explicitNullableTypeChecker).serveVaadinService(
                        NullCheckerTestClass.class.getSimpleName(),
                testNullMethodName, createRequestParameters("{}"),
                        requestMock);

        verify(explicitNullableTypeChecker).checkValueForAnnotatedElement(null,
                testNullMethod);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ObjectNode jsonNodes = new ObjectMapper().readValue(response.getBody(),
                ObjectNode.class);

        assertEquals(VaadinConnectException.class.getName(),
                jsonNodes.get("type").asText());
        final String message = jsonNodes.get("message").asText();
        assertTrue(message.contains("Unexpected return value"));
        assertTrue(message.contains(NullCheckerTestClass.class.getSimpleName()));
        assertTrue(message.contains(testNullMethodName));
        assertTrue(message.contains(errorMessage));
    }

    private void assertServiceInfoPresent(String responseBody) {
        assertTrue(String.format(
                "Response body '%s' should have service information in it",
                responseBody), responseBody.contains(TEST_SERVICE_NAME));
        assertTrue(String.format(
                "Response body '%s' should have service information in it",
                responseBody), responseBody.contains(TEST_METHOD.getName()));
    }

    private ObjectNode createRequestParameters(String jsonBody) {
        try {
            return new ObjectMapper().readValue(jsonBody, ObjectNode.class);
        } catch (IOException e) {
            throw new AssertionError(String
                    .format("Failed to deserialize the json: %s", jsonBody), e);
        }
    }

    private <T> VaadinConnectController createVaadinController(T service) {
        return createVaadinController(service, null, null, null, null);
    }

    private <T> VaadinConnectController createVaadinController(T service,
            ObjectMapper vaadinServiceMapper) {
        return createVaadinController(service, vaadinServiceMapper, null, null, null);
    }

    private <T> VaadinConnectController createVaadinController(T service,
            VaadinConnectAccessChecker accessChecker) {
        return createVaadinController(service, null, accessChecker, null, null);
    }

    private <T> VaadinConnectController createVaadinController(T service,
            ObjectMapper vaadinServiceMapper,
            VaadinConnectAccessChecker accessChecker,
            VaadinServiceNameChecker serviceNameChecker,
            ExplicitNullableTypeChecker explicitNullableTypeChecker) {
        Class<?> serviceClass = service.getClass();

        ApplicationContext contextMock = mock(ApplicationContext.class);
        when(contextMock.getBeansWithAnnotation(VaadinService.class))
                .thenReturn(Collections.singletonMap(serviceClass.getName(),
                        service));

        if (vaadinServiceMapper == null) {
            vaadinServiceMapper = new ObjectMapper();
        }

        if (accessChecker == null) {
            accessChecker = mock(
                    VaadinConnectAccessChecker.class);
            when(accessChecker.check(TEST_METHOD, requestMock)).thenReturn(null);
        }

        if (serviceNameChecker == null) {
            serviceNameChecker = mock(VaadinServiceNameChecker.class);
            when(serviceNameChecker.check(TEST_SERVICE_NAME)).thenReturn(null);
        }

        if (explicitNullableTypeChecker == null) {
            explicitNullableTypeChecker = mock(
                    ExplicitNullableTypeChecker.class);
            when(explicitNullableTypeChecker.checkValueForType(any(), any()))
                    .thenReturn(null);
        }

        return new VaadinConnectController(vaadinServiceMapper, accessChecker,
                serviceNameChecker, explicitNullableTypeChecker, contextMock);
    }

    private VaadinConnectController createVaadinControllerWithoutPrincipal() {
        when(requestMock.getUserPrincipal()).thenReturn(null);
        return createVaadinController(TEST_SERVICE, new VaadinConnectAccessChecker());
    }

    private Method createServiceMethodMockThatThrows(Object argument,
            Exception exceptionToThrow) throws Exception {
        Method serviceMethodMock = mock(Method.class);
        when(serviceMethodMock.invoke(TEST_SERVICE, argument))
                .thenThrow(exceptionToThrow);
        when(serviceMethodMock.getParameters())
                .thenReturn(TEST_METHOD.getParameters());
        doReturn(TEST_METHOD.getDeclaringClass()).when(serviceMethodMock)
                .getDeclaringClass();
        when(serviceMethodMock.getParameterTypes())
                .thenReturn(TEST_METHOD.getParameterTypes());
        when(serviceMethodMock.getName()).thenReturn(TEST_METHOD.getName());
        return serviceMethodMock;
    }
}
