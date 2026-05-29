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

import java.util.function.BiConsumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import com.vaadin.flow.micrometer.trace.VaadinObservationNames;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Measures request duration and counts errors.
 * <p>
 * Two modes:
 * <ul>
 * <li>If {@code config.isTraces()} and an {@link ObservationRegistry} is
 * supplied, requests are driven through the Observation API. The Observation
 * name matches the Timer name ({@link MeterNames#REQUEST_DURATION}) so a
 * {@code DefaultMeterObservationHandler} produces the same Timer that the
 * direct-recording path would. The Observation's {@code contextualName} carries
 * the span-friendly name ({@code vaadin.request}) used by tracing
 * handlers.</li>
 * <li>Otherwise (no obs registry / traces disabled / observation handler
 * unavailable), the binder falls back to recording the Timer directly. This
 * keeps backward compatibility for standalone deployments that haven't
 * configured Observation.</li>
 * </ul>
 */
final class RequestMetricsBinder implements VaadinRequestInterceptor {

    private static final BiConsumer<VaadinRequest, String> NO_ENRICH = (r,
            t) -> {
    };

    private final MeterRegistry registry;
    private final ObservationRegistry observationRegistry;
    private final VaadinMetricsConfig config;
    private final BiConsumer<VaadinRequest, String> httpObservationEnricher;
    private final ThreadLocal<Timer.Sample> sample = new ThreadLocal<>();
    private final ThreadLocal<Boolean> errored = ThreadLocal
            .withInitial(() -> Boolean.FALSE);
    private final ThreadLocal<Observation> observation = new ThreadLocal<>();
    private final ThreadLocal<Observation.Scope> observationScope = new ThreadLocal<>();

    RequestMetricsBinder(MeterRegistry registry, VaadinMetricsConfig config) {
        this(registry, null, config, NO_ENRICH);
    }

    RequestMetricsBinder(MeterRegistry registry,
            ObservationRegistry observationRegistry,
            VaadinMetricsConfig config) {
        this(registry, observationRegistry, config, NO_ENRICH);
    }

    RequestMetricsBinder(MeterRegistry registry,
            ObservationRegistry observationRegistry, VaadinMetricsConfig config,
            BiConsumer<VaadinRequest, String> httpObservationEnricher) {
        this.registry = registry;
        this.observationRegistry = observationRegistry;
        this.config = config;
        this.httpObservationEnricher = httpObservationEnricher != null
                ? httpObservationEnricher
                : NO_ENRICH;
    }

    private boolean useObservation() {
        return config.isTraces() && observationRegistry != null;
    }

    @Override
    public void requestStart(VaadinRequest request, VaadinResponse response) {
        if (useObservation()) {
            String type = requestType(request);
            // Let DI integrations (Spring/Boot) lift Vaadin type into the
            // parent HTTP span so the trace UI shows the request type
            // without drilling down. Defaults to no-op for standalone.
            httpObservationEnricher.accept(request, type);
            Observation obs = Observation
                    .createNotStarted(MeterNames.REQUEST_DURATION,
                            observationRegistry)
                    .contextualName(VaadinObservationNames.REQUEST + "." + type)
                    .lowCardinalityKeyValue(
                            VaadinObservationNames.KEY_REQUEST_TYPE, type)
                    .lowCardinalityKeyValue(
                            VaadinObservationNames.KEY_HTTP_METHOD,
                            httpMethod(request))
                    .start();
            observation.set(obs);
            observationScope.set(obs.openScope());
        } else if (config.isRequests()) {
            sample.set(Timer.start(registry));
        }
    }

    private static String httpMethod(VaadinRequest request) {
        if (request == null) {
            return "unknown";
        }
        String m = request.getMethod();
        return m == null ? "unknown" : m;
    }

    @Override
    public void handleException(VaadinRequest request, VaadinResponse response,
            VaadinSession vaadinSession, Exception t) {
        errored.set(Boolean.TRUE);
        if (config.isErrors() && t != null) {
            Counter.builder(MeterNames.ERRORS)
                    .tag(MeterNames.TAG_EXCEPTION, t.getClass().getSimpleName())
                    .register(registry).increment();
        }
        Observation obs = observation.get();
        if (obs != null && t != null) {
            obs.error(t);
        }
    }

    @Override
    public void requestEnd(VaadinRequest request, VaadinResponse response,
            VaadinSession session) {
        boolean wasError = errored.get();
        errored.remove();
        String outcome = wasError ? MeterNames.OUTCOME_ERROR
                : MeterNames.OUTCOME_SUCCESS;
        Timer.Sample s = sample.get();
        sample.remove();
        if (s != null) {
            s.stop(registry.timer(MeterNames.REQUEST_DURATION,
                    MeterNames.TAG_OUTCOME, outcome));
        }
        Observation.Scope scope = observationScope.get();
        observationScope.remove();
        if (scope != null) {
            scope.close();
        }
        Observation obs = observation.get();
        observation.remove();
        if (obs != null) {
            obs.lowCardinalityKeyValue(VaadinObservationNames.KEY_OUTCOME,
                    outcome);
            obs.stop();
        }
    }

    private static String requestType(VaadinRequest request) {
        if (request == null) {
            return VaadinObservationNames.REQUEST_TYPE_OTHER;
        }
        String path = request.getPathInfo();
        if (path != null) {
            if (path.contains("/PUSH/")) {
                return VaadinObservationNames.REQUEST_TYPE_PUSH;
            }
            if (path.contains("/HEARTBEAT/")) {
                return VaadinObservationNames.REQUEST_TYPE_HEARTBEAT;
            }
        }
        String vr = request.getParameter("v-r");
        if ("uidl".equals(vr)) {
            return VaadinObservationNames.REQUEST_TYPE_UIDL;
        }
        if ("heartbeat".equals(vr)) {
            return VaadinObservationNames.REQUEST_TYPE_HEARTBEAT;
        }
        if (path != null && (path.startsWith("/VAADIN/")
                || path.startsWith("/static/"))) {
            return VaadinObservationNames.REQUEST_TYPE_STATIC;
        }
        return VaadinObservationNames.REQUEST_TYPE_OTHER;
    }
}
