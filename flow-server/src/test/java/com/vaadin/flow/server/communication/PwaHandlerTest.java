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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.PwaIcon;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PwaHandlerTest {

    private final VaadinSession session = mock(VaadinSession.class);

    private final VaadinRequest request = mock(VaadinRequest.class);

    private final VaadinResponse response = mock(VaadinResponse.class);

    @Test
    void handleRequest_noPwaRegistry_returnsFalse() throws IOException {
        PwaHandler handler = new PwaHandler(() -> null);
        assertFalse(handler.handleRequest(session, request, response));
    }

    @Test
    void handleRequest_pwaRegistryConfigIsDisabled_returnsFalse()
            throws IOException {
        PwaRegistry registry = mock(PwaRegistry.class);
        PwaConfiguration configuration = mock(PwaConfiguration.class);
        when(registry.getPwaConfiguration()).thenReturn(configuration);
        when(configuration.isEnabled()).thenReturn(false);
        PwaHandler handler = new PwaHandler(() -> registry);
        assertFalse(handler.handleRequest(session, request, response));
    }

    @Test
    void handleRequest_pwaRegistryConfigIsEnabled_pathIsPwaResource_returnsTrue()
            throws IOException {
        PwaRegistry registry = mock(PwaRegistry.class);
        PwaConfiguration configuration = mock(PwaConfiguration.class);
        when(registry.getPwaConfiguration()).thenReturn(configuration);
        when(configuration.isEnabled()).thenReturn(true);
        PwaHandler handler = new PwaHandler(() -> registry);

        when(response.getWriter())
                .thenReturn(new PrintWriter(new StringWriter()));
        when(registry.getRuntimeServiceWorkerJs(any())).thenReturn("");

        when(request.getPathInfo())
                .thenReturn("/sw-runtime-resources-precache.js");
        assertTrue(handler.handleRequest(session, request, response));
    }

    @Test
    void handleRequest_pwaRegistryConfigIsEnabled_handlerIsInitializedOnce()
            throws IOException {

        PwaRegistry registry = mock(PwaRegistry.class);
        PwaConfiguration configuration = mock(PwaConfiguration.class);
        when(registry.getPwaConfiguration()).thenReturn(configuration);
        when(configuration.isEnabled()).thenReturn(true);
        PwaHandler handler = new PwaHandler(() -> registry);

        when(response.getWriter())
                .thenReturn(new PrintWriter(new StringWriter()));
        when(registry.getRuntimeServiceWorkerJs(any())).thenReturn("");
        when(request.getPathInfo())
                .thenReturn("/sw-runtime-resources-precache.js");

        assertTrue(handler.handleRequest(session, request, response));
        assertTrue(handler.handleRequest(session, request, response));

        verify(registry, times(1)).getIcons();
    }

    @Test
    void handleRequest_writeIconOnResponseFailure_doesNotThrow()
            throws Exception {

        PwaRegistry registry = mock(PwaRegistry.class);
        PwaConfiguration configuration = mock(PwaConfiguration.class);
        when(registry.getPwaConfiguration()).thenReturn(configuration);

        PwaHandler handler = new PwaHandler(() -> registry);

        PwaIcon icon = spy(createIcon(registry, 100));
        when(registry.getIcons()).thenReturn(List.of(icon));
        when(configuration.isEnabled()).thenReturn(true);

        doAnswer(i -> {
            throw new UncheckedIOException(
                    "Failed to store the icon image into the stream provided",
                    new IOException("Broken pipe"));
        }).when(icon).write(any());
        when(request.getPathInfo()).thenReturn("/icons/icon-100x100.png");

        assertTrue(handler.handleRequest(session, request, response));
    }

    private PwaIcon createIcon(PwaRegistry registry, int size)
            throws Exception {
        Constructor<PwaIcon> ctor = PwaIcon.class
                .getDeclaredConstructor(int.class, int.class, String.class);
        ctor.setAccessible(true);
        PwaIcon icon = ctor.newInstance(size, size,
                PwaConfiguration.DEFAULT_ICON);
        icon.setRegistry(registry);
        Method setImage = PwaIcon.class.getDeclaredMethod("setImage",
                InputStream.class);
        setImage.setAccessible(true);
        setImage.invoke(icon, new ByteArrayInputStream(new byte[] { 1 }));
        return icon;
    }
}
