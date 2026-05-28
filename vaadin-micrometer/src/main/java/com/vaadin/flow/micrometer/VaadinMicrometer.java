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
package com.vaadin.flow.micrometer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Standalone bootstrap entry point for Vaadin Flow Micrometer instrumentation.
 * <p>
 * Use this from non-Spring deployments (plain servlet container, CDI, etc.) to
 * supply the {@link MeterRegistry} and configuration that the
 * {@link MetricsServiceInitListener} — loaded via Java SPI — will read at
 * service init time.
 * <p>
 * Spring and Spring Boot integrations construct the listener as a bean and do
 * not call {@link #install(MeterRegistry, VaadinMetricsConfig)}.
 */
public final class VaadinMicrometer {

    private static final AtomicReference<MeterRegistry> REGISTRY = new AtomicReference<>();
    private static final AtomicReference<VaadinMetricsConfig> CONFIG = new AtomicReference<>();

    private VaadinMicrometer() {
    }

    /**
     * Installs a registry and configuration for the standalone (SPI-loaded)
     * {@link MetricsServiceInitListener}. Must be called before the first
     * Vaadin service is initialized, typically from a
     * {@link jakarta.servlet.ServletContextListener} or equivalent
     * application-startup hook.
     *
     * @param registry
     *            Micrometer meter registry, not {@code null}
     * @param config
     *            instrumentation configuration, not {@code null}
     */
    public static void install(MeterRegistry registry,
            VaadinMetricsConfig config) {
        REGISTRY.set(Objects.requireNonNull(registry, "registry"));
        CONFIG.set(Objects.requireNonNull(config, "config"));
    }

    /**
     * Clears the installed registry and configuration. Intended for tests and
     * for application shutdown.
     */
    public static void uninstall() {
        REGISTRY.set(null);
        CONFIG.set(null);
    }

    static MeterRegistry registry() {
        return REGISTRY.get();
    }

    static VaadinMetricsConfig config() {
        return CONFIG.get();
    }
}
