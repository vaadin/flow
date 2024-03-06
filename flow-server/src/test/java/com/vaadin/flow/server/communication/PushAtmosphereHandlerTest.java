/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Properties;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

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

        VaadinContext context = new MockVaadinContext();
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(config.getContext()).thenReturn(context);
        VaadinServletService service = new VaadinServletService(null,
                new DefaultDeploymentConfiguration(config, getClass(),
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

    private void writeSessionExpiredAsync(String httpMethod)
            throws IOException {
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
