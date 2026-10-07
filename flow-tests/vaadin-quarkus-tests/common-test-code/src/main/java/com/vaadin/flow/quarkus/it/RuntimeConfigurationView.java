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
package com.vaadin.flow.quarkus.it;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;

/**
 * Shows the heartbeat interval of the deployment configuration, which
 * {@code RuntimeConfigurationIT} sets with a system property when the
 * application starts.
 */
@Route("runtime-configuration")
public class RuntimeConfigurationView extends Div {

    public static final String HEARTBEAT_INTERVAL_ID = "runtime-heartbeat-interval";

    public RuntimeConfigurationView() {
        Span heartbeatInterval = new Span(
                String.valueOf(VaadinService.getCurrent()
                        .getDeploymentConfiguration().getHeartbeatInterval()));
        heartbeatInterval.setId(HEARTBEAT_INTERVAL_ID);
        add(new Div(new Span("Heartbeat interval (vaadin.heartbeatInterval): "),
                heartbeatInterval));
    }
}
