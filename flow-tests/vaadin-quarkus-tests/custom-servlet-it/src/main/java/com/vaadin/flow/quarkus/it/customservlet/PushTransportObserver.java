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
package com.vaadin.flow.quarkus.it.customservlet;

import jakarta.enterprise.event.Observes;

import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Forces the Push fallback transport to websocket, so that Push either works
 * over a websocket or does not connect at all. Without this, a failing
 * websocket connection would silently fall back to long polling and the Push
 * tests would still pass.
 */
public class PushTransportObserver {

    void onUIInit(@Observes UIInitEvent uiInitEvent) {
        uiInitEvent.getUI().getPushConfiguration()
                .setFallbackTransport(Transport.WEBSOCKET);
    }
}
