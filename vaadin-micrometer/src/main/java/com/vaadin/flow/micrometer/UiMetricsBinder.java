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

import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.micrometer.client.ClientMetricsBinder;
import com.vaadin.flow.micrometer.client.MetricsCollectorElement;
import com.vaadin.flow.micrometer.trace.VaadinObservationNames;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;

/**
 * Tracks per-UI lifecycle metrics and, when navigation metrics are enabled,
 * attaches a {@link NavigationMetricsBinder} to each newly initialized UI.
 */
final class UiMetricsBinder implements UIInitListener {

    private final MeterRegistry registry;
    private final ObservationRegistry observationRegistry;
    private final VaadinMetricsConfig config;
    private final Counter created;
    private final AtomicLong active = new AtomicLong();
    private final NavigationMetricsBinder navigationBinder;
    private final ClientMetricsBinder clientBinder;

    UiMetricsBinder(MeterRegistry registry, VaadinMetricsConfig config) {
        this(registry, null, config);
    }

    UiMetricsBinder(MeterRegistry registry,
            ObservationRegistry observationRegistry,
            VaadinMetricsConfig config) {
        this.registry = registry;
        this.observationRegistry = observationRegistry;
        this.config = config;
        this.created = Counter.builder(MeterNames.UI_CREATED)
                .register(registry);
        Gauge.builder(MeterNames.UI_ACTIVE, active, AtomicLong::get)
                .register(registry);
        this.navigationBinder = config.isNavigation()
                ? new NavigationMetricsBinder(registry, observationRegistry,
                        config,
                        new RouteTagResolver(config.getRouteCardinalityLimit()))
                : null;
        this.clientBinder = config.isClient()
                ? new ClientMetricsBinder(registry, config)
                : null;
    }

    @Override
    public void uiInit(UIInitEvent event) {
        UI ui = event.getUI();
        if (config.isUis()) {
            created.increment();
            active.incrementAndGet();
            ui.addDetachListener(e -> active.decrementAndGet());
        }
        if (navigationBinder != null) {
            ui.addBeforeEnterListener(navigationBinder);
            ui.addAfterNavigationListener(navigationBinder);
        }
        if (config.isTraces() && observationRegistry != null) {
            // Polls are the high-frequency UIDL noise; labelling them lets the
            // request span read "vaadin.request.poll" instead of an opaque
            // "vaadin.request.uidl".
            ui.addPollListener(e -> RequestInteraction
                    .mark(VaadinObservationNames.INTERACTION_POLL));
        }
        if (clientBinder != null) {
            ui.add(new MetricsCollectorElement(clientBinder, config));
        }
    }
}
