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

import org.atmosphere.cpr.HeaderConfig;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.shared.ui.Transport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushRequestHandlerTest {

    @Test
    void serverSentEventsRequest_proxyBufferingDisabled() {
        // The client puts the transport in the query string, an Atmosphere
        // client may put it in a header instead.
        assertBufferingDisabled(requestWithParameter(
                Transport.SERVER_SENT_EVENTS.getIdentifier()));
        assertBufferingDisabled(requestWithHeader(
                Transport.SERVER_SENT_EVENTS.getIdentifier()));
    }

    @Test
    void otherTransports_responseNotTouched() {
        assertBufferingNotTouched(
                requestWithParameter(Transport.WEBSOCKET.getIdentifier()));
        assertBufferingNotTouched(
                requestWithParameter(Transport.LONG_POLLING.getIdentifier()));
        assertBufferingNotTouched(requestWithParameter(null));
    }

    private static void assertBufferingDisabled(VaadinRequest request) {
        VaadinResponse response = mock(VaadinResponse.class);

        PushRequestHandler.disableProxyBufferingForServerSentEvents(request,
                response);

        verify(response).setHeader("X-Accel-Buffering", "no");
    }

    private static void assertBufferingNotTouched(VaadinRequest request) {
        VaadinResponse response = mock(VaadinResponse.class);

        PushRequestHandler.disableProxyBufferingForServerSentEvents(request,
                response);

        verify(response, never()).setHeader(anyString(), any());
    }

    private static VaadinRequest requestWithParameter(String transport) {
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getParameter(HeaderConfig.X_ATMOSPHERE_TRANSPORT))
                .thenReturn(transport);
        return request;
    }

    private static VaadinRequest requestWithHeader(String transport) {
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getHeader(HeaderConfig.X_ATMOSPHERE_TRANSPORT))
                .thenReturn(transport);
        return request;
    }
}
