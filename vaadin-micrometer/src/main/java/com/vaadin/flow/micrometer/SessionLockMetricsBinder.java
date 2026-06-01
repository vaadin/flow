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
 * hold start times are kept in thread locals.
 */
final class SessionLockMetricsBinder implements SessionLockListener {

    private final MeterRegistry registry;
    private final transient ThreadLocal<Long> waitStart = new ThreadLocal<>();
    private final transient ThreadLocal<Long> holdStart = new ThreadLocal<>();

    SessionLockMetricsBinder(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void lockRequested(SessionLockEvent event) {
        waitStart.set(System.nanoTime());
    }

    @Override
    public void lockAcquired(SessionLockEvent event) {
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
