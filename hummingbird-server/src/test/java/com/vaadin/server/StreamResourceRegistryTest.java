/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vaadin Ltd
 *
 */
public class StreamResourceRegistryTest {

    @Test
    public void registerResource_registrationResultContainsExpectedUri() {
        StreamResourceRegistry registry = new StreamResourceRegistry();

        StreamResource resource = new StreamResource("name",
                () -> makeEmptyStream());
        StreamResourceRegistration registration = registry
                .registerResource(resource);
        Assert.assertNotNull(registration);

        String uri = registration.getResourceUri();
        Assert.assertTrue("Unexpected URI prefix",
                uri.startsWith(VaadinSession.DYN_RES_PREFIX));

        StreamResource stored = registry.getResource(uri);
        Assert.assertSame(
                "Unexpected stored resource is returned for registered URI",
                stored, resource);
    }

    @Test
    public void unregisterResource_resourceIsRemoved() {
        StreamResourceRegistry registry = new StreamResourceRegistry();

        StreamResource resource = new StreamResource("name",
                () -> makeEmptyStream());
        StreamResourceRegistration registration = registry
                .registerResource(resource);
        Assert.assertNotNull(registration);

        String uri = registration.getResourceUri();

        registration.unregister();

        StreamResource stored = registry.getResource(uri);
        Assert.assertNull(
                "Unexpected stored resource is found after unregister()",
                stored);
    }

    @Test
    public void registerTwoResourcesWithSameName_resourcesHasDifferentURI() {
        StreamResourceRegistry registry = new StreamResourceRegistry();

        StreamResource resource1 = new StreamResource("name",
                () -> makeEmptyStream());
        StreamResourceRegistration registration1 = registry
                .registerResource(resource1);

        StreamResource resource2 = new StreamResource("name",
                () -> makeEmptyStream());
        StreamResourceRegistration registration2 = registry
                .registerResource(resource2);

        Assert.assertNotEquals(
                "Two different resource are registered to the same URI",
                registration1.getResourceUri(), registration2.getResourceUri());

        registration1.unregister();

        assertNotNull(
                "Second resource is not found after first resource has been unregistered",
                registry.getResource(registration2.getResourceUri()));
    }

    private InputStream makeEmptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }
}
