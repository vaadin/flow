/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.vaadin.flow.server.connect.auth.VaadinConnectAccessChecker;

/**
 * A configuration class for customizing the {@link VaadinConnectController}
 * class.
 */
@Configuration
public class VaadinConnectControllerConfiguration {
    private final VaadinConnectProperties vaadinConnectProperties;

    /**
     * Initializes the connect configuration.
     *
     * @param vaadinConnectProperties
     *            Vaadin Connect properties
     */
    public VaadinConnectControllerConfiguration(
            VaadinConnectProperties vaadinConnectProperties) {
        this.vaadinConnectProperties = vaadinConnectProperties;
    }

    /**
     * Registers {@link VaadinConnectController} to use
     * {@link VaadinConnectProperties#getVaadinConnectEndpoint()} as an endpoint
     * for all Vaadin Connect services.
     *
     * @return updated configuration for {@link VaadinConnectController}
     */
    @Bean
    public WebMvcRegistrations webMvcRegistrationsHandlerMapping() {
        return new WebMvcRegistrations() {

            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new RequestMappingHandlerMapping() {

                    // If Spring application context initialization fails here with a stack overflow
                    // when starting a project that also has the `vaadin-spring` dependency, make sure
                    // that the Spring version in `flow-server` and in `vaadin-spring` is the same.
                    @Override
                    protected void registerHandlerMethod(Object handler,
                            Method method, RequestMappingInfo mapping) {

                        if (VaadinConnectController.class
                                .equals(method.getDeclaringClass())) {
                            PatternsRequestCondition connectServicePattern = new PatternsRequestCondition(
                                    vaadinConnectProperties
                                            .getVaadinConnectEndpoint())
                                                    .combine(mapping
                                                            .getPatternsCondition());

                            mapping = new RequestMappingInfo(mapping.getName(),
                                    connectServicePattern,
                                    mapping.getMethodsCondition(),
                                    mapping.getParamsCondition(),
                                    mapping.getHeadersCondition(),
                                    mapping.getConsumesCondition(),
                                    mapping.getProducesCondition(),
                                    mapping.getCustomCondition());
                        }

                        super.registerHandlerMethod(handler, method, mapping);
                    }
                };
            }
        };
    }

    /**
     * Registers a service name checker responsible for validating the service
     * names.
     *
     * @return the service name checker
     */
    @Bean
    public VaadinServiceNameChecker serviceNameChecker() {
        return new VaadinServiceNameChecker();
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
}
