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
package com.vaadin.base.devserver.devloop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An application the daemon did not launch must be left entirely alone: the
 * ownership model says the daemon aggregates state, it never competes for it.
 */
class DevLoopRegistrationTest {

    @AfterEach
    void clearHandshakeProperties() {
        System.clearProperty(DevLoopRegistration.DAEMON_PORT_PROPERTY);
        System.clearProperty(DevLoopRegistration.TOKEN_PROPERTY);
    }

    @Test
    void noHandshakeProperties_isNotDaemonLaunched() {
        assertFalse(DevLoopRegistration.isDaemonLaunched());
    }

    @Test
    void portWithoutToken_isNotDaemonLaunched() {
        // Half a handshake is not a handshake: connecting without the token
        // would only ever get "unauthorized" back.
        System.setProperty(DevLoopRegistration.DAEMON_PORT_PROPERTY, "51234");

        assertFalse(DevLoopRegistration.isDaemonLaunched());
    }

    @Test
    void bothProperties_isDaemonLaunched() {
        System.setProperty(DevLoopRegistration.DAEMON_PORT_PROPERTY, "51234");
        System.setProperty(DevLoopRegistration.TOKEN_PROPERTY, "s3cr3t");

        assertTrue(DevLoopRegistration.isDaemonLaunched());
    }

    @Test
    void hotswapper_withoutADaemon_neverBecomesActive() {
        // A hotswap agent creates Flow's hotswapper in applications the daemon
        // never launched, and every VaadinHotswapper on the classpath joins
        // that
        // chain. This one has to stay out of the way: no active instance, so
        // every callback is a no-op and nothing walks a component tree on its
        // behalf.
        DevLoopHotswapper hotswapper = new DevLoopHotswapper();

        hotswapper.onInit(Mockito.mock(VaadinService.class));

        assertNull(DevLoopHotswapper.getActive());
        assertFalse(hotswapper.isCompleted());
    }

    @Test
    void hotswapper_daemonLaunched_becomesActive() {
        System.setProperty(DevLoopRegistration.DAEMON_PORT_PROPERTY, "51234");
        System.setProperty(DevLoopRegistration.TOKEN_PROPERTY, "s3cr3t");
        DevLoopHotswapper hotswapper = new DevLoopHotswapper();

        hotswapper.onInit(Mockito.mock(VaadinService.class));

        assertSame(hotswapper, DevLoopHotswapper.getActive());
    }

    @Test
    void modeOf_distinguishesADevBundleFromAViteApplication() {
        // The regression this guards: isDevModeLiveReloadEnabled() is
        // isDevToolsEnabled() && devmode.liveReload, both true by default, so
        // it reported DEVELOPMENT_FRONTEND_LIVERELOAD for a dev-bundle
        // application too. The daemon decides whether a frontend edit was
        // already applied by Vite or needs the bundle rebuilt from this, and
        // those are opposite answers.
        assertEquals("DEVELOPMENT_BUNDLE", DevLoopRegistration
                .modeOf(serviceInMode(Mode.DEVELOPMENT_BUNDLE)));
        assertEquals("DEVELOPMENT_FRONTEND_LIVERELOAD", DevLoopRegistration
                .modeOf(serviceInMode(Mode.DEVELOPMENT_FRONTEND_LIVERELOAD)));
    }

    private static VaadinService serviceInMode(Mode mode) {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.getMode()).thenReturn(mode);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        return service;
    }

    @Test
    void initListener_withoutADaemon_touchesNothing() {
        VaadinService service = Mockito.mock(VaadinService.class);
        ServiceInitEvent event = Mockito.mock(ServiceInitEvent.class);
        Mockito.when(event.getSource()).thenReturn(service);

        new DevLoopInitListener().serviceInit(event);

        // Not even the deployment configuration is asked for: an application
        // running from an IDE must see no trace of the dev loop.
        Mockito.verify(service, Mockito.never()).getDeploymentConfiguration();
    }

    @Test
    void initListener_productionMode_doesNotRegister() {
        System.setProperty(DevLoopRegistration.DAEMON_PORT_PROPERTY, "51234");
        System.setProperty(DevLoopRegistration.TOKEN_PROPERTY, "s3cr3t");
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        ServiceInitEvent event = Mockito.mock(ServiceInitEvent.class);
        Mockito.when(event.getSource()).thenReturn(service);

        new DevLoopInitListener().serviceInit(event);

        // The launch and the configuration disagree; the dev loop stays out
        // rather than opening a socket it could never redefine anything over.
        Mockito.verify(service, Mockito.never()).getContext();
    }
}
