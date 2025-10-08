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
package com.vaadin.flow.contexttest.ui;

import java.util.List;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

public class PushUtil {
    private PushUtil() {
    }

    public static void setupPush() {
        // server-side bootstrap
        String transportName = VaadinRequest.getCurrent()
                .getParameter("transport");

        // client-side bootstrap
        if (transportName == null) {
            List<String> list = UI.getCurrent().getInternals()
                    .getLastHandledLocation().getQueryParameters()
                    .getParameters().get("transport");
            if (list != null && !list.isEmpty()) {
                transportName = list.get(0);
            }
        }

        Transport transport = Transport.getByIdentifier(transportName);
        if (transport != null) {
            PushConfiguration pushConfiguration = UI.getCurrent()
                    .getPushConfiguration();
            pushConfiguration.setPushMode(PushMode.MANUAL);
            pushConfiguration.setTransport(transport);
            Transport fallbackTransport = transport == Transport.WEBSOCKET_XHR
                    ? Transport.WEBSOCKET
                    : transport;
            pushConfiguration.setFallbackTransport(fallbackTransport);
        }
    }
}
