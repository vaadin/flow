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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationListener;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;

/**
 * Times each navigation from {@code beforeEnter} to {@code afterNavigation}.
 * Per-UI sample state is stored as a UI attribute via {@link ComponentUtil} so
 * each UI's navigations are tracked independently.
 */
final class NavigationMetricsBinder
        implements BeforeEnterListener, AfterNavigationListener {

    private static final String SAMPLE_KEY = NavigationMetricsBinder.class
            .getName() + ".sample";
    private static final String ROUTE_KEY = NavigationMetricsBinder.class
            .getName() + ".route";

    private final MeterRegistry registry;
    private final RouteTagResolver routes;

    NavigationMetricsBinder(MeterRegistry registry, RouteTagResolver routes) {
        this.registry = registry;
        this.routes = routes;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        UI ui = event.getUI();
        Timer.Sample sample = Timer.start(registry);
        ComponentUtil.setData(ui, SAMPLE_KEY, sample);
        ComponentUtil.setData(ui, ROUTE_KEY,
                routes.tagFor(event.getNavigationTarget()));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }
        Object sample = ComponentUtil.getData(ui, SAMPLE_KEY);
        Object route = ComponentUtil.getData(ui, ROUTE_KEY);
        ComponentUtil.setData(ui, SAMPLE_KEY, null);
        ComponentUtil.setData(ui, ROUTE_KEY, null);
        if (sample instanceof Timer.Sample s) {
            s.stop(registry.timer(MeterNames.NAVIGATION, MeterNames.TAG_ROUTE,
                    route instanceof String r ? r : MeterNames.ROUTE_UNKNOWN,
                    MeterNames.TAG_OUTCOME, MeterNames.OUTCOME_SUCCESS));
        }
    }
}
