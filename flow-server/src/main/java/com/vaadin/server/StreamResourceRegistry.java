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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.vaadin.server.communication.StreamReceiverRequestHandler;
import com.vaadin.server.communication.StreamResourceRequestHandler;

/**
 * Registry for {@link StreamResource} instances.
 *
 * @author Vaadin Ltd
 *
 */
public class StreamResourceRegistry implements Serializable {

    private final Map<URI, StreamResource> resources = new HashMap<>();
    private final Map<URI, StreamReceiver> receivers = new HashMap<>();

    private final VaadinSession session;

    private static final class Registration implements StreamRegistration {

        private final StreamResourceRegistry registry;

        private final URI uri;

        private Registration(StreamResourceRegistry registry, String id,
                String fileName) {
            this.registry = registry;
            uri = getURI(id, fileName);
        }

        @Override
        public URI getResourceUri() {
            return uri;
        }

        @Override
        public void unregister() {
            registry.resources.remove(getResourceUri());
        }

        @Override
        public StreamResource getResource() {
            Optional<StreamResource> resource = registry
                    .getResource(getResourceUri());
            return resource.isPresent() ? resource.get() : null;
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

    public StreamRegistration registerResource(
            AbstractStreamResource resource) {
        StreamRegistration registration = null;
        if (resource instanceof StreamResource) {
            registration = registerResource((StreamResource) resource);
        } else if (resource instanceof StreamReceiver) {
            registration = registerResource((StreamReceiver) resource);
        }
        return registration;
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
    public StreamRegistration registerResource(StreamResource resource) {
        assert session.hasLock();
        Registration registration = new Registration(this, resource.getId(),
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

    public static URI getURI(AbstractStreamResource resource) {
        URI uri = null;
        if (resource instanceof StreamResource) {
            uri = getURI((StreamResource) resource);
        } else if (resource instanceof StreamReceiver) {
            uri = getURI((StreamReceiver) resource);
        }
        return uri;
    }

    /**
     * Gets the URI for the given {@code resource}.
     * <p>
     * The URI won't be handled (and won't work) if {@code resource} is not
     * registered in the session.
     *
     * @see #registerResource(StreamResource)
     *
     * @param resource
     *            stream resource
     * @return resource URI
     */
    public static URI getURI(StreamResource resource) {
        return getURI(resource.getId(), resource.getFileName());
    }

    private static URI getURI(String id, String fileName) {
        try {
            return new URI(
                    StreamResourceRequestHandler.generateURI(id, fileName));
        } catch (URISyntaxException e) {
            // this may not happen if implementation is correct
            throw new RuntimeException(e);
        }
    }

    private static final class ReceiverRegistration
            implements StreamRegistration {

        private final StreamResourceRegistry registry;

        private final URI uri;

        private ReceiverRegistration(StreamResourceRegistry registry, String id,
                int nodeId, String attributeName) {
            this.registry = registry;
            uri = getURI(nodeId, id, attributeName);
        }

        @Override
        public URI getResourceUri() {
            return uri;
        }

        @Override
        public void unregister() {
            registry.receivers.remove(getResourceUri());
        }

        @Override
        public StreamReceiver getResource() {
            Optional<StreamReceiver> receiver = registry
                    .getReceiver(getResourceUri());
            return receiver.isPresent() ? receiver.get() : null;
        }
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
    public StreamRegistration registerResource(StreamReceiver resource) {
        assert session.hasLock();
        ReceiverRegistration registration = new ReceiverRegistration(this,
                resource.getId(), resource.getNode().getId(),
                resource.getAttributeName());
        receivers.put(registration.getResourceUri(), resource);
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
    public Optional<StreamReceiver> getReceiver(URI uri) {
        assert session.hasLock();
        return Optional.ofNullable(receivers.get(uri));
    }

    /**
     * Gets the URI for the given {@code resource}.
     * <p>
     * The URI won't be handled (and won't work) if {@code resource} is not
     * registered in the session.
     *
     * @see #registerResource(StreamReceiver)
     *
     * @param resource
     *            stream resource
     * @return resource URI
     */
    public static URI getURI(StreamReceiver resource) {
        return getURI(resource.getNode().getId(), resource.getId(),
                resource.getAttributeName());
    }

    private static URI getURI(int nodeId, String id, String attributeName) {
        try {
            return new URI(StreamReceiverRequestHandler.generateURI(nodeId,
                    attributeName, id));
        } catch (URISyntaxException e) {
            // this may not happen if implementation is correct
            throw new RuntimeException(e);
        }
    }
}
