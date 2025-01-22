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

package com.vaadin.flow.spring.flowsecuritywebsocket;

import org.springframework.stereotype.Component;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.ListenerPriority;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.shared.ui.Transport;

@Component
public class PushWebsocketConfigurer implements VaadinServiceInitListener {

    private final PushTransportSetter pushTransportSetter = new PushTransportSetter();

    @Override
    public void serviceInit(ServiceInitEvent event) {

        event.getSource().addUIInitListener(uiInitEvent -> {
            // Transport cannot be set directly in UI listener because
            // BootstrapHandler overrides it with @Push annotation value.
            uiInitEvent.getUI().addBeforeEnterListener(pushTransportSetter);
        });
    }

    @ListenerPriority(10)
    private static class PushTransportSetter implements BeforeEnterListener {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.getUI().getPushConfiguration()
                    .setTransport(Transport.WEBSOCKET);
        }
    }
}
