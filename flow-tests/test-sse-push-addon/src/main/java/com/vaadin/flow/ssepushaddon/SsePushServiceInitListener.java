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
package com.vaadin.flow.ssepushaddon;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Adds the SSE push transport and the handler of its event streams to every
 * Vaadin service. Found with the service loader, so the add-on works by having
 * it on the classpath, also with Spring Boot and Quarkus.
 * <p>
 * The {@link JsModule} puts the client side of the transport in the frontend
 * bundle, as the bundle includes the modules of service init listeners.
 */
@JsModule("./sse-push/sse-push.js")
public class SsePushServiceInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addPushTransport(new SsePushTransport());
        event.addRequestHandler(new SseRequestHandler(event.getSource()));
    }
}
