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
package org.vaadin.sample.websockets;

import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;

public class SimpleEndpoint extends Endpoint {

    public static final String URI = "/dependency-websocket";

    public static final String PREFIX = ">> Dependency Simple Endpoint: ";

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        Handler handler = new Handler(session.getAsyncRemote());
        session.addMessageHandler(handler);
        handler.reply("Welcome");
    }

    private static class Handler implements MessageHandler.Whole<String> {

        private final RemoteEndpoint.Async remote;

        public Handler(RemoteEndpoint.Async remote) {
            this.remote = remote;
        }

        @Override
        public void onMessage(String message) {
            reply(message);
        }

        private void reply(String message) {
            remote.sendText(PREFIX + message);
        }
    }
}
