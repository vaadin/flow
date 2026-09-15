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
package com.vaadin.flow.quarkus.test;

import io.quarkus.test.QuarkusUnitTest;
import io.undertow.websockets.ServerWebSocketContainer;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Verifies that the Vaadin Quarkus extension defaults
 * {@code quarkus.websocket.dispatch-to-worker} to {@code true}, routing inbound
 * Vaadin Push websocket frames through the worker thread pool instead of the
 * Vert.x event loop.
 * <p>
 * Without this default, Push frame handling runs inline on the Vert.x event
 * loop. If application code blocks while holding the Vaadin session lock (for
 * example a synchronous REST call in {@code BeforeEnterObserver} or
 * {@code AfterNavigationListener}), the event loop deadlocks against the
 * session lock and the application hangs.
 */
public class PushDispatchTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void dispatchToWorker_defaultsToTrue() {
        WebSocketContainer container = ContainerProvider
                .getWebSocketContainer();
        Assertions.assertInstanceOf(ServerWebSocketContainer.class, container,
                "Expected Quarkus to expose its ServerWebSocketContainer as the "
                        + "default JSR-356 container.");
        Assertions.assertTrue(
                ((ServerWebSocketContainer) container).isDispatchToWorker(),
                "Vaadin Quarkus must default quarkus.websocket.dispatch-to-worker=true "
                        + "so Push frame handling runs on the worker pool, not the "
                        + "Vert.x event loop.");
    }
}
