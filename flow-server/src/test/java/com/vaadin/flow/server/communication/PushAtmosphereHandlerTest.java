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
 *
 */

package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.VaadinServletService;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PushAtmosphereHandlerTest {

    private AtmosphereResource resource;

    private AtmosphereRequest request;
    private AtmosphereResponse response;
    private PrintWriter printWriter;

    private PushAtmosphereHandler atmosphereHandler;

    @Before
    public void setup() throws IOException {
        request = Mockito.mock(AtmosphereRequest.class);
        response = Mockito.mock(AtmosphereResponse.class);
        printWriter = Mockito.mock(PrintWriter.class);
        Mockito.when(response.getWriter()).thenReturn(printWriter);

        resource = Mockito.mock(AtmosphereResource.class);
        Mockito.when(resource.getRequest()).thenReturn(request);
        Mockito.when(resource.getResponse()).thenReturn(response);

        VaadinServletService service = new VaadinServletService(null,
                new DefaultDeploymentConfiguration(getClass(),
                        new Properties()));

        PushHandler handler = new PushHandler(service);

        atmosphereHandler = new PushAtmosphereHandler();
        atmosphereHandler.setPushHandler(handler);
    }

    @Test
    public void writeSessionExpiredAsyncGet() throws Exception {
        writeSessionExpiredAsync("GET");
    }

    @Test
    public void writeSessionExpiredAsyncPost() throws Exception {
        writeSessionExpiredAsync("POST");
    }

    private void writeSessionExpiredAsync(String httpMethod) throws IOException {
        Mockito.when(request.getMethod()).thenReturn(httpMethod);

        atmosphereHandler.onRequest(resource);

        String responseContent = CommunicationUtil
                .getStringWhenWriteString(printWriter);

        // response shouldn't contain async
        Assert.assertEquals("Invalid response",
                "for(;;);[{\"meta\":{\"async\":true,\"sessionExpired\":true}}]",
                responseContent);
    }

}
