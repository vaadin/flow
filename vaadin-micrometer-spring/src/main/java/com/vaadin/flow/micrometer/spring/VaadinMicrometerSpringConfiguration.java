/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.micrometer.spring;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vaadin.flow.micrometer.MetricsServiceInitListener;
import com.vaadin.flow.micrometer.VaadinMetricsConfig;

/**
 * Plain-Spring (non-Boot) configuration for vaadin-micrometer.
 * <p>
 * Users opt in by importing this class:
 *
 * <pre>
 * {@code
 * &#64;Configuration
 * &#64;Import(VaadinMicrometerSpringConfiguration.class)
 * public class MyAppConfig { ... }
 * }
 * </pre>
 *
 * Requires a {@link MeterRegistry} bean to be defined elsewhere in the
 * application context.
 */
@Configuration
public class VaadinMicrometerSpringConfiguration {

    @Bean
    public VaadinMetricsConfig vaadinMetricsConfig(
            @Value("${vaadin.metrics.sessions:true}") boolean sessions,
            @Value("${vaadin.metrics.uis:true}") boolean uis,
            @Value("${vaadin.metrics.navigation:true}") boolean navigation,
            @Value("${vaadin.metrics.requests:true}") boolean requests,
            @Value("${vaadin.metrics.errors:true}") boolean errors,
            @Value("${vaadin.metrics.route-cardinality-limit:"
                    + VaadinMetricsConfig.DEFAULT_ROUTE_CARDINALITY_LIMIT
                    + "}") int routeCardinalityLimit) {
        return VaadinMetricsConfig.builder().sessions(sessions).uis(uis)
                .navigation(navigation).requests(requests).errors(errors)
                .routeCardinalityLimit(routeCardinalityLimit).build();
    }

    @Bean
    public MetricsServiceInitListener metricsServiceInitListener(
            MeterRegistry registry, VaadinMetricsConfig config) {
        return new MetricsServiceInitListener(registry, config);
    }
}
