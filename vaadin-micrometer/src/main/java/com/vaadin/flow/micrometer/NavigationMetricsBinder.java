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
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.micrometer.trace.VaadinObservationNames;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationListener;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;

/**
 * Times each navigation from {@code beforeEnter} to {@code afterNavigation}.
 * <p>
 * When an {@link ObservationRegistry} is supplied and
 * {@link VaadinMetricsConfig#isTraces()} is on, the navigation is observed
 * (producing both a span and, through a registered
 * {@code DefaultMeterObservationHandler}, the Timer). Otherwise the binder
 * falls back to direct Timer recording. Per-UI state is stored as a UI
 * attribute so concurrent UIs are tracked independently.
 */
final class NavigationMetricsBinder
        implements BeforeEnterListener, AfterNavigationListener {

    private static final String SAMPLE_KEY = NavigationMetricsBinder.class
            .getName() + ".sample";
    private static final String ROUTE_KEY = NavigationMetricsBinder.class
            .getName() + ".route";
    private static final String OBSERVATION_KEY = NavigationMetricsBinder.class
            .getName() + ".observation";
    private static final String OBSERVATION_SCOPE_KEY = NavigationMetricsBinder.class
            .getName() + ".observation.scope";

    private final MeterRegistry registry;
    private final ObservationRegistry observationRegistry;
    private final VaadinMetricsConfig config;
    private final RouteTagResolver routes;

    NavigationMetricsBinder(MeterRegistry registry, RouteTagResolver routes) {
        this(registry, null, VaadinMetricsConfig.defaults(), routes);
    }

    NavigationMetricsBinder(MeterRegistry registry,
            ObservationRegistry observationRegistry, VaadinMetricsConfig config,
            RouteTagResolver routes) {
        this.registry = registry;
        this.observationRegistry = observationRegistry;
        this.config = config;
        this.routes = routes;
    }

    private boolean useObservation() {
        return config.isTraces() && observationRegistry != null;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        UI ui = event.getUI();
        String route = routes.tagFor(event.getNavigationTarget());
        ComponentUtil.setData(ui, ROUTE_KEY, route);
        if (useObservation()) {
            Observation obs = Observation
                    .createNotStarted(MeterNames.NAVIGATION,
                            observationRegistry)
                    .contextualName(
                            VaadinObservationNames.NAVIGATION + " " + route)
                    .lowCardinalityKeyValue(VaadinObservationNames.KEY_ROUTE,
                            route)
                    .start();
            ComponentUtil.setData(ui, OBSERVATION_KEY, obs);
            ComponentUtil.setData(ui, OBSERVATION_SCOPE_KEY, obs.openScope());
        } else {
            ComponentUtil.setData(ui, SAMPLE_KEY, Timer.start(registry));
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }
        Object sample = ComponentUtil.getData(ui, SAMPLE_KEY);
        Object route = ComponentUtil.getData(ui, ROUTE_KEY);
        Object obsObj = ComponentUtil.getData(ui, OBSERVATION_KEY);
        Object scopeObj = ComponentUtil.getData(ui, OBSERVATION_SCOPE_KEY);
        ComponentUtil.setData(ui, SAMPLE_KEY, null);
        ComponentUtil.setData(ui, ROUTE_KEY, null);
        ComponentUtil.setData(ui, OBSERVATION_KEY, null);
        ComponentUtil.setData(ui, OBSERVATION_SCOPE_KEY, null);
        if (sample instanceof Timer.Sample s) {
            s.stop(registry.timer(MeterNames.NAVIGATION, MeterNames.TAG_ROUTE,
                    route instanceof String r ? r : MeterNames.ROUTE_UNKNOWN,
                    MeterNames.TAG_OUTCOME, MeterNames.OUTCOME_SUCCESS));
        }
        if (scopeObj instanceof Observation.Scope scope) {
            scope.close();
        }
        if (obsObj instanceof Observation obs) {
            obs.lowCardinalityKeyValue(VaadinObservationNames.KEY_OUTCOME,
                    VaadinObservationNames.OUTCOME_SUCCESS);
            obs.stop();
        }
    }
}
