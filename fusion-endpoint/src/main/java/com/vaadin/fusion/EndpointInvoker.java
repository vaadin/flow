/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.fusion;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.gentyref.GenericTypeReflector;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.fusion.EndpointRegistry.VaadinEndpointData;
import com.vaadin.fusion.auth.FusionAccessChecker;
import com.vaadin.fusion.endpointransfermapper.EndpointTransferMapper;
import com.vaadin.fusion.exception.EndpointException;
import com.vaadin.fusion.exception.EndpointValidationException;
import com.vaadin.fusion.exception.EndpointValidationException.ValidationErrorData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

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
    private final EndpointRegistry endpointRegistry;
    private final ObjectMapper vaadinEndpointMapper;
    private final Validator validator = Validation
            .buildDefaultValidatorFactory().getValidator();
    private static EndpointTransferMapper endpointTransferMapper = new EndpointTransferMapper();
    private final ExplicitNullableTypeChecker explicitNullableTypeChecker;
    private ApplicationContext applicationContext;
    private ServletContext servletContext;

    /**
     * Creates an instance of this bean.
     * 
     * @param vaadinEndpointMapper
     *            optional bean to override the default {@link ObjectMapper}
     *            that is used for serializing and deserializing request and
     *            response bodies Use
     *            {@link FusionController#VAADIN_ENDPOINT_MAPPER_BEAN_QUALIFIER}
     *            qualifier to override the mapper.
     * @param explicitNullableTypeChecker
     *            the method parameter and return value type checker to verify
     *            that null values are explicit
     * @param applicationContext
     *            Spring context to extract beans annotated with
     *            {@link Endpoint} from
     * @param endpointRegistry
     *            the registry used to store endpoint information
     * 
     */
    public EndpointInvoker(
            @Autowired(required = false) @Qualifier(FusionController.VAADIN_ENDPOINT_MAPPER_BEAN_QUALIFIER) ObjectMapper vaadinEndpointMapper,
            ExplicitNullableTypeChecker explicitNullableTypeChecker,
            ApplicationContext applicationContext,
            ServletContext servletContext, EndpointRegistry endpointRegistry) {
        this.vaadinEndpointMapper = vaadinEndpointMapper != null
                ? vaadinEndpointMapper
                : FusionController
                        .createVaadinConnectObjectMapper(applicationContext);

        this.explicitNullableTypeChecker = explicitNullableTypeChecker;
        this.applicationContext = applicationContext;
        this.servletContext = servletContext;
        this.endpointRegistry = endpointRegistry;
    }

    /**
     * Invoke the given endpoint method with the given parameters if the user
     * has access to do so.
     * 
     * @param endpointName
     *            the name of the endpoint
     * @param methodName
     *            the name of the method in the endpoint
     * @param methodParameters
     *            method parameters as a JSON object
     * @param principal
     *            the user principal object
     * @param rolesChecker
     *            a function for checking if a user is in a given role
     * @return the return value of the invoked endpoint method
     * @throws EndpointInvocationException
     *             if the invocation failed
     * 
     */
    public Object invoke(String endpointName, String methodName,
            ObjectNode methodParameters, Principal principal,
            Function<String, Boolean> rolesChecker)
            throws EndpointInvocationException {
        VaadinEndpointData vaadinEndpointData = endpointRegistry
                .get(endpointName);
        if (vaadinEndpointData == null) {
            getLogger().debug("Endpoint '{}' not found", endpointName);
            throw new EndpointInvocationException(
                    EndpointInvocationException.Type.NOT_FOUND);
        }

        Method methodToInvoke = vaadinEndpointData.getMethod(methodName)
                .orElse(null);
        if (methodToInvoke == null) {
            getLogger().debug("Method '{}' not found in endpoint '{}'",
                    methodName, endpointName);
            throw new EndpointInvocationException(
                    EndpointInvocationException.Type.NOT_FOUND);
        }

        Object returnValue = invokeVaadinEndpointMethod(endpointName,
                methodName, methodToInvoke, methodParameters,
                vaadinEndpointData, principal, rolesChecker);
        return returnValue;
    }

    private Object invokeVaadinEndpointMethod(String endpointName,
            String methodName, Method methodToInvoke, ObjectNode body,
            VaadinEndpointData vaadinEndpointData, Principal principal,
            Function<String, Boolean> rolesChecker)
            throws EndpointInvocationException {
        FusionAccessChecker accessChecker = getAccessChecker();
        String checkError = accessChecker.check(methodToInvoke, principal,
                rolesChecker);
        if (checkError != null) {
            throw new EndpointInvocationException(
                    EndpointInvocationException.Type.ACCESS_DENIED,
                    String.format(
                            "Endpoint '%s' method '%s' request cannot be accessed, reason: '%s'",
                            endpointName, methodName, checkError));
        }

        Map<String, JsonNode> requestParameters = getRequestParameters(body);
        Type[] javaParameters = getJavaParameters(methodToInvoke, ClassUtils
                .getUserClass(vaadinEndpointData.getEndpointObject()));
        if (javaParameters.length != requestParameters.size()) {
            String errorMessage = String.format(
                    "Incorrect number of parameters for endpoint '%s' method '%s', "
                            + "expected: %s, got: %s",
                    endpointName, methodName, javaParameters.length,
                    requestParameters.size());
            throw new EndpointInvocationException(
                    EndpointInvocationException.Type.INVALID_INPUT_DATA,
                    errorMessage);
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
            throw new EndpointInvocationException(
                    EndpointInvocationException.Type.INVALID_INPUT_DATA,
                    errorMessage);
        } catch (IllegalAccessException e) {
            String errorMessage = String.format(
                    "Endpoint '%s' method '%s' access failure", endpointName,
                    methodName);
            getLogger().error(errorMessage, e);
            throw new EndpointInvocationException(
                    EndpointInvocationException.Type.INTERNAL_ERROR);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof EndpointException) {
                EndpointException endpointException = (EndpointException) cause;
                getLogger().debug(
                        "Endpoint '{}' method '{}' aborted the execution",
                        endpointName, methodName, endpointException);
                throw endpointException;
            } else {
                String errorMessage = String.format(
                        "Endpoint '%s' method '%s' execution failure",
                        endpointName, methodName);
                getLogger().error(errorMessage, e);
                throw new EndpointInvocationException(
                        EndpointInvocationException.Type.INTERNAL_ERROR,
                        errorMessage);
            }
        }

        returnValue = endpointTransferMapper.toTransferType(returnValue);

        String implicitNullError = this.explicitNullableTypeChecker
                .checkValueForAnnotatedElement(returnValue, methodToInvoke);
        if (implicitNullError != null) {
            String errorMessage = String.format(
                    "Unexpected return value in endpoint '%s' method '%s'. %s",
                    endpointName, methodName, implicitNullError);
            getLogger().error(errorMessage);
            throw new EndpointInvocationException(
                    EndpointInvocationException.Type.INTERNAL_ERROR,
                    errorMessage);
        }

        Set<ConstraintViolation<Object>> returnValueConstraintViolations = validator
                .forExecutables()
                .validateReturnValue(vaadinEndpointData.getEndpointObject(),
                        methodToInvoke, returnValue);
        if (!returnValueConstraintViolations.isEmpty()) {
            getLogger().error(
                    "Endpoint '{}' method '{}' had returned a value that has validation errors: '{}', this might cause bugs on the client side. Fix the method implementation.",
                    endpointName, methodName, returnValueConstraintViolations);
        }
        return returnValue;
    }

    private Type[] getJavaParameters(Method methodToInvoke, Type classType) {
        return Stream.of(GenericTypeReflector
                .getExactParameterTypes(methodToInvoke, classType))
                .toArray(Type[]::new);
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
        private final FusionAccessChecker accessChecker;

        private VaadinConnectAccessCheckerWrapper(FusionAccessChecker checker) {
            accessChecker = checker;
        }
    }

    FusionAccessChecker getAccessChecker() {
        VaadinServletContext vaadinServletContext = new VaadinServletContext(
                servletContext);
        VaadinConnectAccessCheckerWrapper wrapper = vaadinServletContext
                .getAttribute(VaadinConnectAccessCheckerWrapper.class, () -> {
                    FusionAccessChecker accessChecker = applicationContext
                            .getBean(FusionAccessChecker.class);
                    ApplicationConfiguration cfg = ApplicationConfiguration
                            .get(vaadinServletContext);
                    if (cfg != null) {
                        accessChecker.enableCsrf(cfg.isXsrfProtectionEnabled());
                    }
                    return new VaadinConnectAccessCheckerWrapper(accessChecker);
                });
        return wrapper.accessChecker;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(EndpointInvoker.class);
    }

}
