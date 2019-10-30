package com.vaadin.flow.server.connect;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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

import com.vaadin.flow.server.connect.auth.VaadinConnectAccessChecker;
import com.vaadin.flow.server.connect.exception.VaadinConnectException;
import com.vaadin.flow.server.connect.exception.VaadinConnectValidationException;
import com.vaadin.flow.server.connect.testservice.BridgeMethodTestService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void should_ThrowException_When_ContextHasNoBeanData() {
        String beanName = "test";

        ApplicationContext contextMock = mock(ApplicationContext.class);
        when(contextMock.getType(beanName)).thenReturn(null);
        when(contextMock.getBeansWithAnnotation(VaadinService.class))
                .thenReturn(Collections.singletonMap(beanName, null));

        exception.expect(IllegalStateException.class);
        exception.expectMessage(beanName);
        new VaadinConnectController(mock(ObjectMapper.class), null, null,
                null, contextMock);
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
                .serveVaadinService(missingServiceName, null, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void should_Return404_When_MethodNotFound() {
        String missingServiceMethod = "whatever";
        assertNotEquals(TEST_METHOD.getName(), missingServiceMethod);

        ResponseEntity<?> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(TEST_SERVICE_NAME, missingServiceMethod,
                        null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void should_Return404_When_IllegalAccessToMethodIsPerformed() {
        String accessErrorMessage = "Access error";

        VaadinConnectAccessChecker restrictingCheckerMock = mock(
                VaadinConnectAccessChecker.class);
        when(restrictingCheckerMock.check(TEST_METHOD))
                .thenReturn(accessErrorMessage);

        VaadinServiceNameChecker nameCheckerMock = mock(
                VaadinServiceNameChecker.class);
        when(nameCheckerMock.check(TEST_SERVICE_NAME)).thenReturn(null);

        ExplicitNullableTypeChecker explicitNullableTypeCheckerMock = mock(
                ExplicitNullableTypeChecker.class
        );

        ResponseEntity<String> response = createVaadinController(TEST_SERVICE,
                new ObjectMapper(), restrictingCheckerMock, nameCheckerMock,
                explicitNullableTypeCheckerMock)
                        .serveVaadinService(TEST_SERVICE_NAME,
                                TEST_METHOD.getName(), null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        String responseBody = response.getBody();
        assertServiceInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(accessErrorMessage));

        verify(restrictingCheckerMock, only()).check(TEST_METHOD);
        verify(restrictingCheckerMock, times(1)).check(TEST_METHOD);
    }

    @Test
    public void should_Return400_When_LessParametersSpecified1() {
        ResponseEntity<String> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(TEST_SERVICE_NAME, TEST_METHOD.getName(),
                        null);

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
                                "{\"value1\": 222, \"value2\": 333}"));

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
                        createRequestParameters("{\"value\": [222]}"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assertServiceInfoPresent(responseBody);
        assertTrue(String.format("Invalid response body: '%s'", responseBody),
                responseBody.contains(
                        TEST_METHOD.getParameterTypes()[0].getSimpleName()));
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
                        String.format("{\"value\": %s}", inputValue)));

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
                        String.format("{\"value\": %s}", inputValue)));

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
                        String.format("{\"value\": %s}", inputValue)));

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
                        String.format("{\"value\": %s}", inputValue)));

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
                        String.format("{\"value\": %s}", inputValue)));

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
                        createRequestParameters("{\"value\": 222}"));

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
                createRequestParameters("{\"value\": 222}"));
    }

    @Test
    public void should_ReturnCorrectResponse_When_EverythingIsCorrect() {
        int inputValue = 222;
        String expectedOutput = TEST_SERVICE.testMethod(inputValue);

        ResponseEntity<String> response = createVaadinController(TEST_SERVICE)
                .serveVaadinService(TEST_SERVICE_NAME, TEST_METHOD.getName(),
                        createRequestParameters(
                                String.format("{\"value\": %s}", inputValue)));

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
                                "{\"value\": {\"id\": \"%s\"}}", inputId)));
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
                                String.format("{\"value\": %s}", inputId)));
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
                                String.format("{\"value\": %s}", inputId)));
        assertEquals(inputId, response.getBody());
    }

    @Test
    public void should_UseCustomServiceName_When_ItIsDefined() {
        int input = 111;
        String expectedOutput = new TestClassWithCustomServiceName()
                .testMethod(input);
        String beanName = TestClassWithCustomServiceName.class.getSimpleName();

        ApplicationContext contextMock = mock(ApplicationContext.class);
        when(contextMock.getType(beanName))
                .thenReturn((Class) TestClassWithCustomServiceName.class);
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
                                String.format("{\"value\": %s}", input)));
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
                                String.format("{\"value\": %s}", inputValue)));

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
                        createRequestParameters(inputValue));

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
                        createRequestParameters("{\"parameter\": null}"));

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
                                invalidPropertyValue)));

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
        VaadinConnectAccessChecker accessCheckerMock = mock(
                VaadinConnectAccessChecker.class);
        when(accessCheckerMock.check(TEST_METHOD)).thenReturn(null);

        VaadinServiceNameChecker nameCheckerMock = mock(
                VaadinServiceNameChecker.class);
        when(nameCheckerMock.check(TEST_SERVICE_NAME)).thenReturn(null);

        ExplicitNullableTypeChecker explicitNullableTypeCheckerMock = mock(
                ExplicitNullableTypeChecker.class
        );

        return createVaadinController(service, new ObjectMapper(),
                accessCheckerMock, nameCheckerMock, explicitNullableTypeCheckerMock);
    }

    private <T> VaadinConnectController createVaadinController(T service,
            ObjectMapper vaadinServiceMapper) {
        VaadinConnectAccessChecker accessCheckerMock = mock(
                VaadinConnectAccessChecker.class);
        when(accessCheckerMock.check(TEST_METHOD)).thenReturn(null);

        VaadinServiceNameChecker nameCheckerMock = mock(
                VaadinServiceNameChecker.class);
        when(nameCheckerMock.check(TEST_SERVICE_NAME)).thenReturn(null);

        ExplicitNullableTypeChecker explicitNullableTypeCheckerMock = mock(
                ExplicitNullableTypeChecker.class
        );

        return createVaadinController(service, vaadinServiceMapper,
                accessCheckerMock, nameCheckerMock,
                explicitNullableTypeCheckerMock);
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
        when(contextMock.getType(serviceClass.getName()))
                .thenReturn((Class) serviceClass);
        return new VaadinConnectController(vaadinServiceMapper, accessChecker,
                serviceNameChecker, explicitNullableTypeChecker, contextMock);
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
