/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccess;
import com.vaadin.flow.internal.DebugWindowConnectionTest;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionExpiredException;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
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

    public void onConnect_devMode_websocket_refreshConnection_onConnectIsCalled_callWithUIIsNotCalled()
            throws ServiceException {
        MockVaadinServletService service = Mockito
                .spy(MockVaadinServletService.class);
        MockDeploymentConfiguration deploymentConfiguration = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        deploymentConfiguration.setProductionMode(false);
        deploymentConfiguration.setDevModeLiveReloadEnabled(true);
        service.init();

        VaadinContext context = service.getContext();
        BrowserLiveReload liveReload = DebugWindowConnectionTest
                .mockBrowserLiveReloadImpl(context);

        AtomicReference<AtmosphereResource> res = new AtomicReference<>();
        runTest(service, (handler, resource) -> {
            AtmosphereRequest request = resource.getRequest();
            Mockito.when(request
                    .getParameter(ApplicationConstants.DEBUG_WINDOW_CONNECTION))
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

        service.init();
        mockBrowserLiveReload(service);

        AtomicReference<AtmosphereResource> res = new AtomicReference<>();
        runTest(service, (handler, resource) -> {
            AtmosphereRequest request = resource.getRequest();
            Mockito.when(request
                    .getParameter(ApplicationConstants.DEBUG_WINDOW_CONNECTION))
                    .thenReturn("");
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
            try {
                Mockito.when(request.getReader())
                        .thenReturn(new BufferedReader(new StringReader("{}")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            handler.onMessage(resource);
            res.set(resource);
        });
        Mockito.verify(service, Mockito.times(0)).requestStart(Mockito.any(),
                Mockito.any());
    }

    @Test
    public void onConnect_devMode_notWebsocket_refreshConnection_delegteCallWithUI()
            throws ServiceException, SessionExpiredException {
        MockVaadinServletService service = Mockito
                .spy(MockVaadinServletService.class);
        MockDeploymentConfiguration deploymentConfiguration = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        deploymentConfiguration.setProductionMode(false);
        runTest(service, (handler, resource) -> {
            AtmosphereRequest request = resource.getRequest();
            Mockito.when(request
                    .getParameter(ApplicationConstants.DEBUG_WINDOW_CONNECTION))
                    .thenReturn("");
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.AJAX);
            handler.onConnect(resource);
        });
        Mockito.verify(service).findVaadinSession(Mockito.any());
    }

    @Test
    public void connectionLost_noSession_currentInstancesAreCleared()
            throws SessionExpiredException {
        mockConnectionLost(new MockVaadinSession(), false);

        Assert.assertNull(VaadinSession.getCurrent());
    }

    @Test
    public void connectionLost_sessionIsSetViaCurrent_currentInstancesAreCleared()
            throws SessionExpiredException {
        VaadinSession session = new MockVaadinSession();

        mockConnectionLost(session, true);
        Assert.assertNotNull(VaadinSession.getCurrent());
    }

    private void mockConnectionLost(VaadinSession session, boolean setSession) {
        AtomicBoolean sessionIsSet = new AtomicBoolean();
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public com.vaadin.flow.server.VaadinSession findVaadinSession(
                    VaadinRequest request) throws SessionExpiredException {
                VaadinSession.setCurrent(session);
                sessionIsSet.set(true);
                Assert.assertNotNull(VaadinSession.getCurrent());
                return session;
            }

            @Override
            public UI findUI(VaadinRequest request) {
                return null;
            }
        };

        service.init();

        if (setSession) {
            VaadinSession.setCurrent(session);
        }
        PushHandler handler = new PushHandler(service);

        AtmosphereResource resource = Mockito.mock(AtmosphereResource.class);
        AtmosphereRequest request = Mockito.mock(AtmosphereRequest.class);
        Mockito.when(resource.getRequest()).thenReturn(request);

        AtmosphereResourceEvent event = Mockito
                .mock(AtmosphereResourceEvent.class);
        Mockito.when(event.getResource()).thenReturn(resource);
        handler.connectionLost(event);

        handler.connectionLost(event);

        Assert.assertTrue(sessionIsSet.get());
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

    private void mockBrowserLiveReload(MockVaadinServletService service) {
        Instantiator instantiator = Mockito.mock(Instantiator.class);
        Mockito.when(service.getInstantiator()).thenReturn(instantiator);

        BrowserLiveReloadAccess browserLiveReloadAccess = Mockito
                .mock(BrowserLiveReloadAccess.class);
        Mockito.when(instantiator.getOrCreate(BrowserLiveReloadAccess.class))
                .thenReturn(browserLiveReloadAccess);

        BrowserLiveReload liveReload = Mockito.mock(BrowserLiveReload.class);
        Mockito.when(browserLiveReloadAccess.getLiveReload(service))
                .thenReturn(liveReload);
    }
}
