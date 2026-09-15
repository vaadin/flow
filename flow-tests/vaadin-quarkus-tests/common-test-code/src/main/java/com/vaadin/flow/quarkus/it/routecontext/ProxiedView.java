/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.routecontext;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.quarkus.it.instantiator.Useless;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.quarkus.annotation.RouteScopeOwner;
import com.vaadin.quarkus.annotation.RouteScoped;

@Route(value = "proxied-route-scope", layout = MainLayout.class)
@RouteScoped
// Interceptor binding causes Arc to create a proxy subclass for this view
@Useless
public class ProxiedView extends AbstractCountedView
        implements BeforeEnterObserver, AfterNavigationObserver {

    public static final String PRESENTER_LABEL = "PRESENTER_STATUS";

    @Inject
    Event<String> viewEvent;

    @Inject
    @RouteScopeOwner(ProxiedView.class)
    ProxiedViewPresenter presenter;

    private Span presenterLabel;

    @PostConstruct
    private void init() {
        presenterLabel = new Span();
        presenterLabel.setId(PRESENTER_LABEL);
        add(new Span("PROXIED"), presenterLabel);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        viewEvent.fire("beforeEnter");
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        viewEvent.fire("afterNavigation");
        presenterLabel.setText(presenter.getLastEvent());
    }
}
