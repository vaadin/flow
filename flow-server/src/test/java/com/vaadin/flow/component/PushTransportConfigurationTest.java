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
package com.vaadin.flow.component;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.internal.nodefeature.PushConfigurationMap;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.AtmospherePushConnection;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.server.communication.PushTransport;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Selecting push transports added by add-ons.
 */
class PushTransportConfigurationTest {

    private static final String SCRIPT = "VAADIN/static/test/test-push.js";

    private VaadinSession session;

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.unlock();
        }
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
    }

    @Test
    void getTransportName_builtInTransport_isItsIdentifier() {
        UI ui = createUi(true);

        assertEquals("websocket-xhr",
                ui.getPushConfiguration().getTransportName());
    }

    @Test
    void setTransportName_builtInTransport_setsTransport() {
        UI ui = createUi(true, new TestTransport("test-push"));

        ui.getPushConfiguration().setTransportName("long-polling");

        assertEquals(Transport.LONG_POLLING,
                ui.getPushConfiguration().getTransport());
        assertNull(getMap(ui).getTransportScript());
    }

    @Test
    void setTransportName_addOnTransport_usesItsConnectionAndScript() {
        UI ui = createUi(true, new TestTransport("test-push"));

        ui.getPushConfiguration().setTransportName("test-push");
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        assertEquals("test-push", ui.getPushConfiguration().getTransportName());
        assertNull(ui.getPushConfiguration().getTransport());
        assertEquals(SCRIPT, getMap(ui).getTransportScript());
        assertInstanceOf(TestConnection.class,
                ui.getInternals().getPushConnection());
    }

    @Test
    void setTransportName_unknownTransport_throws() {
        UI ui = createUi(true, new TestTransport("test-push"));

        assertThrows(IllegalArgumentException.class,
                () -> ui.getPushConfiguration().setTransportName("unknown"));
        assertEquals(Transport.WEBSOCKET_XHR,
                ui.getPushConfiguration().getTransport());
    }

    @Test
    void setTransportName_whenPushEnabled_replacesConnection() {
        UI ui = createUi(true, new TestTransport("test-push"));
        ui.getPushConfiguration().setTransport(Transport.WEBSOCKET);
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        assertInstanceOf(AtmospherePushConnection.class,
                ui.getInternals().getPushConnection());

        ui.getPushConfiguration().setTransportName("test-push");

        assertInstanceOf(TestConnection.class,
                ui.getInternals().getPushConnection());
    }

    @Test
    void setTransport_fromAddOnTransportWhenPushEnabled_replacesConnection() {
        UI ui = createUi(true, new TestTransport("test-push"));
        ui.getPushConfiguration().setTransportName("test-push");
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        ui.getPushConfiguration().setTransport(Transport.WEBSOCKET);

        assertEquals(Transport.WEBSOCKET,
                ui.getPushConfiguration().getTransport());
        assertNull(getMap(ui).getTransportScript());
        assertInstanceOf(AtmospherePushConnection.class,
                ui.getInternals().getPushConnection());
    }

    @Test
    void setPushMode_withoutAtmosphere_usesFirstAddOnTransport() {
        UI ui = createUi(false, new TestTransport("first"),
                new TestTransport("second"));

        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        assertEquals("first", ui.getPushConfiguration().getTransportName());
        assertInstanceOf(TestConnection.class,
                ui.getInternals().getPushConnection());
    }

    @Test
    void setPushMode_defaultTransportWithAtmosphere_usesAddOnTransport() {
        // E.g. in development mode, where the dev tools keep Atmosphere on
        // the classpath
        UI ui = createUi(true, new TestTransport("test-push"));

        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        assertEquals("test-push", ui.getPushConfiguration().getTransportName());
        assertInstanceOf(TestConnection.class,
                ui.getInternals().getPushConnection());
    }

    @Test
    void setPushMode_chosenBuiltInTransportWithAtmosphere_isKept() {
        UI ui = createUi(true, new TestTransport("test-push"));
        ui.getPushConfiguration().setTransportName("websocket-xhr");

        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        assertEquals(Transport.WEBSOCKET_XHR,
                ui.getPushConfiguration().getTransport());
        assertInstanceOf(AtmospherePushConnection.class,
                ui.getInternals().getPushConnection());
    }

    @Test
    void setPushMode_noAddOnTransports_keepsDefaultTransport() {
        UI ui = createUi(true);

        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        assertEquals(Transport.WEBSOCKET_XHR,
                ui.getPushConfiguration().getTransport());
        assertInstanceOf(AtmospherePushConnection.class,
                ui.getInternals().getPushConnection());
    }

    @Test
    void setPushMode_withoutAtmosphereOrAddOnTransports_throws() {
        UI ui = createUi(false);

        // As before add-on transports: push is not available
        assertThrows(IllegalStateException.class, () -> ui
                .getPushConfiguration().setPushMode(PushMode.AUTOMATIC));
    }

    @Test
    void setTransport_builtInTransportWithoutAtmosphere_keepsAddOnTransport() {
        // An @Push annotation names a built-in transport, which cannot work
        // without Atmosphere
        UI ui = createUi(false, new TestTransport("test-push"));
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        ui.getPushConfiguration().setTransport(Transport.WEBSOCKET_XHR);

        assertEquals("test-push", ui.getPushConfiguration().getTransportName());
        assertInstanceOf(TestConnection.class,
                ui.getInternals().getPushConnection());
    }

    private UI createUi(boolean atmosphereAvailable,
            PushTransport... transports) {
        List<PushTransport> added = List.of(transports);
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public boolean ensurePushAvailable() {
                return atmosphereAvailable;
            }

            @Override
            public Optional<PushTransport> getPushTransport(String name) {
                return added.stream()
                        .filter(transport -> transport.getName().equals(name))
                        .findFirst();
            }

            @Override
            public Optional<PushTransport> getDefaultPushTransport() {
                return added.stream().findFirst();
            }

            @Override
            public Optional<PushTransport> getFallbackPushTransport() {
                return atmosphereAvailable ? Optional.empty()
                        : added.stream().findFirst();
            }
        };
        session = new MockVaadinSession(service);
        session.lock();
        return new MockUI(session);
    }

    private static PushConfigurationMap getMap(UI ui) {
        return ui.getInternals().getStateTree().getRootNode()
                .getFeature(PushConfigurationMap.class);
    }

    private record TestTransport(String name) implements PushTransport {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public PushConnection createPushConnection(UI ui) {
            return mock(TestConnection.class);
        }

        @Override
        public String getClientScript() {
            return SCRIPT;
        }
    }

    private interface TestConnection extends PushConnection {
    }
}
