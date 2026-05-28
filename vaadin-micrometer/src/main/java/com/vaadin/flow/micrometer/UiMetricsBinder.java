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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;

/**
 * Tracks per-UI lifecycle metrics and, when navigation metrics are enabled,
 * attaches a {@link NavigationMetricsBinder} to each newly initialized UI.
 */
final class UiMetricsBinder implements UIInitListener {

    private final VaadinMetricsConfig config;
    private final Counter created;
    private final AtomicLong active = new AtomicLong();
    private final NavigationMetricsBinder navigationBinder;

    UiMetricsBinder(MeterRegistry registry, VaadinMetricsConfig config) {
        this.config = config;
        this.created = Counter.builder(MeterNames.UI_CREATED)
                .register(registry);
        Gauge.builder(MeterNames.UI_ACTIVE, active, AtomicLong::get)
                .register(registry);
        this.navigationBinder = config.isNavigation()
                ? new NavigationMetricsBinder(registry,
                        new RouteTagResolver(config.getRouteCardinalityLimit()))
                : null;
    }

    @Override
    public void uiInit(UIInitEvent event) {
        if (config.isUis()) {
            created.increment();
            active.incrementAndGet();
            UI ui = event.getUI();
            ui.addDetachListener(e -> active.decrementAndGet());
        }
        if (navigationBinder != null) {
            UI ui = event.getUI();
            ui.addBeforeEnterListener(navigationBinder);
            ui.addAfterNavigationListener(navigationBinder);
        }
    }
}
