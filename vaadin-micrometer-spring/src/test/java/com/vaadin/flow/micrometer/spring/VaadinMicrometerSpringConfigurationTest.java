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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockPropertySource;

import com.vaadin.flow.micrometer.MetricsServiceInitListener;
import com.vaadin.flow.micrometer.VaadinMetricsConfig;

public class VaadinMicrometerSpringConfigurationTest {

    @Configuration
    @Import(VaadinMicrometerSpringConfiguration.class)
    static class TestConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }

    @Test
    public void defaultsExposeBindersAndConfig() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                TestConfig.class)) {
            VaadinMetricsConfig config = ctx.getBean(VaadinMetricsConfig.class);
            Assert.assertTrue(config.isSessions());
            Assert.assertTrue(config.isUis());
            Assert.assertTrue(config.isNavigation());
            Assert.assertTrue(config.isRequests());
            Assert.assertTrue(config.isErrors());
            Assert.assertEquals(
                    VaadinMetricsConfig.DEFAULT_ROUTE_CARDINALITY_LIMIT,
                    config.getRouteCardinalityLimit());

            Assert.assertNotNull(ctx.getBean(MetricsServiceInitListener.class));
        }
    }

    @Test
    public void propertyOverridesAreHonored() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            MockPropertySource props = new MockPropertySource()
                    .withProperty("vaadin.metrics.sessions", "false")
                    .withProperty("vaadin.metrics.navigation", "false")
                    .withProperty("vaadin.metrics.route-cardinality-limit",
                            "42");
            ((ConfigurableEnvironment) ctx.getEnvironment())
                    .getPropertySources().addFirst(props);
            ctx.register(TestConfig.class);
            ctx.refresh();

            VaadinMetricsConfig config = ctx.getBean(VaadinMetricsConfig.class);
            Assert.assertFalse(config.isSessions());
            Assert.assertFalse(config.isNavigation());
            Assert.assertTrue(config.isUis());
            Assert.assertEquals(42, config.getRouteCardinalityLimit());
        }
    }
}
