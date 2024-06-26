/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
    public void getLiveReload_liveReloadDisabled_nullIsReturned() {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(config);
        Mockito.when(config.isProductionMode()).thenReturn(false);
        Mockito.when(config.isDevModeLiveReloadEnabled()).thenReturn(false);

        Assert.assertNull(access.getLiveReload(service));
    }

    @Test
    public void getLiveReload_devMode_contextHasNoReloadInstance_instanceIsCreated() {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(config);
        Mockito.when(config.isProductionMode()).thenReturn(false);
        Mockito.when(config.isDevModeLiveReloadEnabled()).thenReturn(true);

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
        Mockito.when(config.isDevModeLiveReloadEnabled()).thenReturn(true);

        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(context);

        DebugWindowConnection reload = Mockito
                .mock(DebugWindowConnection.class);
        Mockito.when(context.getAttribute(DebugWindowConnection.class))
                .thenReturn(reload);

        Assert.assertSame(reload, access.getLiveReload(service));
    }
}
