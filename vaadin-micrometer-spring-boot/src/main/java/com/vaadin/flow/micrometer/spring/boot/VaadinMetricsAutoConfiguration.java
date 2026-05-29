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
package com.vaadin.flow.micrometer.spring.boot;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.vaadin.flow.micrometer.MetricsServiceInitListener;
import com.vaadin.flow.micrometer.VaadinMetricsConfig;
import com.vaadin.flow.server.VaadinService;

/**
 * Auto-configures the vaadin-micrometer {@link MetricsServiceInitListener} when
 * a {@link MeterRegistry} is present in the Spring context.
 * <p>
 * Activation is gated by the {@code vaadin.metrics.enabled} property (default
 * {@code true}); the listener is also skipped when the user supplies their own
 * {@link MetricsServiceInitListener} bean.
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration" })
@ConditionalOnClass({ MeterRegistry.class, VaadinService.class })
@ConditionalOnProperty(prefix = "vaadin.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(VaadinMetricsProperties.class)
public class VaadinMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public VaadinMetricsConfig vaadinMetricsConfig(
            VaadinMetricsProperties properties) {
        return properties.toConfig();
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean
    public MetricsServiceInitListener metricsServiceInitListener(
            MeterRegistry registry,
            ObjectProvider<ObservationRegistry> observationRegistry,
            VaadinMetricsConfig config) {
        return new BootMetricsServiceInitListener(registry,
                observationRegistry.getIfAvailable(), config);
    }

    /**
     * Boot-aware listener that skips registering
     * {@link io.micrometer.core.instrument.observation.DefaultMeterObservationHandler}
     * because Spring Boot Actuator's {@code ObservationAutoConfiguration}
     * already does so.
     */
    static final class BootMetricsServiceInitListener
            extends MetricsServiceInitListener {

        BootMetricsServiceInitListener(MeterRegistry registry,
                ObservationRegistry observationRegistry,
                VaadinMetricsConfig config) {
            super(registry, observationRegistry, config);
        }

        @Override
        protected void installDefaultObservationHandlers(
                ObservationRegistry observationRegistry,
                MeterRegistry registry) {
            // No-op: Boot Actuator already handles this.
        }

        @Override
        protected void enrichHttpObservation(
                com.vaadin.flow.server.VaadinRequest request, String type) {
            com.vaadin.flow.micrometer.spring.SpringHttpObservationEnricher
                    .enrich(request, type);
        }
    }
}
