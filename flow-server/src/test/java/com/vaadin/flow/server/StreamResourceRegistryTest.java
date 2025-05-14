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
package com.vaadin.flow.server;

import jakarta.servlet.ServletException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.streams.ElementRequestHandler;

public class StreamResourceRegistryTest {

    private UI ui;
    private VaadinServletService service;
    private VaadinSession session;

    @Before
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

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void registerResource_registrationResultCanBeFound() {
        StreamResourceRegistry registry = new StreamResourceRegistry(session);

        StreamResource resource = new StreamResource("name",
                () -> makeEmptyStream());
        StreamRegistration registration = registry.registerResource(resource);
        Assert.assertNotNull(registration);

        URI uri = registration.getResourceUri();

        Optional<StreamResource> stored = registry
                .getResource(StreamResource.class, uri);
        Assert.assertSame(
                "Unexpected stored resource is returned for registered URI",
                resource, stored.get());

        Assert.assertSame(
                "Unexpected resource is returned by the registration instance",
                resource, registration.getResource());
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
        Assert.assertNotNull(registration);

        URI uri = registration.getResourceUri();
        AbstractStreamResource generatedResource = registration.getResource();

        Optional<AbstractStreamResource> stored = registry.getResource(uri);
        Assert.assertSame(
                "Unexpected stored resource is returned for registered URI",
                generatedResource, stored.get());
    }

    @Test
    public void unregisterResource_resourceIsRemoved() {
        StreamResourceRegistry registry = new StreamResourceRegistry(session);

        StreamResource resource = new StreamResource("name",
                () -> makeEmptyStream());
        StreamRegistration registration = registry.registerResource(resource);
        Assert.assertNotNull(registration);

        URI uri = registration.getResourceUri();

        registration.unregister();

        Optional<StreamResource> stored = registry
                .getResource(StreamResource.class, uri);
        Assert.assertFalse(
                "Unexpected stored resource is found after unregister()",
                stored.isPresent());
        Assert.assertNull(
                "Unexpected resource is returned by the registration instance",
                registration.getResource());
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

        Assert.assertNotNull(registration);

        URI uri = registration.getResourceUri();

        registration.unregister();

        Optional<AbstractStreamResource> stored = registry.getResource(uri);
        Assert.assertFalse(
                "Unexpected stored resource is found after unregister()",
                stored.isPresent());
        Assert.assertNull(
                "Unexpected resource is returned by the registration instance",
                registration.getResource());
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

        Assert.assertNotEquals(
                "Two different resource are registered to the same URI",
                registration1.getResourceUri(), registration2.getResourceUri());

        registration1.unregister();

        Assert.assertTrue(
                "Second resource is not found after first resource has been unregistered",
                registry.getResource(registration2.getResourceUri())
                        .isPresent());
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
        Assert.assertTrue("Resource URI is not properly encoded",
                uri.toString().endsWith(suffix));
    }

    private InputStream makeEmptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }
}
