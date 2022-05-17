package dev.hilla;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.gentyref.GenericTypeReflector;
import com.vaadin.flow.server.VaadinServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import dev.hilla.EndpointInvocationException.EndpointAccessDeniedException;
import dev.hilla.EndpointInvocationException.EndpointBadRequestException;
import dev.hilla.EndpointInvocationException.EndpointInternalException;
import dev.hilla.EndpointInvocationException.EndpointNotFoundException;
import dev.hilla.EndpointRegistry.VaadinEndpointData;
import dev.hilla.auth.EndpointAccessChecker;
import dev.hilla.endpointransfermapper.EndpointTransferMapper;
import dev.hilla.exception.EndpointException;
import dev.hilla.exception.EndpointValidationException;
import dev.hilla.exception.EndpointValidationException.ValidationErrorData;

/**
 * Handles invocation of endpoint methods after checking the user has proper
 * access.
 * <p>
 * This class is a generic invoker that does not have knowledge of HTTP requests
 * or the context that the method is being invoked in.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@Component
public class EndpointInvoker {

    private final ObjectMapper vaadinEndpointMapper;
    private final Validator validator = Validation
            .buildDefaultValidatorFactory().getValidator();
    private final ExplicitNullableTypeChecker explicitNullableTypeChecker;
    private final ApplicationContext applicationContext;

    EndpointRegistry endpointRegistry;

    private static EndpointTransferMapper endpointTransferMapper = new EndpointTransferMapper();

    private ServletContext servletContext;

    /**
     * Creates an instance of this bean.
     *
     * @param applicationContext
     *            Spring context to extract beans annotated with
     *            {@link Endpoint} from
     * @param vaadinEndpointMapper
     *            optional bean to override the default {@link ObjectMapper}
     *            that is used for serializing and deserializing request and
     *            response bodies Use
     *            {@link EndpointController#VAADIN_ENDPOINT_MAPPER_BEAN_QUALIFIER}
     *            qualifier to override the mapper.
     * @param explicitNullableTypeChecker
     *            the method parameter and return value type checker to verify
     *            that null values are explicit
     * @param servletContext
     *            the servlet context
     * @param endpointRegistry
     *            the registry used to store endpoint information
     */
    public EndpointInvoker(ApplicationContext applicationContext,
            ObjectMapper vaadinEndpointMapper,
            ExplicitNullableTypeChecker explicitNullableTypeChecker,
            ServletContext servletContext, EndpointRegistry endpointRegistry) {
        this.applicationContext = applicationContext;
        this.servletContext = servletContext;
        this.vaadinEndpointMapper = vaadinEndpointMapper != null
                ? vaadinEndpointMapper
                : createVaadinConnectObjectMapper(applicationContext);
        this.explicitNullableTypeChecker = explicitNullableTypeChecker;
        this.endpointRegistry = endpointRegistry;

    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(EndpointInvoker.class);
    }

    /**
     * Invoke the given endpoint method with the given parameters if the user
     * has access to do so.
     *
     * @param endpointName
     *            the name of the endpoint
     * @param methodName
     *            the name of the method in the endpoint
     * @param body
     *            optional request body, that should be specified if the method
     *            called has parameters
     * @param principal
     *            the user principal object
     * @param rolesChecker
     *            a function for checking if a user is in a given role
     * @return the return value of the invoked endpoint method, wrapped in a
     *         response entity
     * @throws EndpointNotFoundException
     *             if the endpoint was not found
     * @throws EndpointAccessDeniedException
     *             if access to the endpoint was denied
     * @throws EndpointBadRequestException
     *             if there was a problem with the request data
     * @throws EndpointInternalException
     *             if there was an internal error executing the endpoint method
     */
    public Object invoke(String endpointName, String methodName,
            ObjectNode body, Principal principal,
            Function<String, Boolean> rolesChecker)
            throws EndpointNotFoundException, EndpointAccessDeniedException,
            EndpointBadRequestException, EndpointInternalException {
        VaadinEndpointData vaadinEndpointData = endpointRegistry
                .get(endpointName);
        if (vaadinEndpointData == null) {
            getLogger().debug("Endpoint '{}' not found", endpointName);
            throw new EndpointNotFoundException();
        }

        Method methodToInvoke = getMethod(endpointName, methodName);
        if (methodToInvoke == null) {
            getLogger().debug("Method '{}' not found in endpoint '{}'",
                    methodName, endpointName);
            throw new EndpointNotFoundException();
        }

        return invokeVaadinEndpointMethod(endpointName, methodName,
                methodToInvoke, body, vaadinEndpointData, principal,
                rolesChecker);

    }

    private ObjectMapper createVaadinConnectObjectMapper(
            ApplicationContext context) {
        Jackson2ObjectMapperBuilder builder = context
                .getBean(Jackson2ObjectMapperBuilder.class);
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        JacksonProperties jacksonProperties = context
                .getBean(JacksonProperties.class);
        if (jacksonProperties.getVisibility().isEmpty()) {
            objectMapper.setVisibility(PropertyAccessor.ALL,
                    JsonAutoDetect.Visibility.ANY);
        }
        objectMapper.registerModule(new ByteArrayModule());
        return objectMapper;
    }

    private Object invokeVaadinEndpointMethod(String endpointName,
            String methodName, Method methodToInvoke, ObjectNode body,
            VaadinEndpointData vaadinEndpointData, Principal principal,
            Function<String, Boolean> rolesChecker)
            throws EndpointAccessDeniedException, EndpointBadRequestException,
            EndpointInternalException {
        EndpointAccessChecker accessChecker = getAccessChecker();
        String checkError = accessChecker.check(methodToInvoke, principal,
                rolesChecker);
        if (checkError != null) {
            throw new EndpointAccessDeniedException(String.format(
                    "Endpoint '%s' method '%s' request cannot be accessed, reason: '%s'",
                    endpointName, methodName, checkError));
        }

        Map<String, JsonNode> requestParameters = getRequestParameters(body);
        Type[] javaParameters = getJavaParameters(methodToInvoke, ClassUtils
                .getUserClass(vaadinEndpointData.getEndpointObject()));
        if (javaParameters.length != requestParameters.size()) {
            throw new EndpointBadRequestException(String.format(
                    "Incorrect number of parameters for endpoint '%s' method '%s', "
                            + "expected: %s, got: %s",
                    endpointName, methodName, javaParameters.length,
                    requestParameters.size()));
        }

        Object[] vaadinEndpointParameters = getVaadinEndpointParameters(
                requestParameters, javaParameters, methodName, endpointName);

        Set<ConstraintViolation<Object>> methodParameterConstraintViolations = validator
                .forExecutables()
                .validateParameters(vaadinEndpointData.getEndpointObject(),
                        methodToInvoke, vaadinEndpointParameters);
        if (!methodParameterConstraintViolations.isEmpty()) {
            throw new EndpointValidationException(
                    String.format(
                            "Validation error in endpoint '%s' method '%s'",
                            endpointName, methodName),
                    createMethodValidationErrors(
                            methodParameterConstraintViolations));
        }

        Object returnValue;
        try {
            returnValue = methodToInvoke.invoke(
                    vaadinEndpointData.getEndpointObject(),
                    vaadinEndpointParameters);
        } catch (IllegalArgumentException e) {
            String errorMessage = String.format(
                    "Received incorrect arguments for endpoint '%s' method '%s'. "
                            + "Expected parameter types (and their order) are: '[%s]'",
                    endpointName, methodName,
                    listMethodParameterTypes(javaParameters));
            getLogger().debug(errorMessage, e);
            throw new EndpointBadRequestException(errorMessage);
        } catch (IllegalAccessException e) {
            String errorMessage = String.format(
                    "Endpoint '%s' method '%s' access failure", endpointName,
                    methodName);
            getLogger().error(errorMessage, e);
            throw new EndpointInternalException(errorMessage);
        } catch (InvocationTargetException e) {
            return handleMethodExecutionError(endpointName, methodName, e);
        }

        returnValue = endpointTransferMapper.toTransferType(returnValue);

        String implicitNullError = this.explicitNullableTypeChecker
                .checkValueForAnnotatedElement(returnValue, methodToInvoke);
        if (implicitNullError != null) {
            String errorMessage = String.format(
                    "Unexpected return value in endpoint '%s' method '%s'. %s",
                    endpointName, methodName, implicitNullError);
            getLogger().error(errorMessage);
            throw new EndpointInternalException(errorMessage);
        }

        Set<ConstraintViolation<Object>> returnValueConstraintViolations = validator
                .forExecutables()
                .validateReturnValue(vaadinEndpointData.getEndpointObject(),
                        methodToInvoke, returnValue);
        if (!returnValueConstraintViolations.isEmpty()) {
            String errorMessage = String.format(
                    "Endpoint '%s' method '%s' returned a value that has validation errors: '%s'",
                    endpointName, methodName, returnValueConstraintViolations);
            throw new EndpointInternalException(errorMessage);
        }

        return returnValue;
    }

    private Type[] getJavaParameters(Method methodToInvoke, Type classType) {
        return Stream.of(GenericTypeReflector
                .getExactParameterTypes(methodToInvoke, classType))
                .toArray(Type[]::new);
    }

    private ResponseEntity<String> handleMethodExecutionError(
            String endpointName, String methodName, InvocationTargetException e)
            throws EndpointInternalException {
        if (EndpointException.class.isAssignableFrom(e.getCause().getClass())) {
            EndpointException endpointException = ((EndpointException) e
                    .getCause());
            getLogger().debug("Endpoint '{}' method '{}' aborted the execution",
                    endpointName, methodName, endpointException);
            throw endpointException;
        } else {
            String errorMessage = String.format(
                    "Endpoint '%s' method '%s' execution failure", endpointName,
                    methodName);
            getLogger().error(errorMessage, e);
            throw new EndpointInternalException(errorMessage);
        }
    }

    String createResponseErrorObject(String errorMessage) {
        ObjectNode objectNode = vaadinEndpointMapper.createObjectNode();
        objectNode.put(EndpointException.ERROR_MESSAGE_FIELD, errorMessage);
        return objectNode.toString();
    }

    String createResponseErrorObject(Map<String, Object> serializationData)
            throws JsonProcessingException {
        return vaadinEndpointMapper.writeValueAsString(serializationData);
    }

    String writeValueAsString(Object returnValue)
            throws JsonProcessingException {
        return vaadinEndpointMapper.writeValueAsString(returnValue);
    }

    private String listMethodParameterTypes(Type[] javaParameters) {
        return Stream.of(javaParameters).map(Type::getTypeName)
                .collect(Collectors.joining(", "));
    }

    private Object[] getVaadinEndpointParameters(
            Map<String, JsonNode> requestParameters, Type[] javaParameters,
            String methodName, String endpointName) {
        Object[] endpointParameters = new Object[javaParameters.length];
        String[] parameterNames = new String[requestParameters.size()];
        requestParameters.keySet().toArray(parameterNames);
        Map<String, String> errorParams = new HashMap<>();
        Set<ConstraintViolation<Object>> constraintViolations = new LinkedHashSet<>();

        for (int i = 0; i < javaParameters.length; i++) {
            Type parameterType = javaParameters[i];
            Type incomingType = parameterType;
            try {
                Class<?> mappedType = getTransferType(parameterType);
                if (mappedType != null) {
                    incomingType = mappedType;
                }
                Object parameter = vaadinEndpointMapper
                        .readerFor(vaadinEndpointMapper.getTypeFactory()
                                .constructType(incomingType))
                        .readValue(requestParameters.get(parameterNames[i]));
                if (mappedType != null) {
                    parameter = endpointTransferMapper.toEndpointType(parameter,
                            (Class) parameterType);
                }
                endpointParameters[i] = parameter;

                if (parameter != null) {
                    constraintViolations.addAll(validator.validate(parameter));
                }
            } catch (IOException e) {
                String typeName = parameterType.getTypeName();
                getLogger().error(
                        "Unable to deserialize an endpoint '{}' method '{}' "
                                + "parameter '{}' with type '{}'",
                        endpointName, methodName, parameterNames[i], typeName,
                        e);
                errorParams.put(parameterNames[i], typeName);
            }
        }

        if (errorParams.isEmpty() && constraintViolations.isEmpty()) {
            return endpointParameters;
        }
        throw getInvalidEndpointParametersException(methodName, endpointName,
                errorParams, constraintViolations);
    }

    private Class<?> getTransferType(Type type) {
        if (!(type instanceof Class)) {
            return null;
        }

        return endpointTransferMapper.getTransferType((Class) type);
    }

    private EndpointValidationException getInvalidEndpointParametersException(
            String methodName, String endpointName,
            Map<String, String> deserializationErrors,
            Set<ConstraintViolation<Object>> constraintViolations) {
        List<ValidationErrorData> validationErrorData = new ArrayList<>(
                deserializationErrors.size() + constraintViolations.size());

        for (Map.Entry<String, String> deserializationError : deserializationErrors
                .entrySet()) {
            String message = String.format(
                    "Unable to deserialize an endpoint method parameter into type '%s'",
                    deserializationError.getValue());
            validationErrorData.add(new ValidationErrorData(message,
                    deserializationError.getKey()));
        }

        validationErrorData
                .addAll(createBeanValidationErrors(constraintViolations));

        String message = String.format(
                "Validation error in endpoint '%s' method '%s'", endpointName,
                methodName);
        return new EndpointValidationException(message, validationErrorData);
    }

    private List<ValidationErrorData> createBeanValidationErrors(
            Collection<ConstraintViolation<Object>> beanConstraintViolations) {
        return beanConstraintViolations.stream().map(
                constraintViolation -> new ValidationErrorData(String.format(
                        "Object of type '%s' has invalid property '%s' with value '%s', validation error: '%s'",
                        constraintViolation.getRootBeanClass(),
                        constraintViolation.getPropertyPath().toString(),
                        constraintViolation.getInvalidValue(),
                        constraintViolation.getMessage()),
                        constraintViolation.getPropertyPath().toString()))
                .collect(Collectors.toList());
    }

    private List<ValidationErrorData> createMethodValidationErrors(
            Collection<ConstraintViolation<Object>> methodConstraintViolations) {
        return methodConstraintViolations.stream().map(constraintViolation -> {
            String parameterPath = constraintViolation.getPropertyPath()
                    .toString();
            return new ValidationErrorData(String.format(
                    "Method '%s' of the object '%s' received invalid parameter '%s' with value '%s', validation error: '%s'",
                    parameterPath.split("\\.")[0],
                    constraintViolation.getRootBeanClass(), parameterPath,
                    constraintViolation.getInvalidValue(),
                    constraintViolation.getMessage()), parameterPath);
        }).collect(Collectors.toList());
    }

    private Map<String, JsonNode> getRequestParameters(ObjectNode body) {
        Map<String, JsonNode> parametersData = new LinkedHashMap<>();
        if (body != null) {
            body.fields().forEachRemaining(entry -> parametersData
                    .put(entry.getKey(), entry.getValue()));
        }
        return parametersData;
    }

    private static class VaadinConnectAccessCheckerWrapper {
        private final EndpointAccessChecker accessChecker;

        private VaadinConnectAccessCheckerWrapper(
                EndpointAccessChecker checker) {
            accessChecker = checker;
        }
    }

    EndpointAccessChecker getAccessChecker() {
        VaadinServletContext vaadinServletContext = new VaadinServletContext(
                servletContext);
        VaadinConnectAccessCheckerWrapper wrapper = vaadinServletContext
                .getAttribute(VaadinConnectAccessCheckerWrapper.class, () -> {
                    EndpointAccessChecker accessChecker = applicationContext
                            .getBean(EndpointAccessChecker.class);
                    return new VaadinConnectAccessCheckerWrapper(accessChecker);
                });
        return wrapper.accessChecker;
    }

    private Method getMethod(String endpointName, String methodName) {
        VaadinEndpointData endpointData = endpointRegistry.get(endpointName);
        if (endpointData == null) {
            getLogger().debug("Endpoint '{}' not found", endpointName);
            return null;
        }
        return endpointData.getMethod(methodName).orElse(null);
    }

    /**
     * Gets the return type of the given method.
     *
     * @param endpointName
     *            the name of the endpoint
     * @param methodName
     *            the name of the method
     */
    public Class<?> getReturnType(String endpointName, String methodName) {
        Method method = getMethod(endpointName, methodName);
        if (method == null) {
            getLogger().debug("Method '{}' not found in endpoint '{}'",
                    methodName, endpointName);
            return null;
        }
        return method.getReturnType();
    }

}
