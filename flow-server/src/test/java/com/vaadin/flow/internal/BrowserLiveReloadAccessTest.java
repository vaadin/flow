/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

public class BrowserLiveReloadAccessTest {

    private BrowserLiveReloadAccess access = new BrowserLiveReloadAccess();

    @Test
    public void getLiveReload_productionMode_nullIsReturned() {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(config);
        Mockito.when(config.isProductionMode()).thenReturn(true);

        Assert.assertNull(access.getLiveReload(service));
    }

    @Test
    public void getLiveReload_devMode_contextHasNoReloadInstance_instanceIsCreated() {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(config);
        Mockito.when(config.isProductionMode()).thenReturn(false);

        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(context);

        Assert.assertNotNull(access.getLiveReload(service));
    }

    @Test
    public void getLiveReload_devMode_contextHasReloadInstance_instanceIsReturned() {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(config);
        Mockito.when(config.isProductionMode()).thenReturn(false);

        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(context);

        BrowserLiveReloadImpl reload = Mockito
                .mock(BrowserLiveReloadImpl.class);
        Mockito.when(context.getAttribute(BrowserLiveReloadImpl.class))
                .thenReturn(reload);

        Assert.assertSame(reload, access.getLiveReload(service));
    }

    public static BrowserLiveReload mockBrowserLiveReloadImpl(
            VaadinContext context) {
        BrowserLiveReloadImpl liveReload = Mockito
                .mock(BrowserLiveReloadImpl.class);
        context.setAttribute(BrowserLiveReloadImpl.class, liveReload);
        return liveReload;
    }
}
