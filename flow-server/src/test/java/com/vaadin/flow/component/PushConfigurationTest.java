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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.experimental.CoreFeatureFlagProvider;
import com.vaadin.experimental.DisabledFeatureException;
import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.AtmospherePushConnection;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.server.communication.SsePushConnection;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PushConfigurationTest {

    private VaadinService service;
    private VaadinSession session;
    private UI ui;

    @BeforeEach
    void setup() {
        service = new MockVaadinServletService();
        session = new MockVaadinSession(service);
        session.lock();
        ui = new MockUI(session);
    }

    @AfterEach
    void tearDown() {
        session.unlock();
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
    }

    @Test
    void setTransport_serverSentEventsWithoutFeatureFlag_throws() {
        DisabledFeatureException exception = assertThrows(
                DisabledFeatureException.class, () -> ui.getPushConfiguration()
                        .setTransport(Transport.SERVER_SENT_EVENTS));
        assertTrue(
                exception.getMessage().contains(
                        "com.vaadin.experimental.ssePushTransport=true"),
                "The exception should tell how to enable the feature, but was: "
                        + exception.getMessage());
        assertEquals(Transport.WEBSOCKET_XHR,
                ui.getPushConfiguration().getTransport());
    }

    @Test
    void setFallbackTransport_serverSentEventsWithoutFeatureFlag_throws() {
        assertThrows(DisabledFeatureException.class,
                () -> ui.getPushConfiguration()
                        .setFallbackTransport(Transport.SERVER_SENT_EVENTS));
        assertEquals(Transport.LONG_POLLING,
                ui.getPushConfiguration().getFallbackTransport());
    }

    @Test
    void setTransport_serverSentEventsWithFeatureFlag_transportIsUsed() {
        enableServerSentEvents();

        ui.getPushConfiguration().setTransport(Transport.SERVER_SENT_EVENTS);
        ui.getPushConfiguration()
                .setFallbackTransport(Transport.SERVER_SENT_EVENTS);

        assertEquals(Transport.SERVER_SENT_EVENTS,
                ui.getPushConfiguration().getTransport());
        assertEquals(Transport.SERVER_SENT_EVENTS,
                ui.getPushConfiguration().getFallbackTransport());
    }

    @Test
    void setTransport_serverSentEventsWithoutService_throws() {
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
        VaadinService.setCurrent(null);
        UI detachedUi = new UI();

        assertThrows(DisabledFeatureException.class,
                () -> detachedUi.getPushConfiguration()
                        .setTransport(Transport.SERVER_SENT_EVENTS));
    }

    @Test
    void setTransport_otherTransportsWithoutFeatureFlag_transportIsUsed() {
        ui.getPushConfiguration().setTransport(Transport.WEBSOCKET);
        ui.getPushConfiguration().setFallbackTransport(Transport.LONG_POLLING);

        assertEquals(Transport.WEBSOCKET,
                ui.getPushConfiguration().getTransport());
        assertEquals(Transport.LONG_POLLING,
                ui.getPushConfiguration().getFallbackTransport());
    }

    @Test
    void setPushMode_ssePure_usesSsePushConnection() {
        ui.getPushConfiguration().setTransport(Transport.SSE_PURE);
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        assertInstanceOf(SsePushConnection.class,
                ui.getInternals().getPushConnection());
    }

    @Test
    void setPushMode_otherTransport_usesAtmospherePushConnection() {
        ui.getPushConfiguration().setTransport(Transport.WEBSOCKET);
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        assertInstanceOf(AtmospherePushConnection.class,
                ui.getInternals().getPushConnection());
    }

    @Test
    void setPushMode_ssePureWithoutAtmosphere_pushIsEnabled() {
        UI uiWithoutAtmosphere = createUiWithoutAtmosphere();

        uiWithoutAtmosphere.getPushConfiguration()
                .setTransport(Transport.SSE_PURE);
        uiWithoutAtmosphere.getPushConfiguration()
                .setPushMode(PushMode.AUTOMATIC);

        assertEquals(PushMode.AUTOMATIC,
                uiWithoutAtmosphere.getPushConfiguration().getPushMode());
        assertInstanceOf(SsePushConnection.class,
                uiWithoutAtmosphere.getInternals().getPushConnection());
    }

    @Test
    void setPushMode_otherTransportWithoutAtmosphere_throws() {
        UI uiWithoutAtmosphere = createUiWithoutAtmosphere();

        assertThrows(IllegalStateException.class, () -> uiWithoutAtmosphere
                .getPushConfiguration().setPushMode(PushMode.AUTOMATIC));
    }

    @Test
    void setTransport_ssePureWhenPushEnabled_replacesConnection() {
        // Push may be enabled, e.g. by the deployment configuration, before an
        // @Push annotation on the app shell sets the transport
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        PushConnection atmosphereConnection = ui.getInternals()
                .getPushConnection();
        assertInstanceOf(AtmospherePushConnection.class, atmosphereConnection);

        ui.getPushConfiguration().setTransport(Transport.SSE_PURE);

        assertInstanceOf(SsePushConnection.class,
                ui.getInternals().getPushConnection());
    }

    @Test
    void setTransport_fromSsePureWhenPushEnabled_replacesConnection() {
        ui.getPushConfiguration().setTransport(Transport.SSE_PURE);
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        ui.getPushConfiguration().setTransport(Transport.WEBSOCKET);

        assertInstanceOf(AtmospherePushConnection.class,
                ui.getInternals().getPushConnection());
    }

    @Test
    void setTransport_ssePureWhenPushDisabled_noConnection() {
        ui.getPushConfiguration().setTransport(Transport.SSE_PURE);

        assertNull(ui.getInternals().getPushConnection());
    }

    @Test
    void setTransport_sameKindWhenPushEnabled_keepsConnection() {
        ui.getPushConfiguration().setTransport(Transport.SSE_PURE);
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        PushConnection connection = ui.getInternals().getPushConnection();

        ui.getPushConfiguration().setTransport(Transport.SSE_PURE);

        assertEquals(connection, ui.getInternals().getPushConnection());
    }

    private UI createUiWithoutAtmosphere() {
        MockVaadinServletService serviceWithoutAtmosphere = new MockVaadinServletService() {
            @Override
            public boolean ensurePushAvailable() {
                return false;
            }
        };
        VaadinSession sessionWithoutAtmosphere = new MockVaadinSession(
                serviceWithoutAtmosphere);
        // The test thread already holds the lock of the default session, the
        // lock of this session is released when the test ends
        sessionWithoutAtmosphere.lock();
        return new MockUI(sessionWithoutAtmosphere);
    }

    private void enableServerSentEvents() {
        FeatureFlags featureFlags = FeatureFlags.get(service.getContext());
        for (Feature feature : featureFlags.getFeatures()) {
            if (feature.equals(CoreFeatureFlagProvider.SSE_PUSH_TRANSPORT)) {
                feature.setEnabled(true);
            }
        }
    }
}
