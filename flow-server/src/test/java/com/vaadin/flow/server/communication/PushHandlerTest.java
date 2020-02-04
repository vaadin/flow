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

import java.util.function.BiConsumer;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinServletService;

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
