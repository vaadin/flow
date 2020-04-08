/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.server.connect;

import java.lang.reflect.Method;

import com.vaadin.flow.server.frontend.FrontendUtils;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.server.connect.auth.VaadinConnectAccessChecker;

import static com.vaadin.flow.server.connect.VaadinConnectController.VAADIN_ENDPOINT_MAPPER_BEAN_QUALIFIER;

/**
 * A configuration class for customizing the {@link VaadinConnectController}
 * class.
 */
@Configuration
public class VaadinConnectControllerConfiguration {
    private final VaadinEndpointProperties vaadinEndpointProperties;

    /**
     * Initializes the connect configuration.
     *
     * @param vaadinEndpointProperties
     *            Vaadin Connect properties
     */
    public VaadinConnectControllerConfiguration(
            VaadinEndpointProperties vaadinEndpointProperties) {
        this.vaadinEndpointProperties = vaadinEndpointProperties;
    }

    /**
     * Registers {@link VaadinConnectController} to use
     * {@link VaadinEndpointProperties#getVaadinEndpointPrefix()} as a prefix
     * for all Vaadin Connect endpoints.
     *
     * @return updated configuration for {@link VaadinConnectController}
     */
    @Bean
    public WebMvcRegistrations webMvcRegistrationsHandlerMapping() {
        return new WebMvcRegistrations() {

            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new RequestMappingHandlerMapping() {

                    @Override
                    protected void registerHandlerMethod(Object handler,
                            Method method, RequestMappingInfo mapping) {
                        // If Spring context initialization fails here with a
                        // stack overflow in a project that also has the
                        // `vaadin-spring` dependency, make sure that the Spring
                        // version in `flow-server` and in `vaadin-spring` is
                        // the same.

                        if (VaadinConnectController.class
                                .equals(method.getDeclaringClass())) {
                            mapping = prependConnectPrefixUrl(mapping);
                        }

                        super.registerHandlerMethod(handler, method, mapping);
                    }
                };
            }
        };
    }

    /**
     * Prepends the Connect prefix URL from the Vaadin Connect properties to
     * the {@code pattern} of a {@link RequestMappingInfo} object, and returns
     * the updated mapping as a new object (not modifying the given
     * {@param mapping} parameter).
     *
     * @return a new mapping with the Connect prefix URL prepended to the
     *         mapping pattern
     */
    private RequestMappingInfo prependConnectPrefixUrl(
            RequestMappingInfo mapping) {
        String customEnpointPrefixName = FrontendUtils.getCustomEndpointPrefix();
        PatternsRequestCondition connectEndpointPattern =
                new PatternsRequestCondition(
                        customEnpointPrefixName != null ? customEnpointPrefixName :
                vaadinEndpointProperties.getVaadinEndpointPrefix())
                        .combine(mapping.getPatternsCondition());

        return new RequestMappingInfo(mapping.getName(), connectEndpointPattern,
                mapping.getMethodsCondition(), mapping.getParamsCondition(),
                mapping.getHeadersCondition(), mapping.getConsumesCondition(),
                mapping.getProducesCondition(), mapping.getCustomCondition());
    }

    /**
     * Registers an endpoint name checker responsible for validating the
     * endpoint names.
     *
     * @return the endpoint name checker
     */
    @Bean
    public EndpointNameChecker endpointNameChecker() {
        return new EndpointNameChecker();
    }

    /**
     * Registers a default {@link VaadinConnectAccessChecker} bean instance.
     *
     * @return the default Vaadin Connect access checker bean
     */
    @Bean
    public VaadinConnectAccessChecker accessChecker() {
        return new VaadinConnectAccessChecker();
    }

    /**
     * Registers a {@link ExplicitNullableTypeChecker} bean instance.
     *
     * @return the explicit nullable type checker
     */
    @Bean
    public ExplicitNullableTypeChecker typeChecker() {
        return new ExplicitNullableTypeChecker();
    }

    /**
     * Registers a {@link ObjectMapper} bean instance.
     *
     * @param context
     *            Spring application context
     * @return the object mapper for endpoint.
     */
    @Bean
    @Qualifier(VAADIN_ENDPOINT_MAPPER_BEAN_QUALIFIER)
    public ObjectMapper vaadinEndpointMapper(ApplicationContext context) {
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonProperties jacksonProperties = context
                .getBean(JacksonProperties.class);
        if (jacksonProperties.getVisibility().isEmpty()) {
            objectMapper.setVisibility(PropertyAccessor.ALL,
                    JsonAutoDetect.Visibility.ANY);
        }
        return objectMapper;
    }
}
