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
package com.vaadin.flow.uitest.ui.routing;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.osgi.OSGiMarker;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

@Route("com.vaadin.flow.uitest.ui.routing.PushRouteWildcardParameterView")
public class PushRouteWildcardParameterView extends Div
        implements HasUrlParameter<String> {

    private final ScheduledExecutorService executor = Executors
            .newScheduledThreadPool(1);

    public static final String LABEL_ID = "label";
    private final Span label = new Span();

    public PushRouteWildcardParameterView() {
        Lookup lookup = VaadinService.getCurrent().getContext()
                .getAttribute(Lookup.class);
        PushConfiguration pushConfiguration = UI.getCurrent()
                .getPushConfiguration();
        pushConfiguration.setPushMode(PushMode.AUTOMATIC);
        if (lookup.lookup(OSGiMarker.class) == null) {
            pushConfiguration.setTransport(Transport.WEBSOCKET_XHR);
        } else {
            pushConfiguration.setTransport(Transport.LONG_POLLING);
        }
    }

    @Override
    public void setParameter(BeforeEvent event,
            @WildcardParameter String parameter) {
        UI ui = event.getUI();
        executor.schedule(() -> {
            ui.access(() -> {
                label.setId(LABEL_ID);
                add(label);
                label.setText(parameter);
            });
        }, 100, TimeUnit.MILLISECONDS);
    }
}
