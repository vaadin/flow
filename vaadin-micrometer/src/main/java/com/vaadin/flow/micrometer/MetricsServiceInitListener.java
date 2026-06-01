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
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;

import com.vaadin.flow.micrometer.trace.TracingExecutor;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Wires the Vaadin Flow Micrometer binders into a {@link VaadinService} when
 * the service initializes.
 * <p>
 * Three construction paths:
 * <ul>
 * <li>Spring/Boot — instantiated as a bean with explicit {@code registry},
 * {@code observationRegistry} (optional), and {@code config} arguments.</li>
 * <li>Standalone — the no-arg constructor is invoked by the Java
 * {@link java.util.ServiceLoader}; the registry and configuration are looked up
 * from {@link VaadinMicrometer} at {@code serviceInit} time. If
 * {@link VaadinMicrometer#install(MeterRegistry, VaadinMetricsConfig)} was
 * never called, the listener silently no-ops.</li>
 * </ul>
 * <p>
 * When an {@link ObservationRegistry} is available and
 * {@link VaadinMetricsConfig#isTraces()} is on, the listener also wraps the
 * service's executor with a {@link TracingExecutor} so trace context flows
 * across {@code UI.access(...)} boundaries.
 */
public class MetricsServiceInitListener implements VaadinServiceInitListener {

    private final MeterRegistry registry;
    private final ObservationRegistry observationRegistry;
    private final VaadinMetricsConfig config;

    /**
     * Constructor used by {@link java.util.ServiceLoader}. Resolves the
     * registry, observation registry, and configuration lazily from
     * {@link VaadinMicrometer}.
     */
    public MetricsServiceInitListener() {
        this.registry = null;
        this.observationRegistry = null;
        this.config = null;
    }

    /**
     * Constructor used by DI containers that don't provide an
     * {@link ObservationRegistry}.
     */
    public MetricsServiceInitListener(MeterRegistry registry,
            VaadinMetricsConfig config) {
        this(registry, null, config);
    }

    /**
     * Constructor used by DI containers.
     *
     * @param registry
     *            Micrometer meter registry, not {@code null}
     * @param observationRegistry
     *            Micrometer observation registry, may be {@code null} to
     *            disable Observation-based instrumentation
     * @param config
     *            instrumentation configuration, not {@code null}
     */
    public MetricsServiceInitListener(MeterRegistry registry,
            ObservationRegistry observationRegistry,
            VaadinMetricsConfig config) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.observationRegistry = observationRegistry;
        this.config = Objects.requireNonNull(config, "config");
        if (observationRegistry != null && config.isTraces()) {
            installDefaultObservationHandlers(observationRegistry, registry);
        }
    }

    /**
     * Registers default {@link io.micrometer.observation.ObservationHandler}s
     * that make Observations produce
     * {@link io.micrometer.core.instrument.Timer}s.
     * <p>
     * The default implementation installs
     * {@link DefaultMeterObservationHandler}. Spring Boot deployments override
     * this method to no-op because the Boot Actuator's
     * {@code ObservationAutoConfiguration} already registers the same handler.
     */
    protected void installDefaultObservationHandlers(
            ObservationRegistry observationRegistry, MeterRegistry registry) {
        observationRegistry.observationConfig().observationHandler(
                new DefaultMeterObservationHandler(registry));
    }

    /**
     * Hook for DI integrations to enrich the framework-level HTTP observation
     * (e.g. Spring's {@code ServerHttpObservationFilter} span) with
     * Vaadin-specific information so the parent HTTP span renders informatively
     * in the trace UI. Called from {@link RequestMetricsBinder} after the
     * Vaadin request type has been determined and before the
     * {@code vaadin.request.<type>} child observation is started.
     * <p>
     * Default implementation no-ops, keeping the framework-agnostic core free
     * of Spring imports. The Spring/Boot integration modules override this to
     * call into their respective HTTP-observation APIs.
     */
    protected void enrichHttpObservation(VaadinRequest request, String type) {
        // no-op by default
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
        ObservationRegistry or = observationRegistry != null
                ? observationRegistry
                : VaadinMicrometer.observationRegistry();
        bind(event, r, or, c);
    }

    void bind(ServiceInitEvent event, MeterRegistry registry,
            ObservationRegistry observationRegistry,
            VaadinMetricsConfig config) {
        VaadinService service = event.getSource();

        if (config.isSessions()) {
            SessionMetricsBinder sessions = new SessionMetricsBinder(registry);
            service.addSessionInitListener(sessions);
            service.addSessionDestroyListener(sessions);
            service.addSessionLockListener(
                    new SessionLockMetricsBinder(registry));
        }

        if (config.isUis() || config.isNavigation()) {
            service.addUIInitListener(
                    new UiMetricsBinder(registry, observationRegistry, config));
        }

        if (config.isRequests() || config.isErrors()) {
            event.addVaadinRequestInterceptor(new RequestMetricsBinder(registry,
                    observationRegistry, config, this::enrichHttpObservation));
        }

        if (config.isTraces() && observationRegistry != null) {
            event.getExecutor().ifPresent(exec -> event.setExecutor(
                    new TracingExecutor(exec, observationRegistry)));
        }
    }
}
