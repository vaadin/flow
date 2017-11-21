/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.ui.UI;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class StreamResourceRegistryTest {

    private VaadinServlet servlet = new VaadinServlet();
    private VaadinServletService service;
    private VaadinSession session;

    @Before
    public void setUp() throws ServletException {
        UI ui = Mockito.mock(UI.class);
        UI.setCurrent(ui);
        Mockito.when(ui.getUIId()).thenReturn(1);
        service = servlet.getService();
        session = new VaadinSession(service) {

            @Override
            public boolean hasLock() {
                return true;
            }
        };
    }

    @Test
    public void registerResource_registrationResultCanBeFound() {
        StreamResourceRegistry registry = session.getResourceRegistry();

        StreamResource resource = new StreamResource("name",
                () -> makeEmptyStream());
        StreamRegistration registration = registry.registerResource(resource);
        assertNotNull(registration);

        URI uri = registration.getResourceUri();

        Optional<StreamResource> stored = registry
                .getResource(StreamResource.class, uri);
        assertSame(
                "Unexpected stored resource is returned for registered URI",
                resource, stored.get());

        assertSame(
                "Unexpected resource is returned by the registration instance",
                resource, registration.getResource());
    }

    @Test
    public void unregisterResource_resourceIsRemoved() {
        StreamResourceRegistry registry = session.getResourceRegistry();

        StreamResource resource = new StreamResource("name",
                () -> makeEmptyStream());
        StreamRegistration registration = registry.registerResource(resource);
        assertNotNull(registration);

        URI uri = registration.getResourceUri();

        registration.unregister();

        Optional<StreamResource> stored = registry
                .getResource(StreamResource.class, uri);
        assertFalse(
                "Unexpected stored resource is found after unregister()",
                stored.isPresent());
        assertFalse(
                "Unexpected resource is returned by the registration instance",
                registration.getResource() != null);
    }

    @Test
    public void registerTwoResourcesWithSameName_resourcesHasDifferentURI() {
        StreamResourceRegistry registry = session.getResourceRegistry();

        StreamResource resource1 = new StreamResource("name",
                () -> makeEmptyStream());
        StreamRegistration registration1 = registry.registerResource(resource1);

        StreamResource resource2 = new StreamResource("name",
                () -> makeEmptyStream());
        StreamRegistration registration2 = registry.registerResource(resource2);

        assertNotEquals(
                "Two different resource are registered to the same URI",
                registration1.getResourceUri(), registration2.getResourceUri());

        registration1.unregister();

        assertTrue(
                "Second resource is not found after first resource has been unregistered",
                registry.getResource(registration2.getResourceUri())
                        .isPresent());
    }

    @Test
    public void getResourceUrlIsEncoded() throws UnsupportedEncodingException {
        StreamResourceRegistry registry = session.getResourceRegistry();

        StreamResource resource = new StreamResource("a?b=c d&e",
                () -> makeEmptyStream());
        StreamRegistration registration = registry.registerResource(resource);

        URI url = registration.getResourceUri();
        String suffix = URLEncoder.encode(resource.getName(),
                StandardCharsets.UTF_8.name());
        assertTrue("Resource url is not encoded",
                url.toString().endsWith(suffix));
    }

    private InputStream makeEmptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }
}
