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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.VaadinSession;

/**
 * Tracks session lifecycle metrics: cumulative count, currently-active count,
 * and per-session lifetime.
 */
final class SessionMetricsBinder
        implements SessionInitListener, SessionDestroyListener {

    private final Counter created;
    private final AtomicLong active = new AtomicLong();
    private final Timer duration;
    private final Map<VaadinSession, Long> startNanos = new ConcurrentHashMap<>();

    SessionMetricsBinder(MeterRegistry registry) {
        this.created = Counter.builder(MeterNames.SESSIONS_CREATED)
                .register(registry);
        Gauge.builder(MeterNames.SESSIONS_ACTIVE, active, AtomicLong::get)
                .register(registry);
        this.duration = Timer.builder(MeterNames.SESSIONS_DURATION)
                .register(registry);
    }

    @Override
    public void sessionInit(SessionInitEvent event) {
        created.increment();
        active.incrementAndGet();
        startNanos.put(event.getSession(), System.nanoTime());
    }

    @Override
    public void sessionDestroy(SessionDestroyEvent event) {
        Long start = startNanos.remove(event.getSession());
        active.decrementAndGet();
        if (start != null) {
            duration.record(Duration.ofNanos(System.nanoTime() - start));
        }
    }
}
