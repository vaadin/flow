/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletException;

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
import com.vaadin.flow.internal.CurrentInstance;

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
        Assert.assertFalse(
                "Unexpected resource is returned by the registration instance",
                registration.getResource() != null);
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
