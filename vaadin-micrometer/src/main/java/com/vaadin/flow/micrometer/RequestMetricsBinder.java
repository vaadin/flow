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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Measures request duration and counts errors. The Timer sample is stored on a
 * {@link ThreadLocal} because Flow's
 * {@link VaadinRequestInterceptor#requestStart} and
 * {@link VaadinRequestInterceptor#requestEnd} fire on the same thread for a
 * given request.
 */
final class RequestMetricsBinder implements VaadinRequestInterceptor {

    private final MeterRegistry registry;
    private final VaadinMetricsConfig config;
    private final ThreadLocal<Timer.Sample> sample = new ThreadLocal<>();
    private final ThreadLocal<Boolean> errored = ThreadLocal
            .withInitial(() -> Boolean.FALSE);

    RequestMetricsBinder(MeterRegistry registry, VaadinMetricsConfig config) {
        this.registry = registry;
        this.config = config;
    }

    @Override
    public void requestStart(VaadinRequest request, VaadinResponse response) {
        if (config.isRequests()) {
            sample.set(Timer.start(registry));
        }
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
    }

    @Override
    public void requestEnd(VaadinRequest request, VaadinResponse response,
            VaadinSession session) {
        Timer.Sample s = sample.get();
        sample.remove();
        boolean wasError = errored.get();
        errored.remove();
        if (s != null) {
            s.stop(registry.timer(MeterNames.REQUEST_DURATION,
                    MeterNames.TAG_OUTCOME, wasError ? MeterNames.OUTCOME_ERROR
                            : MeterNames.OUTCOME_SUCCESS));
        }
    }
}
