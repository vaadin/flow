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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.vaadin.server.communication.StreamResourceRequestHandler;

/**
 * Registry for {@link StreamResource} instances.
 *
 * @author Vaadin Ltd
 *
 */
public class StreamResourceRegistry implements Serializable {

    private final Map<URI, StreamResource> resources = new HashMap<>();

    private final VaadinSession session;

    private int nextResourceId;

    private static final class Registration
            implements StreamResourceRegistration {

        private final StreamResourceRegistry registry;

        private final URI uri;

        private Registration(StreamResourceRegistry registry, int id,
                String fileName) {
            this.registry = registry;
            try {
                uri = new URI(
                        StreamResourceRequestHandler.generateURI(id, fileName));
            } catch (URISyntaxException e) {
                // this may not happen if implementation is correct
                throw new RuntimeException(e);
            }
        }

        @Override
        public URI getResourceUri() {
            return uri;
        }

        @Override
        public void unregister() {
            registry.resources.remove(getResourceUri());
        }

    }

    /**
     * Creates stream resource registry for provided {@code session}.
     *
     * @param session
     *            vaadin session
     */
    public StreamResourceRegistry(VaadinSession session) {
        this.session = session;
    }

    /**
     * Registers a stream resource in the session and returns registration
     * handler.
     * <p>
     * You can get resource URI to use it in the application (e.g. set an
     * attribute value or property value) via the registration handler. The
     * registration handler should be used to unregister resource when it's not
     * needed anymore. Note that it is the developer's responsibility to
     * unregister resources. Otherwise resources won't be garbage collected
     * until the session expires which causes memory leak.
     *
     * @param resource
     *            stream resource to register
     * @return registration handler.
     */
    public StreamResourceRegistration registerResource(
            StreamResource resource) {
        assert session.hasLock();
        int id = nextResourceId;
        nextResourceId++;
        Registration registration = new Registration(this, id,
                resource.getFileName());
        resources.put(registration.getResourceUri(), resource);
        return registration;
    }

    /**
     * Get registered resource by its {@code URI}.
     *
     * @param uri
     *            resource URI
     * @return an optional resource, or an empty optional if no resource has
     *         been registered with this URI
     */
    public Optional<StreamResource> getResource(URI uri) {
        assert session.hasLock();
        return Optional.ofNullable(resources.get(uri));
    }

}
