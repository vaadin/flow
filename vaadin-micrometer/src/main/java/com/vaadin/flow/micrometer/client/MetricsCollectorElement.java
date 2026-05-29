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
package com.vaadin.flow.micrometer.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.micrometer.VaadinMetricsConfig;

/**
 * Hidden helper component attached to each UI when client metrics are enabled.
 * Exposes a {@link ClientCallable} that receives batches of
 * {@link ClientSample} from the in-browser collector. Loads the client JS on
 * first attach and re-attaches itself to its UI if removed.
 */
@Tag("vaadin-metrics-collector")
public final class MetricsCollectorElement extends Component {

    private static final String CLIENT_INIT_KEY = "vaadinMetricsClientInitialized";
    private static final String CLIENT_RESOURCE = "META-INF/frontend/VaadinMetricsClient.js";

    private final transient ClientMetricsBinder binder;
    private final transient ClientRateLimiter limiter;

    public MetricsCollectorElement(ClientMetricsBinder binder,
            VaadinMetricsConfig config) {
        this.binder = binder;
        this.limiter = new ClientRateLimiter(config.getClientRatePerSession());
        getElement().getStyle().set("display", "none");
        addDetachListener(event -> {
            UI ui = event.getUI();
            if (ui != null && !ui.isClosing()) {
                ui.access(() -> ui.add(this));
            }
        });
    }

    @Override
    protected void onAttach(AttachEvent event) {
        ensureClientLoaded(event.getUI());
    }

    @ClientCallable
    public void recordSamples(List<ClientSample> samples) {
        if (binder == null || samples == null || samples.isEmpty()) {
            return;
        }
        int granted = limiter.tryAcquire(samples.size());
        if (granted < samples.size()) {
            binder.recordThrottled(samples.size() - granted);
            if (granted == 0) {
                return;
            }
            binder.ingest(samples.subList(0, granted));
        } else {
            binder.ingest(samples);
        }
    }

    private static void ensureClientLoaded(UI ui) {
        if (ComponentUtil.getData(ui, CLIENT_INIT_KEY) != null) {
            return;
        }
        ComponentUtil.setData(ui, CLIENT_INIT_KEY, Boolean.TRUE);
        ClassLoader loader = MetricsCollectorElement.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(CLIENT_RESOURCE)) {
            if (in == null) {
                LoggerFactory.getLogger(MetricsCollectorElement.class).warn(
                        "vaadin-micrometer client resource not found: {}",
                        CLIENT_RESOURCE);
                return;
            }
            String js = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            ui.getPage().executeJs(js);
        } catch (IOException e) {
            LoggerFactory.getLogger(MetricsCollectorElement.class)
                    .warn("Could not load vaadin-micrometer client code", e);
        }
    }
}
