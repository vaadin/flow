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

import java.time.Duration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import com.vaadin.flow.server.SessionLockEvent;
import com.vaadin.flow.server.SessionLockListener;
import com.vaadin.flow.server.VaadinService;

/**
 * Records session-lock wait and hold times from Flow's
 * {@link SessionLockListener} SPI.
 * <p>
 * Vaadin serializes all server-side work for a session behind one lock, so
 * {@code vaadin.session.lock.wait} (time blocked acquiring the lock) is the
 * session-contention signal, and {@code vaadin.session.lock.hold} (time the
 * lock was held) is how long each unit of work monopolized the session. The
 * {@code context} tag distinguishes request-thread acquisitions from
 * {@code UI.access}/background acquisitions.
 * <p>
 * The SPI delivers {@code lockRequested} → {@code lockAcquired} →
 * {@code lockReleased} on the same thread for one outermost hold, so wait and
 * hold state is kept in thread locals.
 * <p>
 * Two modes, mirroring {@link RequestMetricsBinder}:
 * <ul>
 * <li>When {@code config.isTraces()} and an {@link ObservationRegistry} is
 * available, each phase is driven through the Observation API. The Observation
 * name matches the Timer name so a {@code DefaultMeterObservationHandler}
 * produces the same Timer the direct-recording path would, while a tracing
 * handler additionally emits a span. Because the lock is acquired while the
 * request (or {@code UI.access}) Observation scope is open, the wait and hold
 * spans nest underneath it, showing on the trace timeline how much of the work
 * was spent blocked on the session lock versus inside the critical
 * section.</li>
 * <li>Otherwise the binder records the Timers directly, preserving behavior for
 * standalone deployments without Observation configured.</li>
 * </ul>
 */
final class SessionLockMetricsBinder implements SessionLockListener {

    private final MeterRegistry registry;
    private final transient ObservationRegistry observationRegistry;
    private final boolean traces;
    private final transient ThreadLocal<Long> waitStart = new ThreadLocal<>();
    private final transient ThreadLocal<Long> holdStart = new ThreadLocal<>();
    private final transient ThreadLocal<Observation> waitObservation = new ThreadLocal<>();
    private final transient ThreadLocal<Observation> holdObservation = new ThreadLocal<>();
    private final transient ThreadLocal<Observation.Scope> holdScope = new ThreadLocal<>();

    SessionLockMetricsBinder(MeterRegistry registry) {
        this(registry, null, false);
    }

    SessionLockMetricsBinder(MeterRegistry registry,
            ObservationRegistry observationRegistry, boolean traces) {
        this.registry = registry;
        this.observationRegistry = observationRegistry;
        this.traces = traces;
    }

    private boolean useObservation() {
        return traces && observationRegistry != null;
    }

    @Override
    public void lockRequested(SessionLockEvent event) {
        if (useObservation()) {
            // Started while the request/UI.access Observation scope is open, so
            // the wait span parents to it. Stopped on acquire; the
            // DefaultMeterObservationHandler then produces the wait Timer.
            Observation obs = Observation
                    .createNotStarted(MeterNames.SESSION_LOCK_WAIT,
                            observationRegistry)
                    .contextualName(MeterNames.SESSION_LOCK_WAIT)
                    .lowCardinalityKeyValue(MeterNames.TAG_CONTEXT, context())
                    .start();
            waitObservation.set(obs);
        } else {
            waitStart.set(System.nanoTime());
        }
    }

    @Override
    public void lockAcquired(SessionLockEvent event) {
        if (useObservation()) {
            Observation wait = waitObservation.get();
            waitObservation.remove();
            if (wait != null) {
                wait.stop();
            }
            Observation hold = Observation
                    .createNotStarted(MeterNames.SESSION_LOCK_HOLD,
                            observationRegistry)
                    .contextualName(MeterNames.SESSION_LOCK_HOLD)
                    .lowCardinalityKeyValue(MeterNames.TAG_CONTEXT, context())
                    .start();
            holdObservation.set(hold);
            // Keep the hold span current for the duration of the critical
            // section so per-invocation RPC spans nest underneath it.
            holdScope.set(hold.openScope());
            return;
        }
        long now = System.nanoTime();
        Long started = waitStart.get();
        waitStart.remove();
        String context = context();
        if (started != null) {
            registry.timer(MeterNames.SESSION_LOCK_WAIT, MeterNames.TAG_CONTEXT,
                    context).record(Duration.ofNanos(now - started));
        }
        holdStart.set(now);
    }

    @Override
    public void lockReleased(SessionLockEvent event) {
        if (useObservation()) {
            Observation.Scope scope = holdScope.get();
            holdScope.remove();
            if (scope != null) {
                scope.close();
            }
            Observation hold = holdObservation.get();
            holdObservation.remove();
            if (hold != null) {
                hold.stop();
            }
            return;
        }
        long now = System.nanoTime();
        Long started = holdStart.get();
        holdStart.remove();
        if (started != null) {
            registry.timer(MeterNames.SESSION_LOCK_HOLD, MeterNames.TAG_CONTEXT,
                    context()).record(Duration.ofNanos(now - started));
        }
    }

    /**
     * Best-effort classification: a lock taken while a Vaadin request is
     * current is attributed to request handling, otherwise to a
     * {@code UI.access}/background acquisition.
     */
    private static String context() {
        return VaadinService.getCurrentRequest() != null
                ? MeterNames.CONTEXT_REQUEST
                : MeterNames.CONTEXT_ACCESS;
    }
}
