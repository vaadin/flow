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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        when(context.getAttribute(eq(BrowserLiveReload.class), any()))
                .thenReturn(mock(BrowserLiveReload.class));

        assertNotNull(access.getLiveReload(service));
    }

    @Test
    void getLiveReload_devMode_contextHasNoReloadInstance_instanceIsCreated() {
        VaadinService service = mockConfiguration(false, true);
        VaadinContext context = service.getContext();
        when(context.getAttribute(eq(BrowserLiveReload.class), any()))
                .thenReturn(mock(BrowserLiveReload.class));

        assertNotNull(access.getLiveReload(service));
    }

    @Test
    void getLiveReload_devMode_contextHasReloadInstance_instanceIsReturned() {
        VaadinService service = mockConfiguration(false, true);
        BrowserLiveReload reload = mock(BrowserLiveReload.class);
        when(service.getContext().getAttribute(eq(BrowserLiveReload.class),
                any())).thenReturn(reload);

        assertSame(reload, access.getLiveReload(service));
    }

    private VaadinService mockConfiguration(boolean productionMode,
            boolean liveReloadEnabled) {
        VaadinService service = mock(VaadinService.class);
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        when(service.getDeploymentConfiguration()).thenReturn(config);
        when(config.isProductionMode()).thenReturn(productionMode);
        when(config.isDevModeLiveReloadEnabled()).thenReturn(liveReloadEnabled);
        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        when(applicationConfiguration.isProductionMode())
                .thenReturn(productionMode);
        VaadinContext context = mock(VaadinContext.class);
        when(service.getContext()).thenReturn(context);
        when(context.getAttribute(eq(ApplicationConfiguration.class), any()))
                .thenReturn(applicationConfiguration);
        return service;
    }
}
