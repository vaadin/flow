package com.vaadin.fusion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.fusion.auth.CsrfChecker;
import com.vaadin.fusion.auth.FusionAccessChecker;
import com.vaadin.fusion.exception.EndpointException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.NoOp;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class FusionControllerTest {
    private static final TestClass TEST_ENDPOINT = new TestClass();
    private static final String TEST_ENDPOINT_NAME = TEST_ENDPOINT.getClass()
            .getSimpleName();
    private static final Method TEST_METHOD;
    private static final Method TEST_VALIDATION_METHOD;
    private HttpServletRequest requestMock;
    private Principal principal;
    private ApplicationConfiguration appConfig;

    static {
        TEST_METHOD = Stream.of(TEST_ENDPOINT.getClass().getDeclaredMethods())
                .filter(method -> "testMethod".equals(method.getName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "Failed to find a test endpoint method"));
        TEST_VALIDATION_METHOD = Stream
                .of(TEST_ENDPOINT.getClass().getDeclaredMethods())
                .filter(method -> "testValidationMethod"
                        .equals(method.getName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "Failed to find a test validation endpoint method"));
    }

    private static class TestValidationParameter {
        @Min(10)
        private final int count;

        public TestValidationParameter(@JsonProperty("count") int count) {
            this.count = count;
        }
    }

    @Endpoint
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
        @RolesAllowed({ "FOO_ROLE", "BAR_ROLE" })
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

        @PermitAll
        public String getUserName() {
            return VaadinService.getCurrentRequest().getUserPrincipal()
                    .getName();
        }
    }

    @Endpoint("CustomEndpoint")
    public static class TestClassWithCustomEndpointName {
        public String testMethod(int parameter) {
            return parameter + "-test";
        }
    }

    @Endpoint("my endpoint")
    public static class TestClassWithIllegalEndpointName {
        public String testMethod(int parameter) {
            return parameter + "-test";
        }
    }

    @Endpoint
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
        principal = mock(Principal.class);

        appConfig = Mockito.mock(ApplicationConfiguration.class);

        when(requestMock.getUserPrincipal()).thenReturn(principal);
        when(requestMock.getHeader("X-CSRF-Token")).thenReturn("Vaadin Fusion");
        doReturn(mockServletContext()).when(requestMock).getServletContext();

        when(requestMock.getCookies()).thenReturn(new Cookie[] {
                new Cookie(ApplicationConstants.CSRF_TOKEN, "Vaadin Fusion") });
    }

    @Test
    public void should_Return404_When_EndpointNotFound() {
        String missingEndpointName = "whatever";
        assertNotEquals(missingEndpointName, TEST_ENDPOINT_NAME);

        ResponseEntity<?> response = createVaadinController(TEST_ENDPOINT)
                .serveEndpoint(missingEndpointName, null, null, requestMock);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void should_Return404_When_MethodNotFound() {
        String missingEndpointMethod = "whatever";
        assertNotEquals(TEST_METHOD.getName(), missingEndpointMethod);

        ResponseEntity<?> response = createVaadinController(TEST_ENDPOINT)
                .serveEndpoint(TEST_ENDPOINT_NAME, missingEndpointMethod, null,
                        requestMock);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void should_Return404_When_IllegalAccessToMethodIsPerformed() {
        String accessErrorMessage = "Access error";

        FusionAccessChecker restrictingCheckerMock = mock(
                FusionAccessChecker.class);
        when(restrictingCheckerMock.check(Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(accessErrorMessage);

        EndpointNameChecker nameCheckerMock = mock(EndpointNameChecker.class);
        when(nameCheckerMock.check(TEST_ENDPOINT_NAME)).thenReturn(null);

        ExplicitNullableTypeChecker explicitNullableTypeCheckerMock = mock(
                ExplicitNullableTypeChecker.class);

        ResponseEntity<String> response = createVaadinController(TEST_ENDPOINT,
                new ObjectMapper(), restrictingCheckerMock, nameCheckerMock,
                explicitNullableTypeCheckerMock, null).serveEndpoint(
                        TEST_ENDPOINT_NAME, TEST_METHOD.getName(), null,
                        requestMock);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        String responseBody = response.getBody();
        assertEndpointInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(accessErrorMessage));

        verify(restrictingCheckerMock).check(Mockito.any(), Mockito.any(),
                Mockito.any());
        Mockito.verifyNoMoreInteractions(restrictingCheckerMock);
        verify(restrictingCheckerMock, times(1)).check(Mockito.any(),
                Mockito.any(), Mockito.any());
    }

    @Test
    public void should_Return400_When_LessParametersSpecified1() {
        ResponseEntity<String> response = createVaadinController(TEST_ENDPOINT)
                .serveEndpoint(TEST_ENDPOINT_NAME, TEST_METHOD.getName(), null,
                        requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertEndpointInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains("0"));
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(
                        Integer.toString(TEST_METHOD.getParameterCount())));
    }

    @Test
    public void should_Return400_When_MoreParametersSpecified() {
        ResponseEntity<String> response = createVaadinController(TEST_ENDPOINT)
                .serveEndpoint(TEST_ENDPOINT_NAME, TEST_METHOD.getName(),
                        createRequestParameters(
                                "{\"value1\": 222, \"value2\": 333}"),
                        requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertEndpointInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains("2"));
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(
                        Integer.toString(TEST_METHOD.getParameterCount())));
    }

    @Test
    public void should_Return400_When_IncorrectParameterTypesAreProvided() {
        ResponseEntity<String> response = createVaadinController(TEST_ENDPOINT)
                .serveEndpoint(TEST_ENDPOINT_NAME, TEST_METHOD.getName(),
                        createRequestParameters("{\"value\": [222]}"),
                        requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertEndpointInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(
                        TEST_METHOD.getParameterTypes()[0].getSimpleName()));
    }

    @Test
    public void should_NotCallMethod_When_a_CSRF_request() {
        when(appConfig.isXsrfProtectionEnabled()).thenReturn(true);
        when(requestMock.getHeader("X-CSRF-Token")).thenReturn(null);

        FusionController vaadinController = createVaadinControllerWithoutPrincipal();
        ResponseEntity<String> response = vaadinController.serveEndpoint(
                TEST_ENDPOINT_NAME, "testAnonymousMethod",
                createRequestParameters("{}"), requestMock);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        String responseBody = response.getBody();
        assertNotNull("Response body should not be null", responseBody);
        assertTrue("Should return unauthorized error",
                responseBody.contains(FusionAccessChecker.ACCESS_DENIED_MSG));
    }

    @Test
    @Ignore("Does not work yet")
    public void should_CallEnableCsrf_When_CreatingFusionController() {
        ApplicationContext appContext = mockApplicationContext(TEST_ENDPOINT);
        ServletContext servletContext = mockServletContext();

        CsrfChecker csrfChecker = Mockito.mock(CsrfChecker.class);
        createVaadinController(TEST_ENDPOINT, null, null, null, null,
                csrfChecker);

        verify(csrfChecker).setCsrfProtection(Mockito.anyBoolean());
    }

    @Test
    public void should_clearVaadinRequestInstance_after_EndpointCall() {
        FusionController vaadinController = createVaadinController(
                TEST_ENDPOINT,
                new FusionAccessChecker(new AccessAnnotationChecker()));

        vaadinController.serveEndpoint(TEST_ENDPOINT_NAME, "getUserName",
                createRequestParameters("{}"), requestMock);

        Assert.assertNull(CurrentInstance.get(VaadinRequest.class));
        Assert.assertNull(VaadinRequest.getCurrent());
    }

    @Test
    @Ignore("requires mockito version with plugin for final classes")
    public void should_Return400_When_EndpointMethodThrowsIllegalArgumentException()
            throws Exception {
        int inputValue = 222;

        Method endpointMethodMock = createEndpointMethodMockThatThrows(
                inputValue, new IllegalArgumentException("OOPS"));

        FusionController controller = createVaadinController(TEST_ENDPOINT);
        controller.endpointRegistry
                .get(TEST_ENDPOINT_NAME.toLowerCase()).methods.put(
                        TEST_METHOD.getName().toLowerCase(),
                        endpointMethodMock);

        ResponseEntity<String> response = controller.serveEndpoint(
                TEST_ENDPOINT_NAME, TEST_METHOD.getName(),
                createRequestParameters(
                        String.format("{\"value\": %s}", inputValue)),
                requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertEndpointInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(
                        TEST_METHOD.getParameterTypes()[0].getSimpleName()));

        verify(endpointMethodMock, times(1)).invoke(TEST_ENDPOINT, inputValue);
        verify(endpointMethodMock, times(1)).getParameters();
    }

    @Test
    @Ignore("requires mockito version with plugin for final classes")
    public void should_Return500_When_EndpointMethodThrowsIllegalAccessException()
            throws Exception {
        int inputValue = 222;

        Method endpointMethodMock = createEndpointMethodMockThatThrows(
                inputValue, new IllegalAccessException("OOPS"));

        FusionController controller = createVaadinController(TEST_ENDPOINT);
        controller.endpointRegistry
                .get(TEST_ENDPOINT_NAME.toLowerCase()).methods.put(
                        TEST_METHOD.getName().toLowerCase(),
                        endpointMethodMock);

        ResponseEntity<String> response = controller.serveEndpoint(
                TEST_ENDPOINT_NAME, TEST_METHOD.getName(),
                createRequestParameters(
                        String.format("{\"value\": %s}", inputValue)),
                requestMock);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());
        String responseBody = response.getBody();
        assertEndpointInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains("access failure"));

        verify(endpointMethodMock, times(1)).invoke(TEST_ENDPOINT, inputValue);
        verify(endpointMethodMock, times(1)).getParameters();
    }

    @Test
    @Ignore("requires mockito version with plugin for final classes")
    public void should_Return500_When_EndpointMethodThrowsInvocationTargetException()
            throws Exception {
        int inputValue = 222;

        Method endpointMethodMock = createEndpointMethodMockThatThrows(
                inputValue, new InvocationTargetException(
                        new IllegalStateException("OOPS")));

        FusionController controller = createVaadinController(TEST_ENDPOINT);
        controller.endpointRegistry
                .get(TEST_ENDPOINT_NAME.toLowerCase()).methods.put(
                        TEST_METHOD.getName().toLowerCase(),
                        endpointMethodMock);

        ResponseEntity<String> response = controller.serveEndpoint(
                TEST_ENDPOINT_NAME, TEST_METHOD.getName(),
                createRequestParameters(
                        String.format("{\"value\": %s}", inputValue)),
                requestMock);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());
        String responseBody = response.getBody();
        assertEndpointInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains("execution failure"));

        verify(endpointMethodMock, times(1)).invoke(TEST_ENDPOINT, inputValue);
        verify(endpointMethodMock, times(1)).getParameters();
    }

    @Test
    @Ignore("requires mockito version with plugin for final classes")
    public void should_Return400_When_EndpointMethodThrowsVaadinConnectException()
            throws Exception {
        int inputValue = 222;
        String expectedMessage = "OOPS";

        Method endpointMethodMock = createEndpointMethodMockThatThrows(
                inputValue, new InvocationTargetException(
                        new EndpointException(expectedMessage)));

        FusionController controller = createVaadinController(TEST_ENDPOINT);
        controller.endpointRegistry
                .get(TEST_ENDPOINT_NAME.toLowerCase()).methods.put(
                        TEST_METHOD.getName().toLowerCase(),
                        endpointMethodMock);

        ResponseEntity<String> response = controller.serveEndpoint(
                TEST_ENDPOINT_NAME, TEST_METHOD.getName(),
                createRequestParameters(
                        String.format("{\"value\": %s}", inputValue)),
                requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(EndpointException.class.getName()));
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(expectedMessage));

        verify(endpointMethodMock, times(1)).invoke(TEST_ENDPOINT, inputValue);
        verify(endpointMethodMock, times(1)).getParameters();
    }

    @Test
    @Ignore("requires mockito version with plugin for final classes")
    public void should_Return400_When_EndpointMethodThrowsVaadinConnectExceptionSubclass()
            throws Exception {
        int inputValue = 222;
        String expectedMessage = "OOPS";

        class MyCustomException extends EndpointException {
            public MyCustomException() {
                super(expectedMessage);
            }
        }

        Method endpointMethodMock = createEndpointMethodMockThatThrows(
                inputValue,
                new InvocationTargetException(new MyCustomException()));

        FusionController controller = createVaadinController(TEST_ENDPOINT);
        controller.endpointRegistry
                .get(TEST_ENDPOINT_NAME.toLowerCase()).methods.put(
                        TEST_METHOD.getName().toLowerCase(),
                        endpointMethodMock);

        ResponseEntity<String> response = controller.serveEndpoint(
                TEST_ENDPOINT_NAME, TEST_METHOD.getName(),
                createRequestParameters(
                        String.format("{\"value\": %s}", inputValue)),
                requestMock);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(MyCustomException.class.getName()));
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(expectedMessage));

        verify(endpointMethodMock, times(1)).invoke(TEST_ENDPOINT, inputValue);
        verify(endpointMethodMock, times(1)).getParameters();
    }

    @Test
    public void should_ReturnCorrectResponse_When_EverythingIsCorrect() {
        int inputValue = 222;
        String expectedOutput = TEST_ENDPOINT.testMethod(inputValue);

        ResponseEntity<String> response = createVaadinController(TEST_ENDPOINT)
                .serveEndpoint(TEST_ENDPOINT_NAME, TEST_METHOD.getName(),
                        createRequestParameters(
                                String.format("{\"value\": %s}", inputValue)),
                        requestMock);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(String.format("\"%s\"", expectedOutput),
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
                testNullMethod)).thenReturn(errorMessage);

        ResponseEntity<String> response = createVaadinController(
                new NullCheckerTestClass(), null, null, null,
                explicitNullableTypeChecker, null).serveEndpoint(
                        NullCheckerTestClass.class.getSimpleName(),
                        testNullMethodName, createRequestParameters("{}"),
                        requestMock);

        verify(explicitNullableTypeChecker).checkValueForAnnotatedElement(null,
                testNullMethod);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());
        ObjectNode jsonNodes = new ObjectMapper().readValue(response.getBody(),
                ObjectNode.class);

        final String message = jsonNodes.get("message").asText();
        assertTrue(message.contains("Unexpected return value"));
        assertTrue(
                message.contains(NullCheckerTestClass.class.getSimpleName()));
        assertTrue(message.contains(testNullMethodName));
        assertTrue(message.contains(errorMessage));
    }

    @Test
    public void should_ReturnCorrectResponse_When_EndpointClassIsProxied() {

        ApplicationContext contextMock = mock(ApplicationContext.class);
        TestClass endpoint = new TestClass();

        // CGLib proxies are supported as entry-point classes
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(TestClass.class);
        enhancer.setCallback(NoOp.INSTANCE);
        TestClass proxy = (TestClass) enhancer.create();

        when(contextMock.getBeansWithAnnotation(Endpoint.class))

                .thenReturn(Collections.singletonMap(
                        endpoint.getClass().getSimpleName(), proxy));

        FusionController fusionController = createVaadinControllerWithApplicationContext(
                contextMock);

        int inputValue = 222;
        String expectedOutput = endpoint.testMethod(inputValue);

        ResponseEntity<String> response = fusionController.serveEndpoint(
                "TestClass", "testMethod",
                createRequestParameters(
                        String.format("{\"value\": %s}", inputValue)),
                requestMock);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(String.format("\"%s\"", expectedOutput),
                response.getBody());
    }

    private void assertEndpointInfoPresent(String responseBody) {
        assertTrue(String.format(
                "Response body '%s' should have endpoint information in it",
                responseBody), responseBody.contains(TEST_ENDPOINT_NAME));
        assertTrue(String.format(
                "Response body '%s' should have endpoint information in it",
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

    private <T> FusionController createVaadinController(T endpoint) {
        return createVaadinController(endpoint, null, null, null, null, null);
    }

    private <T> FusionController createVaadinController(T endpoint,
            ObjectMapper vaadinEndpointMapper) {
        return createVaadinController(endpoint, vaadinEndpointMapper, null,
                null, null, null);
    }

    private <T> FusionController createVaadinController(T endpoint,
            FusionAccessChecker accessChecker) {
        return createVaadinController(endpoint, null, accessChecker, null, null,
                null);
    }

    private <T> FusionController createVaadinController(T endpoint,
            ObjectMapper vaadinEndpointMapper,
            FusionAccessChecker accessChecker,
            EndpointNameChecker endpointNameChecker,
            ExplicitNullableTypeChecker explicitNullableTypeChecker,
            CsrfChecker csrfChecker) {
        if (vaadinEndpointMapper == null) {
            vaadinEndpointMapper = new ObjectMapper();
        }

        if (accessChecker == null) {
            accessChecker = mock(FusionAccessChecker.class);
            when(accessChecker.check(TEST_METHOD, requestMock))
                    .thenReturn(null);
        }
        if (csrfChecker == null) {
            csrfChecker = new CsrfChecker();
        }

        if (endpointNameChecker == null) {
            endpointNameChecker = mock(EndpointNameChecker.class);
            when(endpointNameChecker.check(TEST_ENDPOINT_NAME))
                    .thenReturn(null);
        }

        if (explicitNullableTypeChecker == null) {
            explicitNullableTypeChecker = mock(
                    ExplicitNullableTypeChecker.class);
            when(explicitNullableTypeChecker.checkValueForType(any(), any()))
                    .thenReturn(null);
        }

        ApplicationContext mockApplicationContext = mockApplicationContext(
                endpoint);
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);
        EndpointRegistry registry = new EndpointRegistry(endpointNameChecker);

        EndpointInvoker endpointInvoker = Mockito.spy(new EndpointInvoker(
                vaadinEndpointMapper, explicitNullableTypeChecker,
                mockApplicationContext, servletContext, registry));

        FusionController connectController = Mockito.spy(new FusionController(
                vaadinEndpointMapper, mockApplicationContext, registry,
                endpointInvoker, csrfChecker, servletContext));
        Mockito.doReturn(accessChecker).when(endpointInvoker)
                .getAccessChecker();
        return connectController;
    }

    private FusionController createVaadinControllerWithoutPrincipal() {
        when(requestMock.getUserPrincipal()).thenReturn(null);
        return createVaadinController(TEST_ENDPOINT,
                new FusionAccessChecker(new AccessAnnotationChecker()));
    }

    private FusionController createVaadinControllerWithApplicationContext(
            ApplicationContext applicationContext) {
        FusionControllerMockBuilder controllerMockBuilder = new FusionControllerMockBuilder();
        FusionController fusionController = controllerMockBuilder
                .withObjectMapper(new ObjectMapper())
                .withApplicationContext(applicationContext).build();
        return fusionController;
    }

    private Method createEndpointMethodMockThatThrows(Object argument,
            Exception exceptionToThrow) throws Exception {
        Method endpointMethodMock = mock(Method.class);
        when(endpointMethodMock.invoke(TEST_ENDPOINT, argument))
                .thenThrow(exceptionToThrow);
        when(endpointMethodMock.getParameters())
                .thenReturn(TEST_METHOD.getParameters());
        doReturn(TEST_METHOD.getDeclaringClass()).when(endpointMethodMock)
                .getDeclaringClass();
        when(endpointMethodMock.getParameterTypes())
                .thenReturn(TEST_METHOD.getParameterTypes());
        when(endpointMethodMock.getName()).thenReturn(TEST_METHOD.getName());
        return endpointMethodMock;
    }

    private ServletContext mockServletContext() {
        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(
                context.getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfig);
        return context;
    }

    private <T> ApplicationContext mockApplicationContext(T endpoint) {
        Class<?> endpointClass = endpoint.getClass();

        ApplicationContext contextMock = mock(ApplicationContext.class);
        when(contextMock.getBeansWithAnnotation(Endpoint.class)).thenReturn(
                Collections.singletonMap(endpointClass.getName(), endpoint));
        return contextMock;
    }

}
