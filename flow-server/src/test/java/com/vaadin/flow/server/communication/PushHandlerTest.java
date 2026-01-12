/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionExpiredException;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
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
        deploymentConfiguration.setDevToolsEnabled(true);

        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(applicationConfiguration.isProductionMode())
                .thenReturn(false);

        VaadinContext context = service.getContext();
        context.setAttribute(ApplicationConfiguration.class,
                applicationConfiguration);

        BrowserLiveReload liveReload = mockBrowserLiveReloadImpl(
                service.getLookup());

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
        deploymentConfiguration.setDevToolsEnabled(true);
        setProductionMode(service, false);

        mockBrowserLiveReloadImpl(service.getLookup());

        AtomicReference<AtmosphereResource> res = new AtomicReference<>();
        runTest(service, (handler, resource) -> {
            AtmosphereRequest request = resource.getRequest();
            try {
                Mockito.when(request.getReader())
                        .thenReturn(new BufferedReader(new StringReader("")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
    public void onConnect_devMode_websocket_noRefreshConnection_delegteCallWithUI()
            throws ServiceException {
        MockVaadinServletService service = Mockito
                .spy(MockVaadinServletService.class);
        MockDeploymentConfiguration deploymentConfiguration = (MockDeploymentConfiguration) service
                .getDeploymentConfiguration();
        deploymentConfiguration.setProductionMode(false);
        runTest(service, (handler, resource) -> {
            AtmosphereRequest request = resource.getRequest();
            Mockito.when(request
                    .getParameter(ApplicationConstants.DEBUG_WINDOW_CONNECTION))
                    .thenReturn(null);
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
            handler.onConnect(resource);
        });
        Mockito.verify(service).requestStart(Mockito.any(), Mockito.any());
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
        try {
            mockConnectionLost(new MockVaadinSession(), false);

            Assert.assertNull(VaadinSession.getCurrent());
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    public void connectionLost_sessionIsSetViaCurrent_currentInstancesAreCleared()
            throws SessionExpiredException {
        try {
            mockConnectionLost(new MockVaadinSession(), true);
            Assert.assertNotNull(VaadinSession.getCurrent());
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    public void connect_noSession_sendNotification() {
        try {
            Assert.assertNull(VaadinSession.getCurrent());
            AtomicReference<AtmosphereResource> res = new AtomicReference<>();

            runTest((handler, resource) -> {
                Mockito.when(resource.transport())
                        .thenReturn(TRANSPORT.WEBSOCKET);
                handler.onConnect(resource);
                res.set(resource);
            });
            Assert.assertNull(VaadinSession.getCurrent());
            Mockito.verify(res.get(), Mockito.times(2)).getResponse();
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    public void connectionLost_connectWithoutSession_doNotSendNotification() {
        try {
            AtmosphereResource resource = Mockito
                    .mock(AtmosphereResource.class);
            Mockito.when(resource.uuid()).thenReturn("1");
            try {
                MockVaadinServletService service = new MockVaadinServletService() {
                    @Override
                    public com.vaadin.flow.server.VaadinSession findVaadinSession(
                            VaadinRequest request)
                            throws SessionExpiredException {
                        // simulating expired session.
                        throw new SessionExpiredException();
                    }

                    @Override
                    public UI findUI(VaadinRequest request) {
                        return null;
                    }
                };
                setProductionMode(service, false);

                VaadinSession.setCurrent(null);
                PushHandler handler = new PushHandler(service);

                AtmosphereRequest request = Mockito
                        .mock(AtmosphereRequest.class);
                Mockito.when(resource.getRequest()).thenReturn(request);

                AtmosphereResourceEvent event = Mockito
                        .mock(AtmosphereResourceEvent.class);
                Mockito.when(event.getResource()).thenReturn(resource);

                AtmosphereResponse response = Mockito
                        .mock(AtmosphereResponse.class);
                Mockito.when(response.getWriter())
                        .thenReturn(Mockito.mock(PrintWriter.class));
                Mockito.when(resource.getResponse()).thenReturn(response);

                Mockito.when(resource.transport())
                        .thenReturn(TRANSPORT.WEBSOCKET);

                // Connection lost without session (could be with session too,
                // simplified for the test).
                handler.connectionLost(event);
                handler.onConnect(resource); // connecting without session

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Assert.assertNull(VaadinSession.getCurrent());
            Mockito.verify(resource, Mockito.times(0)).getResponse();
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    public void debugWindowConnection_productionMode_mustNeverBeConnected()
            throws Exception {
        MockVaadinServletService service = Mockito
                .spy(MockVaadinServletService.class);
        setProductionMode(service, true);

        runTest(service, (handler, resource) -> {
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
            Mockito.when(resource.getRequest()
                    .getParameter(ApplicationConstants.DEBUG_WINDOW_CONNECTION))
                    .thenReturn("");
            Mockito.doNothing().when(handler)
                    .callWithServiceAndSession(Mockito.any(), Mockito.any());

            handler.onConnect(resource);

            Mockito.verify(handler, Mockito.never())
                    .callWithServiceAndSession(Mockito.any(), Mockito.any());
            Mockito.verify(handler, Mockito.never()).callWithUi(Mockito.any(),
                    Mockito.any());
        });

    }

    private void setProductionMode(VaadinService service,
            boolean productionMode) {
        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(applicationConfiguration.isProductionMode())
                .thenReturn(productionMode);

        VaadinContext context = service.getContext();
        context.setAttribute(ApplicationConfiguration.class,
                applicationConfiguration);

    }

    private MockVaadinServletService mockConnectionLost(VaadinSession session,
            boolean setSession) {
        AtomicBoolean sessionIsSet = new AtomicBoolean();
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public com.vaadin.flow.server.VaadinSession findVaadinSession(
                    VaadinRequest request) throws SessionExpiredException {
                VaadinSession.setCurrent(session);
                sessionIsSet.set(true);
                if (session != null) {
                    Assert.assertNotNull(VaadinSession.getCurrent());
                } else {
                    throw new SessionExpiredException();
                }
                return session;
            }

            @Override
            public UI findUI(VaadinRequest request) {
                return null;
            }
        };
        setProductionMode(service, false);

        if (setSession) {
            VaadinSession.setCurrent(session);
        }
        PushHandler handler = new PushHandler(service);

        AtmosphereResource resource = Mockito.mock(AtmosphereResource.class);
        AtmosphereRequest request = Mockito.mock(AtmosphereRequest.class);
        Mockito.when(resource.getRequest()).thenReturn(request);
        Mockito.when(resource.uuid()).thenReturn("1");

        AtmosphereResourceEvent event = Mockito
                .mock(AtmosphereResourceEvent.class);
        Mockito.when(event.getResource()).thenReturn(resource);
        handler.connectionLost(event);

        Assert.assertTrue(sessionIsSet.get());

        return service;
    }

    private VaadinServletService runTest(VaadinServletService service,
            BiConsumer<PushHandler, AtmosphereResource> testExec)
            throws ServiceException {
        try {
            PushHandler handler = Mockito.spy(new PushHandler(service));

            AtmosphereResource resource = Mockito
                    .mock(AtmosphereResource.class);
            AtmosphereRequest request = Mockito.mock(AtmosphereRequest.class);
            AtmosphereResponse response = Mockito
                    .mock(AtmosphereResponse.class);
            Mockito.when(response.getWriter())
                    .thenReturn(Mockito.mock(PrintWriter.class));
            Mockito.when(resource.getRequest()).thenReturn(request);
            Mockito.when(resource.getResponse()).thenReturn(response);
            Mockito.when(resource.uuid()).thenReturn("1");

            testExec.accept(handler, resource);

            return service;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private VaadinServletService runTest(
            BiConsumer<PushHandler, AtmosphereResource> testExec) {
        MockVaadinServletService service = Mockito
                .spy(MockVaadinServletService.class);
        try {
            runTest(service, testExec);
            return service;
        } catch (ServiceException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static BrowserLiveReload mockBrowserLiveReloadImpl(Lookup lookup) {
        BrowserLiveReload liveReload = Mockito.mock(BrowserLiveReload.class);
        Mockito.when(lookup.lookup(BrowserLiveReloadAccessor.class))
                .thenReturn(context -> liveReload);
        return liveReload;
    }
}
