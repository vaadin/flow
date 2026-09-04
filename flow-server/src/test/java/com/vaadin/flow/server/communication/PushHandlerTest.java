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
import org.atmosphere.cpr.AtmosphereResourceImpl;
import org.atmosphere.cpr.AtmosphereResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PushHandlerTest {

    /** How long to wait for a held request to have been run. */
    private static final int AWAIT_MILLIS = 10000;

    /**
     * How long to let a held request run before checking that it did nothing.
     */
    private static final int AWAIT_SETTLE_MILLIS = 500;

    @Test
    void onConnect_websocketTransport_requestStartIsCalledOnServiceInstance() {
        VaadinServletService service = runTest((handler, resource) -> {
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
            handler.onConnect(resource);
        });

        Mockito.verify(service).requestStart(Mockito.any(), Mockito.any());
    }

    @Test
    void onConnect_notWebsocketTransport_requestStartIsNotCalledOnServiceInstance() {
        VaadinServletService service = runTest((handler, resource) -> {
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.AJAX);
            handler.onConnect(resource);
        });

        Mockito.verify(service, Mockito.times(0)).requestStart(Mockito.any(),
                Mockito.any());
    }

    @Test
    void onMessage_websocketTransport_requestStartIsCalledOnServiceInstance() {
        VaadinServletService service = runTest((handler, resource) -> {
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
            handler.onMessage(resource);
        });

        Mockito.verify(service).requestStart(Mockito.any(), Mockito.any());
    }

    @Test
    void onMessage_notWebsocketTransport_requestStartIsNotCalledOnServiceInstance() {
        VaadinServletService service = runTest((handler, resource) -> {
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.AJAX);
            handler.onMessage(resource);
        });

        Mockito.verify(service, Mockito.times(0)).requestStart(Mockito.any(),
                Mockito.any());
    }

    @Test
    void onConnect_serviceNotInitialized_connectionHeldUntilInitCompletes()
            throws ServiceException, IOException, InterruptedException {
        MockVaadinServletService service = Mockito
                .spy(new MockVaadinServletService(false));

        AtomicReference<AtmosphereResource> connectedResource = new AtomicReference<>();
        runTest(service, (handler, resource) -> {
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
            connectedResource.set(resource);
            handler.onConnect(resource);
        });

        AtmosphereResource resource = connectedResource.get();
        // Suspended so that the client does not see the connection fail while
        // it is being held
        Mockito.verify(resource).suspend(-1);
        Mockito.verify(service, Mockito.never()).requestStart(Mockito.any(),
                Mockito.any());

        service.init();

        Mockito.verify(service, Mockito.timeout(AWAIT_MILLIS))
                .requestStart(Mockito.any(), Mockito.any());
        Mockito.verify(resource, Mockito.never()).close();
    }

    @Test
    void onMessage_connectionStillHeld_clientIsRefreshedInsteadOfLeftWaiting()
            throws ServiceException, IOException, InterruptedException {
        MockVaadinServletService service = Mockito
                .spy(new MockVaadinServletService(false));
        PushHandler handler = new PushHandler(service);

        AtmosphereResourceImpl resource = Mockito
                .mock(AtmosphereResourceImpl.class);
        AtmosphereResponse response = Mockito.mock(AtmosphereResponse.class);
        Mockito.when(response.getWriter())
                .thenReturn(Mockito.mock(PrintWriter.class));
        Mockito.when(resource.getRequest())
                .thenReturn(Mockito.mock(AtmosphereRequest.class));
        Mockito.when(resource.getResponse()).thenReturn(response);
        Mockito.when(resource.uuid()).thenReturn("1");
        Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
        Mockito.when(resource.isInScope()).thenReturn(true);

        handler.onConnect(resource);

        // Holding the connection suspends it, which lets the client believe it
        // is connected and start sending before the connection is established
        handler.onMessage(resource);

        // Left waiting for a response that never comes the client would be
        // stuck, so it is told to refresh instead
        Mockito.verify(resource).resume();

        service.init();

        // Establishing it now would bind the UI to a connection the client has
        // already been told to drop
        Mockito.verify(service, Mockito.after(AWAIT_SETTLE_MILLIS).never())
                .requestStart(Mockito.any(), Mockito.any());
    }

    @Test
    void onConnect_serviceNotInitialized_notWebsocket_connectionIsNotHeld()
            throws ServiceException, IOException {
        MockVaadinServletService service = Mockito
                .spy(new MockVaadinServletService(false));

        AtomicReference<AtmosphereResource> connectedResource = new AtomicReference<>();
        runTest(service, (handler, resource) -> {
            Mockito.when(resource.transport())
                    .thenReturn(TRANSPORT.LONG_POLLING);
            connectedResource.set(resource);
            handler.onConnect(resource);
        });

        // Only a websocket reaches the handler before the service has been
        // initialized, and holding any other transport would run it without the
        // request context its own thread set up
        Mockito.verify(connectedResource.get(), Mockito.never())
                .suspend(Mockito.anyLong());
    }

    @Test
    void onMessage_debugWindowConnectionStillHeld_connectionClosedInsteadOfRefreshed()
            throws ServiceException, IOException {
        MockVaadinServletService service = Mockito
                .spy(new MockVaadinServletService(false));
        PushHandler handler = new PushHandler(service);

        AtmosphereResourceImpl resource = Mockito
                .mock(AtmosphereResourceImpl.class);
        AtmosphereRequest request = Mockito.mock(AtmosphereRequest.class);
        Mockito.when(request
                .getParameter(ApplicationConstants.DEBUG_WINDOW_CONNECTION))
                .thenReturn("");
        Mockito.when(resource.getRequest()).thenReturn(request);
        Mockito.when(resource.uuid()).thenReturn("1");
        Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
        Mockito.when(resource.isInScope()).thenReturn(true);

        handler.onConnect(resource);
        handler.onMessage(resource);

        // The debug window does not understand a Vaadin notification, so it is
        // given a closed connection to reopen instead
        Mockito.verify(resource).close();
        Mockito.verify(resource, Mockito.never()).resume();
    }

    @Test
    void onConnect_twoConnectionsWithTheSameTrackingId_areNotMistakenForEachOther()
            throws ServiceException, IOException {
        MockVaadinServletService service = Mockito
                .spy(new MockVaadinServletService(false));
        PushHandler handler = new PushHandler(service);

        // A client that reconnects while the service is still initializing
        // sends back the tracking id it was given, so the two connections share
        // a uuid even though they are different connections
        AtmosphereResource first = mockWebsocketResource("shared-uuid");
        AtmosphereResource second = mockWebsocketResource("shared-uuid");

        handler.onConnect(first);
        handler.onConnect(second);

        // The second one sends before its connection has been established and
        // is given up on. The first one must not be the one given up on.
        handler.onMessage(second);
        service.init();

        // Answering the first connection writes to it, which only happens if
        // it was established rather than mistaken for the second one
        Mockito.verify(first.getResponse(), Mockito.timeout(AWAIT_MILLIS))
                .getWriter();
        Mockito.verify(service, Mockito.timeout(AWAIT_MILLIS))
                .requestStart(Mockito.any(), Mockito.any());
    }

    private static AtmosphereResource mockWebsocketResource(String uuid)
            throws IOException {
        AtmosphereResourceImpl resource = Mockito
                .mock(AtmosphereResourceImpl.class);
        AtmosphereResponse response = Mockito.mock(AtmosphereResponse.class);
        Mockito.when(response.getWriter())
                .thenReturn(Mockito.mock(PrintWriter.class));
        Mockito.when(resource.getRequest())
                .thenReturn(Mockito.mock(AtmosphereRequest.class));
        Mockito.when(resource.getResponse()).thenReturn(response);
        Mockito.when(resource.uuid()).thenReturn(uuid);
        Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
        Mockito.when(resource.isInScope()).thenReturn(true);
        return resource;
    }

    @Test
    void onMessage_secondMessageOnAConnectionGivenUpOn_stillNotProcessed()
            throws ServiceException, IOException {
        MockVaadinServletService service = Mockito
                .spy(new MockVaadinServletService(false));
        PushHandler handler = new PushHandler(service);

        AtmosphereResource resource = mockWebsocketResource("1");
        handler.onConnect(resource);

        // A client that starts sending as soon as it believes it is connected
        // can send more than once before the service is ready
        handler.onMessage(resource);
        handler.onMessage(resource);

        // The second message must not be handled as if the connection existed,
        // which would start a request on a service that cannot serve one
        Mockito.verify(service, Mockito.never()).requestStart(Mockito.any(),
                Mockito.any());
        // Only the first message gives up on the connection
        Mockito.verify(resource, Mockito.times(1)).resume();
    }

    @Test
    void destroy_serviceGoesAwayBeforeInit_heldConnectionsAreClosed()
            throws ServiceException, IOException {
        MockVaadinServletService service = Mockito
                .spy(new MockVaadinServletService(false));
        PushHandler handler = new PushHandler(service);

        AtmosphereResource resource = mockWebsocketResource("1");
        handler.onConnect(resource);

        // A service that is destroyed without ever being initialized, which a
        // servlet that is never loaded does, must not keep what it holds
        handler.destroy();

        Mockito.verify(resource).close();
    }

    @Test
    void onConnect_serviceInitFails_connectionClosedAndRequestNotStarted()
            throws ServiceException, IOException, InterruptedException {
        MockVaadinServletService service = Mockito
                .spy(new MockVaadinServletService(false) {
                    @Override
                    protected Instantiator createInstantiator()
                            throws ServiceException {
                        throw new ServiceException("intentional failure");
                    }
                });

        AtomicReference<AtmosphereResource> connectedResource = new AtomicReference<>();
        runTest(service, (handler, resource) -> {
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
            connectedResource.set(resource);
            handler.onConnect(resource);
        });

        assertThrows(RuntimeException.class, service::init);

        Mockito.verify(connectedResource.get(), Mockito.timeout(AWAIT_MILLIS))
                .close();
        Mockito.verify(service, Mockito.never()).requestStart(Mockito.any(),
                Mockito.any());
    }

    @Test
    void onConnect_connectionClosedWhileHeld_requestIsNotProcessed()
            throws ServiceException, IOException, InterruptedException {
        MockVaadinServletService service = Mockito
                .spy(new MockVaadinServletService(false));
        PushHandler handler = new PushHandler(service);

        AtmosphereResourceImpl resource = Mockito
                .mock(AtmosphereResourceImpl.class);
        Mockito.when(resource.getRequest())
                .thenReturn(Mockito.mock(AtmosphereRequest.class));
        Mockito.when(resource.uuid()).thenReturn("1");
        Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);

        handler.onConnect(resource);

        // The client gives up on the connection while the request is held
        Mockito.when(resource.isInScope()).thenReturn(false);
        service.init();

        Mockito.verify(service, Mockito.after(AWAIT_SETTLE_MILLIS).never())
                .requestStart(Mockito.any(), Mockito.any());
        Mockito.verify(resource, Mockito.never()).close();
    }

    @Test
    void onConnect_devMode_websocket_refreshConnection_onConnectIsCalled_callWithUIIsNotCalled()
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
    void onMessage_devMode_websocket_refreshConnection_callWithUIIsNotCalled()
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
    void onConnect_devMode_websocket_noRefreshConnection_delegteCallWithUI()
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
    void onConnect_devMode_notWebsocket_refreshConnection_delegteCallWithUI()
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
    void connectionLost_noSession_currentInstancesAreCleared()
            throws SessionExpiredException {
        try {
            mockConnectionLost(new MockVaadinSession(), false);

            assertNull(VaadinSession.getCurrent());
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    void connectionLost_sessionIsSetViaCurrent_currentInstancesAreCleared()
            throws SessionExpiredException {
        try {
            mockConnectionLost(new MockVaadinSession(), true);
            assertNotNull(VaadinSession.getCurrent());
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    void connect_noSession_sendNotification() {
        try {
            assertNull(VaadinSession.getCurrent());
            AtomicReference<AtmosphereResource> res = new AtomicReference<>();

            runTest((handler, resource) -> {
                Mockito.when(resource.transport())
                        .thenReturn(TRANSPORT.WEBSOCKET);
                handler.onConnect(resource);
                res.set(resource);
            });
            assertNull(VaadinSession.getCurrent());
            Mockito.verify(res.get(), Mockito.times(2)).getResponse();
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    void connectionLost_connectWithoutSession_doNotSendNotification() {
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
            assertNull(VaadinSession.getCurrent());
            Mockito.verify(resource, Mockito.times(0)).getResponse();
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    void debugWindowConnection_productionMode_mustNeverBeConnected()
            throws Exception {
        MockVaadinServletService service = Mockito
                .spy(MockVaadinServletService.class);
        setProductionMode(service, true);

        AtomicReference<AtmosphereResource> deniedResource = new AtomicReference<>();
        runTest(service, (handler, resource) -> {
            Mockito.when(resource.transport()).thenReturn(TRANSPORT.WEBSOCKET);
            Mockito.when(resource.getRequest()
                    .getParameter(ApplicationConstants.DEBUG_WINDOW_CONNECTION))
                    .thenReturn("");
            Mockito.doNothing().when(handler)
                    .callWithServiceAndSession(Mockito.any(), Mockito.any());
            deniedResource.set(resource);

            handler.onConnect(resource);

            Mockito.verify(handler, Mockito.never())
                    .callWithServiceAndSession(Mockito.any(), Mockito.any());
            Mockito.verify(handler, Mockito.never()).callWithUi(Mockito.any(),
                    Mockito.any());
        });

        // The denied connection must not be left open, as it has been
        // suspended if it was held while the service was initializing
        Mockito.verify(deniedResource.get()).close();

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
                    assertNotNull(VaadinSession.getCurrent());
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

        assertTrue(sessionIsSet.get());

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
