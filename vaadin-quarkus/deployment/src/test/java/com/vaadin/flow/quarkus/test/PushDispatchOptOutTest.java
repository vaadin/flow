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
 * Verifies that an explicit user override of
 * {@code quarkus.websocket.dispatch-to-worker} takes precedence over the
 * default that {@link PushDispatchTest} pins.
 */
public class PushDispatchOptOutTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.websocket.dispatch-to-worker", "false");

    @Test
    public void userOverride_takesPrecedence() {
        WebSocketContainer container = ContainerProvider
                .getWebSocketContainer();
        Assertions.assertInstanceOf(ServerWebSocketContainer.class, container);
        Assertions.assertFalse(
                ((ServerWebSocketContainer) container).isDispatchToWorker(),
                "Explicit quarkus.websocket.dispatch-to-worker=false must "
                        + "override the extension's default.");
    }
}
