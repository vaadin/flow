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
package com.vaadin.flow.spring.test;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SpringBootOnly.class)
public class CustomWebSocketIT extends AbstractSpringTest {

    @ClientEndpoint
    public class CustomWebSocketEndpoint {

        private final CountDownLatch closureLatch = new CountDownLatch(1);

        private String message;

        @OnMessage
        public void onMessage(Session session, String message)
                throws IOException {
            this.message = message;
            session.close();
        }

        @OnClose
        public void onClose(CloseReason reason) {
            closureLatch.countDown();
        }

        public String waitForMessage() throws InterruptedException {
            closureLatch.await(1, TimeUnit.SECONDS);
            return message;
        }
    }

    @Test
    public void properWebsocketResponseIsReceived() throws Exception {
        WebSocketContainer container = ContainerProvider
                .getWebSocketContainer();

        String testUrl = getTestURL().replace("http://", "ws://");
        URI uri = URI.create(testUrl + CustomWebSocket.WEBSOCKET_URL);

        try {
            CustomWebSocketEndpoint clientEndpoint = new CustomWebSocketEndpoint();
            Session session = container.connectToServer(clientEndpoint, uri);
            session.getBasicRemote().sendText("Hello");
            Assert.assertEquals(CustomWebSocket.WEBSOCKET_RESPONSE_TEXT,
                    clientEndpoint.waitForMessage());
            session.close();
        } catch (Exception ex) {
            throw ex;
        }
    }

    @Override
    protected String getTestPath() {
        return "";
    }

}
