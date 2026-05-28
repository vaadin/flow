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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vaadin.flow.micrometer.MetricsServiceInitListener;
import com.vaadin.flow.micrometer.VaadinMetricsConfig;

public class VaadinMetricsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations
                    .of(VaadinMetricsAutoConfiguration.class));

    @Configuration
    static class RegistryConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Test
    public void autoConfigSkippedWhenNoMeterRegistry() {
        contextRunner.run(ctx -> Assert
                .assertFalse(ctx.containsBean("metricsServiceInitListener")));
    }

    @Test
    public void autoConfigEnabledByDefaultWhenRegistryPresent() {
        contextRunner.withUserConfiguration(RegistryConfig.class).run(ctx -> {
            Assert.assertTrue(ctx.containsBean("metricsServiceInitListener"));
            Assert.assertNotNull(ctx.getBean(MetricsServiceInitListener.class));
            VaadinMetricsConfig config = ctx.getBean(VaadinMetricsConfig.class);
            Assert.assertTrue(config.isSessions());
            Assert.assertEquals(
                    VaadinMetricsConfig.DEFAULT_ROUTE_CARDINALITY_LIMIT,
                    config.getRouteCardinalityLimit());
        });
    }

    @Test
    public void autoConfigDisabledViaProperty() {
        contextRunner.withUserConfiguration(RegistryConfig.class)
                .withPropertyValues("vaadin.metrics.enabled=false")
                .run(ctx -> Assert.assertFalse(
                        ctx.containsBean("metricsServiceInitListener")));
    }

    @Test
    public void propertyOverridesAreReflected() {
        contextRunner.withUserConfiguration(RegistryConfig.class)
                .withPropertyValues("vaadin.metrics.sessions=false",
                        "vaadin.metrics.route-cardinality-limit=7")
                .run(ctx -> {
                    VaadinMetricsConfig config = ctx
                            .getBean(VaadinMetricsConfig.class);
                    Assert.assertFalse(config.isSessions());
                    Assert.assertEquals(7, config.getRouteCardinalityLimit());
                });
    }

    @Test
    public void userSuppliedListenerOverridesAuto() {
        contextRunner.withUserConfiguration(RegistryConfig.class,
                UserListenerConfig.class).run(ctx -> {
                    MetricsServiceInitListener listener = ctx
                            .getBean(MetricsServiceInitListener.class);
                    Assert.assertSame(UserListenerConfig.USER_LISTENER,
                            listener);
                });
    }

    @Configuration
    static class UserListenerConfig {
        static final MetricsServiceInitListener USER_LISTENER = new MetricsServiceInitListener(
                new SimpleMeterRegistry(), VaadinMetricsConfig.defaults());

        @Bean
        MetricsServiceInitListener metricsServiceInitListener() {
            return USER_LISTENER;
        }
    }
}
