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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.fusion.auth.CsrfChecker;
import com.vaadin.fusion.auth.FusionAccessChecker;
import com.vaadin.fusion.exception.EndpointException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The controller that is responsible for processing Vaadin endpoint requests.
 * Each class that is annotated with {@link Endpoint} gets its public methods
 * exposed so that those can be triggered by a correct POST request, including
 * the methods inherited from the other classes, excluding {@link Object} class
 * ones. Other methods (non-public) are not considered by the controller.
 * <p>
 * For example, if a class with name {@code TestClass} that has the only public
 * method {@code testMethod} was annotated with the annotation, it can be called
 * via {@literal http://${base_url}/testclass/testmethod} POST call, where
 * {@literal ${base_url}} is the application base url, configured by the user.
 * Class name and method name case in the request URL does not matter, but if
 * the method has parameters, the request body should contain a valid JSON with
 * all parameters in the same order as they are declared in the method. The
 * parameter types should also correspond for the request to be successful.
 */
@RestController
@Import({ FusionControllerConfiguration.class, FusionEndpointProperties.class })
@ConditionalOnBean(annotation = Endpoint.class)
@NpmPackage(value = "@vaadin/fusion-frontend", version = "0.0.15")
@NpmPackage(value = "@vaadin/form", version = "0.0.15")
public class FusionController {
    static final String ENDPOINT_METHODS = "/{endpoint}/{method}";

    /**
     * A qualifier to override the request and response default json mapper.
     *
     * @see #FusionController(ApplicationContext, EndpointRegistry,
     *      EndpointInvoker)
     */
    public static final String VAADIN_ENDPOINT_MAPPER_BEAN_QUALIFIER = "vaadinEndpointMapper";

    EndpointRegistry endpointRegistry;

    private EndpointInvoker endpointInvoker;

    private ObjectMapper vaadinEndpointMapper;

    private CsrfChecker csrfChecker;

    /**
     * A constructor used to initialize the controller.
     *
     * @param vaadinEndpointMapper
     *            optional bean to override the default {@link ObjectMapper}
     *            that is used for serializing and deserializing request and
     *            response bodies Use
     *            {@link FusionController#VAADIN_ENDPOINT_MAPPER_BEAN_QUALIFIER}
     *            qualifier to override the mapper.
     * @param context
     *            Spring context to extract beans annotated with
     *            {@link Endpoint} from
     * @param endpointRegistry
     *            the registry used to store endpoint information
     * @param endpointInvoker
     *            the invoker for endpoint methods
     * @param csrfChecker
     *            the csrf checker to use
     * 
     */
    public FusionController(
            @Autowired(required = false) @Qualifier(FusionController.VAADIN_ENDPOINT_MAPPER_BEAN_QUALIFIER) ObjectMapper vaadinEndpointMapper,
            ApplicationContext context, EndpointRegistry endpointRegistry,
            EndpointInvoker endpointInvoker, CsrfChecker csrfChecker,
            ServletContext servletContext) {
        this.csrfChecker = csrfChecker;
        this.vaadinEndpointMapper = vaadinEndpointMapper != null
                ? vaadinEndpointMapper
                : FusionController.createVaadinConnectObjectMapper(context);
        this.endpointInvoker = endpointInvoker;

        context.getBeansWithAnnotation(Endpoint.class)
                .forEach((name, endpointBean) -> endpointRegistry
                        .registerEndpoint(endpointBean));

        ApplicationConfiguration cfg = ApplicationConfiguration
                .get(new VaadinServletContext(servletContext));
        if (cfg != null) {
            csrfChecker.setCsrfProtection(cfg.isXsrfProtectionEnabled());
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(FusionController.class);
    }

    /**
     * Captures and processes the Vaadin endpoint requests.
     * <p>
     * Matches the endpoint name and a method name with the corresponding Java
     * class and a public method in the class. Extracts parameters from a
     * request body if the Java method requires any and applies in the same
     * order. After the method call, serializes the Java method execution result
     * and sends it back.
     * <p>
     * If an issue occurs during the request processing, an error response is
     * returned instead of the serialized Java method return value.
     *
     * @param endpointName
     *            the name of an endpoint to address the calls to, not case
     *            sensitive
     * @param methodName
     *            the method name to execute on an endpoint, not case sensitive
     * @param body
     *            optional request body, that should be specified if the method
     *            called has parameters
     * @param request
     *            the current request which triggers the endpoint call
     * @return execution result as a JSON string or an error message string
     */
    @PostMapping(path = ENDPOINT_METHODS, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> serveEndpoint(
            @PathVariable("endpoint") String endpointName,
            @PathVariable("method") String methodName,
            @RequestBody(required = false) ObjectNode body,
            HttpServletRequest request) {
        getLogger().debug("Endpoint: {}, method: {}, request body: {}",
                endpointName, methodName, body);

        if (!csrfChecker.validateCsrfTokenInRequest(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createResponseErrorObject(
                            FusionAccessChecker.ACCESS_DENIED_MSG));
        }

        try {
            // Put a VaadinRequest in the instances object so as the request is
            // available in the end-point method
            VaadinServletService service = (VaadinServletService) VaadinService
                    .getCurrent();
            CurrentInstance.set(VaadinRequest.class,
                    new VaadinServletRequest(request, service));

            Object returnValue = endpointInvoker.invoke(endpointName,
                    methodName, body, request.getUserPrincipal(),
                    request::isUserInRole);
            return ResponseEntity
                    .ok(vaadinEndpointMapper.writeValueAsString(returnValue));
        } catch (EndpointException e) {
            try {
                return ResponseEntity.badRequest().body(vaadinEndpointMapper
                        .writeValueAsString(e.getSerializationData()));
            } catch (JsonProcessingException ee) {
                String errorMessage = "Failed to serialize exception data";
                getLogger().error(errorMessage, ee);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorMessage);
            }
        } catch (EndpointInvocationException e) {
            switch (e.getType()) {
            case ACCESS_DENIED:
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createResponseErrorObject(e.getErrorMessage()));
            case INTERNAL_ERROR:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createResponseErrorObject(e.getErrorMessage()));
            case INVALID_INPUT_DATA:
                return ResponseEntity.badRequest()
                        .body(createResponseErrorObject(e.getErrorMessage()));
            case NOT_FOUND:
                return ResponseEntity.notFound().build();
            }

            // This cannot really be reached unless a constant is missing above
            return ResponseEntity.internalServerError().build();
        } catch (JsonProcessingException e) {
            String errorMessage = String.format(
                    "Failed to serialize endpoint '%s' method '%s' response. "
                            + "Double check method's return type or specify a custom mapper bean with qualifier '%s'",
                    endpointName, methodName,
                    FusionController.VAADIN_ENDPOINT_MAPPER_BEAN_QUALIFIER);
            getLogger().error(errorMessage, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        } finally {
            CurrentInstance.set(VaadinRequest.class, null);
        }
    }

    private String createResponseErrorObject(String errorMessage) {
        ObjectNode objectNode = vaadinEndpointMapper.createObjectNode();
        objectNode.put(EndpointException.ERROR_MESSAGE_FIELD, errorMessage);
        return objectNode.toString();
    }

    static ObjectMapper createVaadinConnectObjectMapper(
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
        return objectMapper;
    }

}
