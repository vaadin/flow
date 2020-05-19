/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.communication;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessTest;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.tests.util.MockDeploymentConfiguration;

public class PushHandlerTest {

    @Test
    public void onConnect_websocketTransport_requestStartIsCalledOnServiceInstance() {
        VaadinServletService service = runTest((handler, resource) -> {
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
            handler.onConnect(resource);
        });

        Mockito.verify(service).requestStart(Mockito.any(), Mockito.any());
    }

    @Test
    public void onConnect_notWebsocketTransport_requestStartIsNotCalledOnServiceInstance() {
        VaadinServletService service = runTest((handler, resource) -> {
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.AJAX);
            handler.onConnect(resource);
        });

        Mockito.verify(service, Mockito.times(0)).requestStart(Mockito.any(),
                Mockito.any());
    }

    @Test
    public void onMessage_websocketTransport_requestStartIsCalledOnServiceInstance() {
        VaadinServletService service = runTest((handler, resource) -> {
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
            handler.onMessage(resource);
        });

        Mockito.verify(service).requestStart(Mockito.any(), Mockito.any());
    }

    @Test
    public void onMessage_notWebsocketTransport_requestStartIsNotCalledOnServiceInstance() {
        VaadinServletService service = runTest((handler, resource) -> {
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.AJAX);
            handler.onMessage(resource);
        });

        Mockito.verify(service, Mockito.times(0)).requestStart(Mockito.any(),
                Mockito.any());
    }

    @Test
    public void onConnect_devMode_websocket_refreshConnection_onConnectIsCalled_callWithUIIsNotCalled()
            throws ServiceException {
        MockVaadinServletService service = Mockito
                .spy(MockVaadinServletService.class);
        MockDeploymentConfiguration deploymentConfiguration = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        deploymentConfiguration.setProductionMode(false);
        deploymentConfiguration.setDevModeLiveReloadEnabled(true);

        VaadinContext context = service.getContext();
        BrowserLiveReload liveReload = BrowserLiveReloadAccessTest
                .mockBrowserLiveReloadImpl(context);

        AtomicReference<AtmosphereResource> res = new AtomicReference<>();
        runTest(service, (handler, resource) -> {
            AtmosphereRequest request = resource.getRequest();
            Mockito.when(request
                    .getParameter(ApplicationConstants.LIVE_RELOAD_CONNECTION))
                    .thenReturn("");
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
            handler.onConnect(resource);
            res.set(resource);
        });
        Mockito.verify(service, Mockito.times(0)).requestStart(Mockito.any(),
                Mockito.any());
        Mockito.verify(liveReload).onConnect(res.get());
    }

    @Test
    public void onMessage_devMode_websocket_refreshConnection_callWithUIIsNotCalled()
            throws ServiceException {
        MockVaadinServletService service = Mockito
                .spy(MockVaadinServletService.class);
        MockDeploymentConfiguration deploymentConfiguration = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        deploymentConfiguration.setProductionMode(false);
        deploymentConfiguration.setDevModeLiveReloadEnabled(true);

        VaadinContext context = service.getContext();

        AtomicReference<AtmosphereResource> res = new AtomicReference<>();
        runTest(service, (handler, resource) -> {
            AtmosphereRequest request = resource.getRequest();
            Mockito.when(request
                    .getParameter(ApplicationConstants.LIVE_RELOAD_CONNECTION))
                    .thenReturn("");
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
            handler.onMessage(resource);
            res.set(resource);
        });
        Mockito.verify(service, Mockito.times(0)).requestStart(Mockito.any(),
                Mockito.any());
    }

    private VaadinServletService runTest(VaadinServletService service,
                                         BiConsumer<PushHandler, AtmosphereResource> testExec)
            throws ServiceException {
        service.init();
        PushHandler handler = new PushHandler(service);

        AtmosphereResource resource = Mockito.mock(AtmosphereResource.class);
        AtmosphereRequest request = Mockito.mock(AtmosphereRequest.class);
        Mockito.when(resource.getRequest()).thenReturn(request);

        testExec.accept(handler, resource);

        return service;
    }

    private VaadinServletService runTest(
            BiConsumer<PushHandler, AtmosphereResource> testExec) {
        MockVaadinServletService service = Mockito
                .spy(MockVaadinServletService.class);
        service.init();
        PushHandler handler = new PushHandler(service);

        AtmosphereResource resource = Mockito.mock(AtmosphereResource.class);
        AtmosphereRequest request = Mockito.mock(AtmosphereRequest.class);
        Mockito.when(resource.getRequest()).thenReturn(request);

        testExec.accept(handler, resource);

        return service;
    }
}
