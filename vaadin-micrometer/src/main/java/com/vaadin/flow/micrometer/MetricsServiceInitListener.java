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

import io.micrometer.core.instrument.MeterRegistry;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Wires the Vaadin Flow Micrometer binders into a {@link VaadinService} when
 * the service initializes.
 * <p>
 * Two construction paths:
 * <ul>
 * <li>Spring/Boot — instantiated as a bean with explicit {@code registry} and
 * {@code config} arguments.</li>
 * <li>Standalone — the no-arg constructor is invoked by the Java
 * {@link java.util.ServiceLoader}; the registry and configuration are looked up
 * from {@link VaadinMicrometer} at {@code serviceInit} time. If
 * {@link VaadinMicrometer#install(MeterRegistry, VaadinMetricsConfig)} was
 * never called, the listener silently no-ops.</li>
 * </ul>
 */
public class MetricsServiceInitListener implements VaadinServiceInitListener {

    private final MeterRegistry registry;
    private final VaadinMetricsConfig config;

    /**
     * Constructor used by {@link java.util.ServiceLoader}. Resolves the
     * registry and configuration lazily from {@link VaadinMicrometer}.
     */
    public MetricsServiceInitListener() {
        this.registry = null;
        this.config = null;
    }

    /**
     * Constructor used by DI containers.
     *
     * @param registry
     *            Micrometer meter registry, not {@code null}
     * @param config
     *            instrumentation configuration, not {@code null}
     */
    public MetricsServiceInitListener(MeterRegistry registry,
            VaadinMetricsConfig config) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        MeterRegistry r = registry != null ? registry
                : VaadinMicrometer.registry();
        VaadinMetricsConfig c = config != null ? config
                : VaadinMicrometer.config();
        if (r == null || c == null) {
            return;
        }
        bind(event, r, c);
    }

    static void bind(ServiceInitEvent event, MeterRegistry registry,
            VaadinMetricsConfig config) {
        VaadinService service = event.getSource();

        if (config.isSessions()) {
            SessionMetricsBinder sessions = new SessionMetricsBinder(registry);
            service.addSessionInitListener(sessions);
            service.addSessionDestroyListener(sessions);
        }

        if (config.isUis() || config.isNavigation()) {
            service.addUIInitListener(new UiMetricsBinder(registry, config));
        }

        if (config.isRequests() || config.isErrors()) {
            event.addVaadinRequestInterceptor(
                    new RequestMetricsBinder(registry, config));
        }
    }
}
