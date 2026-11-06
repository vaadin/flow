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
import org.mockito.Mockito;

import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.server.communication.PushConnectionFactory;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PushConnectionFactoryTest {

    private VaadinSession session;
    private UI ui;

    @BeforeEach
    void setup() {
        // A service without Atmosphere
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public boolean ensurePushAvailable() {
                return false;
            }
        };
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
    void setPushMode_withoutAtmosphere_throws() {
        assertThrows(IllegalStateException.class, () -> ui
                .getPushConfiguration().setPushMode(PushMode.AUTOMATIC));
    }

    @Test
    void setPushMode_factoryRequiringAtmosphereWithoutAtmosphere_throws() {
        ui.getPushConfiguration().setPushConnectionFactory(
                u -> Mockito.mock(PushConnection.class));

        assertThrows(IllegalStateException.class, () -> ui
                .getPushConfiguration().setPushMode(PushMode.AUTOMATIC));
    }

    @Test
    void setPushMode_factoryWithoutAtmosphere_connectionOfFactoryIsUsed() {
        PushConnection connection = Mockito.mock(PushConnection.class);
        ui.getPushConfiguration()
                .setPushConnectionFactory(new PushConnectionFactory() {
                    @Override
                    public PushConnection apply(UI u) {
                        return connection;
                    }

                    @Override
                    public boolean isAtmosphereRequired() {
                        return false;
                    }
                });

        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        assertSame(connection, ui.getInternals().getPushConnection());
    }
}
