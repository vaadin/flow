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
package com.vaadin.flow.quarkus.it;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.vaadin.sample.websockets.DependencyAnnotatedWS;
import org.vaadin.sample.websockets.SimpleEndpoint;

@QuarkusIntegrationTest
class CustomWebsocketsIT {

    @TestHTTPResource(DependencyAnnotatedWS.URI)
    URI dependencyAnnotatedWSURI;

    @TestHTTPResource(SimpleEndpoint.URI)
    URI dependencyNotAnnotatedWSURI;

    @TestHTTPResource(CustomAnnotatedEnpoint.URI)
    URI appAnnotatedWSURI;

    @Test
    void dependencyAnnotatedEndpointShouldWork() throws Exception {
        assertWebsocketWorks(dependencyAnnotatedWSURI,
                DependencyAnnotatedWS.PREFIX);
    }

    @Test
    void applicationAnnotatedEndpointShouldWork() throws Exception {
        assertWebsocketWorks(appAnnotatedWSURI, CustomAnnotatedEnpoint.PREFIX);
    }

    @Test
    void dependencyNotAnnotatedEndpointShouldWork() throws Exception {
        assertWebsocketWorks(dependencyNotAnnotatedWSURI,
                SimpleEndpoint.PREFIX);
    }

    void assertWebsocketWorks(URI uri, String messagePrefix) throws Exception {
        Client client = new Client();
        try (Session session = ContainerProvider.getWebSocketContainer()
                .connectToServer(client, uri)) {
            Assertions.assertEquals("CONNECT", client.receivedMessage());
            Assertions.assertEquals(messagePrefix + "Welcome",
                    client.receivedMessage());
            session.getBasicRemote().sendText("hello world");
            Assertions.assertEquals(messagePrefix + "hello world",
                    client.receivedMessage());
        }
    }

    @ClientEndpoint
    public static class Client {
        final LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>();

        @OnOpen
        public void open(Session session) throws IOException {
            messages.add("CONNECT");
        }

        @OnMessage
        void message(String msg) {
            messages.add(msg);
        }

        String receivedMessage() throws InterruptedException {
            return messages.poll(12, TimeUnit.SECONDS);
        }

    }
}
