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
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

public class PwaHandlerTest {

    private VaadinSession session = Mockito.mock(VaadinSession.class);

    private VaadinRequest request = Mockito.mock(VaadinRequest.class);

    private VaadinResponse response = Mockito.mock(VaadinResponse.class);

    @Test
    public void handleRequest_noPwaRegistry_returnsFalse() throws IOException {
        PwaHandler handler = new PwaHandler(() -> null);
        Assert.assertFalse(handler.handleRequest(session, request, response));
    }

    @Test
    public void handleRequest_pwaRegistryConfigIsDisabled_returnsFalse()
            throws IOException {
        PwaRegistry registry = Mockito.mock(PwaRegistry.class);
        PwaConfiguration configuration = Mockito.mock(PwaConfiguration.class);
        Mockito.when(registry.getPwaConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.isEnabled()).thenReturn(false);
        PwaHandler handler = new PwaHandler(() -> registry);
        Assert.assertFalse(handler.handleRequest(session, request, response));
    }

    @Test
    public void handleRequest_pwaRegistryConfigIsEnabled_pathIsPwaResource_returnsTrue()
            throws IOException {
        PwaRegistry registry = Mockito.mock(PwaRegistry.class);
        PwaConfiguration configuration = Mockito.mock(PwaConfiguration.class);
        Mockito.when(registry.getPwaConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.isEnabled()).thenReturn(true);
        PwaHandler handler = new PwaHandler(() -> registry);

        Mockito.when(response.getWriter())
                .thenReturn(new PrintWriter(new StringWriter()));
        Mockito.when(registry.getRuntimeServiceWorkerJs()).thenReturn("");

        Mockito.when(request.getPathInfo())
                .thenReturn("/sw-runtime-resources-precache.js");
        Assert.assertTrue(handler.handleRequest(session, request, response));
    }

    @Test
    public void handleRequest_pwaRegistryConfigIsEnabled_handlerIsInitializedOnce()
            throws IOException {

        PwaRegistry registry = Mockito.mock(PwaRegistry.class);
        PwaConfiguration configuration = Mockito.mock(PwaConfiguration.class);
        Mockito.when(registry.getPwaConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.isEnabled()).thenReturn(true);
        PwaHandler handler = new PwaHandler(() -> registry);

        Mockito.when(response.getWriter())
                .thenReturn(new PrintWriter(new StringWriter()));
        Mockito.when(registry.getRuntimeServiceWorkerJs()).thenReturn("");
        Mockito.when(request.getPathInfo())
                .thenReturn("/sw-runtime-resources-precache.js");

        Assert.assertTrue(handler.handleRequest(session, request, response));
        Assert.assertTrue(handler.handleRequest(session, request, response));

        Mockito.verify(registry, Mockito.times(1)).getIcons();
    }
}
