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
package com.vaadin.flow.server;

import jakarta.servlet.ServletException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.streams.ElementRequestHandler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamResourceRegistryTest {

    private UI ui;
    private VaadinServletService service;
    private VaadinSession session;

    @BeforeEach
    public void setUp() throws ServletException, ServiceException {
        service = new MockVaadinServletService();
        session = new VaadinSession(service) {
            @Override
            public boolean hasLock() {
                return true;
            }
        };

        ui = Mockito.mock(UI.class);
        Mockito.when(ui.getUIId()).thenReturn(1);
        UI.setCurrent(ui);
    }

    @AfterEach
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void registerResource_registrationResultCanBeFound() {
        StreamResourceRegistry registry = new StreamResourceRegistry(session);

        StreamResource resource = new StreamResource("name",
                () -> makeEmptyStream());
        StreamRegistration registration = registry.registerResource(resource);
        assertNotNull(registration);

        URI uri = registration.getResourceUri();

        Optional<StreamResource> stored = registry
                .getResource(StreamResource.class, uri);
        assertSame(resource, stored.get(),
                "Unexpected stored resource is returned for registered URI");

        assertSame(resource, registration.getResource(),
                "Unexpected resource is returned by the registration instance");
    }

    @Test
    public void registerElementResourceHandler_registrationResultCanBeFound() {
        StreamResourceRegistry registry = new StreamResourceRegistry(session);

        ElementRequestHandler handler = (request, response, session, owner) -> {
            // nop
        };
        Element owner = Mockito.mock(Element.class);
        StreamRegistration registration = registry.registerResource(handler,
                owner);
        assertNotNull(registration);

        URI uri = registration.getResourceUri();
        AbstractStreamResource generatedResource = registration.getResource();

        Optional<AbstractStreamResource> stored = registry.getResource(uri);
        assertSame(generatedResource, stored.get(),
                "Unexpected stored resource is returned for registered URI");
    }

    @Test
    public void unregisterResource_resourceIsRemoved() {
        StreamResourceRegistry registry = new StreamResourceRegistry(session);

        StreamResource resource = new StreamResource("name",
                () -> makeEmptyStream());
        StreamRegistration registration = registry.registerResource(resource);
        assertNotNull(registration);

        URI uri = registration.getResourceUri();

        registration.unregister();

        Optional<StreamResource> stored = registry
                .getResource(StreamResource.class, uri);
        assertFalse(stored.isPresent(),
                "Unexpected stored resource is found after unregister()");
        assertNull(registration.getResource(),
                "Unexpected resource is returned by the registration instance");
    }

    @Test
    public void unregisterElementResourceHandler_resourceIsRemoved() {
        StreamResourceRegistry registry = new StreamResourceRegistry(session);

        ElementRequestHandler handler = (request, response, session, owner) -> {
            // nop
        };
        Element owner = Mockito.mock(Element.class);
        StreamRegistration registration = registry.registerResource(handler,
                owner);

        assertNotNull(registration);

        URI uri = registration.getResourceUri();

        registration.unregister();

        Optional<AbstractStreamResource> stored = registry.getResource(uri);
        assertFalse(stored.isPresent(),
                "Unexpected stored resource is found after unregister()");
        assertNull(registration.getResource(),
                "Unexpected resource is returned by the registration instance");
    }

    @Test
    public void registerTwoResourcesWithSameName_resourcesHasDifferentURI() {
        StreamResourceRegistry registry = new StreamResourceRegistry(session);

        StreamResource resource1 = new StreamResource("name",
                () -> makeEmptyStream());
        StreamRegistration registration1 = registry.registerResource(resource1);

        StreamResource resource2 = new StreamResource("name",
                () -> makeEmptyStream());
        StreamRegistration registration2 = registry.registerResource(resource2);

        assertNotEquals(registration1.getResourceUri(),
                registration2.getResourceUri(),
                "Two different resource are registered to the same URI");

        registration1.unregister();

        assertTrue(
                registry.getResource(registration2.getResourceUri())
                        .isPresent(),
                "Second resource is not found after first resource has been unregistered");
    }

    @Test
    public void getResourceUriIsEncoded_withQueryParams() {
        assertResourceUriIsEncoded("a?b=c d&e", "a%3Fb%3Dc%20d%26e");
    }

    @Test
    public void getResourceUriIsEncoded_withContainingPlus() {
        assertResourceUriIsEncoded("image++.svg", "image%2B%2B.svg");
    }

    @Test
    public void getResourceUriIsEncoded_withSimpleSpace() {
        assertResourceUriIsEncoded("my file.png", "my%20file.png");
    }

    private void assertResourceUriIsEncoded(String resourceName,
            String suffix) {
        StreamResourceRegistry registry = new StreamResourceRegistry(session);

        StreamResource resource = new StreamResource(resourceName,
                this::makeEmptyStream);
        StreamRegistration registration = registry.registerResource(resource);

        URI uri = registration.getResourceUri();
        assertTrue(uri.toString().endsWith(suffix),
                "Resource URI is not properly encoded");
    }

    private InputStream makeEmptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }
}
