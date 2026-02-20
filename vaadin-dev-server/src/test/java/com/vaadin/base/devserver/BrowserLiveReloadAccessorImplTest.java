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
package com.vaadin.base.devserver;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BrowserLiveReloadAccessorImplTest {

    private BrowserLiveReloadAccessorImpl access = new BrowserLiveReloadAccessorImpl();

    @Test
    void getLiveReload_productionMode_nullIsReturned() {
        VaadinService service = mockConfiguration(true, false);

        assertNull(access.getLiveReload(service));
    }

    @Test
    void getLiveReload_liveReloadDisabled_instanceIsCreated() {
        VaadinService service = mockConfiguration(false, false);
        VaadinContext context = service.getContext();
        Mockito.when(context.getAttribute(Mockito.eq(BrowserLiveReload.class),
                Mockito.any()))
                .thenReturn(Mockito.mock(BrowserLiveReload.class));

        assertNotNull(access.getLiveReload(service));
    }

    @Test
    void getLiveReload_devMode_contextHasNoReloadInstance_instanceIsCreated() {
        VaadinService service = mockConfiguration(false, true);
        VaadinContext context = service.getContext();
        Mockito.when(context.getAttribute(Mockito.eq(BrowserLiveReload.class),
                Mockito.any()))
                .thenReturn(Mockito.mock(BrowserLiveReload.class));

        assertNotNull(access.getLiveReload(service));
    }

    @Test
    void getLiveReload_devMode_contextHasReloadInstance_instanceIsReturned() {
        VaadinService service = mockConfiguration(false, true);
        BrowserLiveReload reload = Mockito.mock(BrowserLiveReload.class);
        Mockito.when(service.getContext().getAttribute(
                Mockito.eq(BrowserLiveReload.class), Mockito.any()))
                .thenReturn(reload);

        assertSame(reload, access.getLiveReload(service));
    }

    private VaadinService mockConfiguration(boolean productionMode,
            boolean liveReloadEnabled) {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(config);
        Mockito.when(config.isProductionMode()).thenReturn(productionMode);
        Mockito.when(config.isDevModeLiveReloadEnabled())
                .thenReturn(liveReloadEnabled);
        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(applicationConfiguration.isProductionMode())
                .thenReturn(productionMode);
        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(context);
        Mockito.when(context.getAttribute(
                Mockito.eq(ApplicationConfiguration.class), Mockito.any()))
                .thenReturn(applicationConfiguration);
        return service;
    }
}
