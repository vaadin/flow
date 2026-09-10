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
package com.vaadin.base.devserver.hotswap;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flow now registers the hotswapper itself, so it has to be safe to register
 * twice: hotswap tools still inject a call of their own, and two instances
 * would double every refresh.
 */
class HotswapperRegistrationTest {

    private MockVaadinContext context;
    private VaadinService service;
    private DeploymentConfiguration configuration;

    @BeforeEach
    void setup() {
        context = new MockVaadinContext();
        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(applicationConfiguration.isProductionMode())
                .thenReturn(false);
        context.setAttribute(ApplicationConfiguration.class,
                applicationConfiguration);
        service = mockService();
        configuration = service.getDeploymentConfiguration();
    }

    private VaadinService mockService() {
        VaadinService mock = Mockito.mock(VaadinService.class);
        DeploymentConfiguration deployment = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(deployment.isProductionMode()).thenReturn(false);
        Mockito.when(mock.getDeploymentConfiguration()).thenReturn(deployment);
        Mockito.when(mock.getContext()).thenReturn(context);
        return mock;
    }

    @Test
    void register_twice_returnsTheSameHotswapper() {
        Optional<Hotswapper> first = Hotswapper.register(service);
        Optional<Hotswapper> second = Hotswapper.register(service);

        assertTrue(first.isPresent());
        assertSame(first.get(), second.get());
        // The listeners must be added once, or every class change refreshes
        // twice.
        Mockito.verify(service, Mockito.times(1))
                .addUIInitListener(Mockito.any());
        Mockito.verify(service, Mockito.times(1))
                .addSessionInitListener(Mockito.any());
        Mockito.verify(service, Mockito.times(1))
                .addSessionDestroyListener(Mockito.any());
        Mockito.verify(service, Mockito.times(1))
                .addServiceDestroyListener(Mockito.any());
    }

    @Test
    void getRegistered_afterRegister_findsIt() {
        Hotswapper registered = Hotswapper.register(service).orElseThrow();

        assertSame(registered, Hotswapper.getRegistered(service).orElseThrow());
    }

    @Test
    void getRegistered_withoutRegister_isEmpty() {
        assertTrue(Hotswapper.getRegistered(service).isEmpty());
    }

    @Test
    void getRegistered_anotherServiceInTheSameContext_isEmpty() {
        Hotswapper.register(service);

        // Handing one service's hotswapper to another would refresh the wrong
        // UIs.
        assertTrue(Hotswapper.getRegistered(mockService()).isEmpty());
    }

    @Test
    void register_secondServiceInTheSameContext_keepsTheFirstsRegistration() {
        // Two VaadinServlets in one web app are two services sharing one
        // ServletContext, which is where the registration lives.
        Hotswapper first = Hotswapper.register(service).orElseThrow();
        VaadinService other = mockService();

        Hotswapper second = Hotswapper.register(other).orElseThrow();

        assertNotSame(first, second, "each service needs its own hotswapper");
        // Evicting the first is what made the dev loop's connector answer
        // ERR kind=no-hotswapper for whichever service registered earlier.
        assertSame(first, Hotswapper.getRegistered(service).orElseThrow());
        assertSame(second, Hotswapper.getRegistered(other).orElseThrow());
        // And with the first still findable, registering it again reuses it
        // rather than building a second one that doubles every refresh.
        assertSame(first, Hotswapper.register(service).orElseThrow());
        Mockito.verify(service, Mockito.times(1))
                .addUIInitListener(Mockito.any());
    }

    @Test
    void serviceDestroy_forgetsTheRegistration() {
        Hotswapper hotswapper = Hotswapper.register(service).orElseThrow();

        hotswapper.serviceDestroy(new ServiceDestroyEvent(service));

        // The registration outlives the service otherwise: it is held in a
        // context attribute, and the context outlives every service in it.
        assertTrue(Hotswapper.getRegistered(service).isEmpty());
    }

    @Test
    void register_productionMode_isSkipped() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        assertTrue(Hotswapper.register(service).isEmpty());
    }

}
