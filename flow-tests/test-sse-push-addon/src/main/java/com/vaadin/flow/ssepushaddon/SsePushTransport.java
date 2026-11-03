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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.server.communication.PushTransport;

/**
 * A push transport that streams messages from the server with server-sent
 * events, without Atmosphere. Messages to the server are regular requests.
 * <p>
 * Registered by {@link SsePushServiceInitListener}, which also adds the
 * {@link SseRequestHandler} that serves the event streams.
 */
public class SsePushTransport implements PushTransport {

    /**
     * The name of the transport, to select it with
     * {@code @Push(transportName = SsePushTransport.NAME)}.
     */
    public static final String NAME = "sse-addon";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public PushConnection createPushConnection(UI ui) {
        return new SsePushConnection(ui);
    }
}
