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
package com.vaadin.flow.server;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.server.communication.PushTransport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class VaadinServicePushTransportTest {

    @Test
    void collectPushTransports_keepsOrderOfAdding() {
        ServiceInitEvent event = new ServiceInitEvent(
                mock(VaadinService.class));
        event.addPushTransport(new NamedTransport("second"));
        event.addPushTransport(new NamedTransport("first"));

        assertEquals(List.of("second", "first"), List
                .copyOf(VaadinService.collectPushTransports(event).keySet()));
    }

    @Test
    void collectPushTransports_nameOfBuiltInTransport_throws() {
        ServiceInitEvent event = new ServiceInitEvent(
                mock(VaadinService.class));
        event.addPushTransport(new NamedTransport("websocket-xhr"));

        assertThrows(IllegalStateException.class,
                () -> VaadinService.collectPushTransports(event));
    }

    @Test
    void collectPushTransports_sameNameTwice_throws() {
        ServiceInitEvent event = new ServiceInitEvent(
                mock(VaadinService.class));
        event.addPushTransport(new NamedTransport("add-on"));
        event.addPushTransport(new NamedTransport("add-on"));

        assertThrows(IllegalStateException.class,
                () -> VaadinService.collectPushTransports(event));
    }

    private record NamedTransport(String name) implements PushTransport {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public PushConnection createPushConnection(UI ui) {
            return mock(PushConnection.class);
        }

        @Override
        public String getClientScript() {
            return "VAADIN/static/add-on/push.js";
        }
    }
}
