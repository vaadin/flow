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

import java.io.IOException;
import java.io.PrintWriter;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinServletService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PushAtmosphereHandlerTest {

    private AtmosphereResource resource;

    private AtmosphereRequest request;
    private AtmosphereResponse response;
    private PrintWriter printWriter;

    private PushAtmosphereHandler atmosphereHandler;

    @BeforeEach
    void setup() throws IOException {
        request = mock(AtmosphereRequest.class);
        response = mock(AtmosphereResponse.class);
        printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);

        resource = mock(AtmosphereResource.class);
        when(resource.getRequest()).thenReturn(request);
        when(resource.getResponse()).thenReturn(response);
        when(resource.uuid()).thenReturn("1");

        // The service must be initialized, otherwise the push handler waits
        // for it to become ready instead of reporting the expired session
        VaadinServletService service = new MockVaadinServletService();

        PushHandler handler = new PushHandler(service);

        atmosphereHandler = new PushAtmosphereHandler();
        atmosphereHandler.setPushHandler(handler);
    }

    @Test
    void writeSessionExpiredAsyncGet() throws Exception {
        writeSessionExpiredAsync("GET");
    }

    @Test
    void writeSessionExpiredAsyncPost() throws Exception {
        writeSessionExpiredAsync("POST");
    }

    private void writeSessionExpiredAsync(String httpMethod)
            throws IOException {
        when(request.getMethod()).thenReturn(httpMethod);

        atmosphereHandler.onRequest(resource);

        String responseContent = CommunicationUtil
                .getStringWhenWriteString(printWriter);

        // response shouldn't contain async
        assertEquals("{\"meta\":{\"async\":true,\"sessionExpired\":true}}",
                responseContent, "Invalid response");
    }

}
