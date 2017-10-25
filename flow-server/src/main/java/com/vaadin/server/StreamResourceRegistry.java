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

import com.vaadin.server.communication.StreamRequestHandler;

/**
 * Registry for {@link StreamResource} instances.
 *
 * @author Vaadin Ltd
 */
public class StreamResourceRegistry implements Serializable {

    private final Map<URI, AbstractStreamResource> res = new HashMap<>();

    private final VaadinSession session;

    private static final class Registration implements StreamRegistration {

        private final StreamResourceRegistry registry;

        private final URI uri;

        private Registration(StreamResourceRegistry registry, String id,
                String fileName) {
            this.registry = registry;
            uri = getURI(fileName, id);
        }

        @Override
        public URI getResourceUri() {
            return uri;
        }

        @Override
        public void unregister() {
            registry.res.remove(getResourceUri());
        }

        @Override
        public StreamResource getResource() {
            Optional<StreamResource> resource = registry
                    .getStreamResource(getResourceUri());
            return resource.isPresent() ? resource.get() : null;
        }
    }

    private static final class ReceiverRegistration
            implements StreamRegistration {

        private final StreamResourceRegistry registry;

        private final URI uri;

        private ReceiverRegistration(StreamResourceRegistry registry, String id,
                String attributeName) {
            this.registry = registry;
            uri = getURI(attributeName, id);
        }

        @Override
        public URI getResourceUri() {
            return uri;
        }

        @Override
        public void unregister() {
            registry.res.remove(getResourceUri());
        }

        @Override
        public StreamReceiver getResource() {
            Optional<StreamReceiver> receiver = registry
                    .getStreamReceiver(getResourceUri());
            return receiver.isPresent() ? receiver.get() : null;
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
     * @return registration handler
     */
    public StreamRegistration registerResource(
            AbstractStreamResource resource) {
        assert session
                .hasLock() : "Session needs to be locked when registering stream resources.";
        StreamRegistration registration = new Registration(this,
                resource.getId(), resource.getName());
        res.put(registration.getResourceUri(), resource);
        return registration;
    }

    /**
     * Gets the URI for the given {@code resource}.
     * <p>
     * The URI won't be handled (and won't work) if {@code resource} is not
     * registered in the session.
     *
     * @see #registerResource(AbstractStreamResource)
     *
     * @param resource
     *            stream resource
     * @return resource URI
     */
    public static URI getURI(AbstractStreamResource resource) {
        return getURI(resource.getName(), resource.getId());
    }

    private static URI getURI(String name, String id) {
        try {
            return new URI(StreamRequestHandler.generateURI(name, id));
        } catch (URISyntaxException e) {
            // this may not happen if implementation is correct
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a registered resource for given {@code URI}.
     * <p>
     * Resource may be a StreamResource or a StreamReceiver
     *
     * @param uri
     *            resource URI
     * @return an optional resource, or an empty optional if no resource has
     *         been registered with this URI
     */
    public Optional<AbstractStreamResource> getResource(URI uri) {
        assert session.hasLock();
        return Optional.ofNullable(res.get(uri));
    }

    /**
     * Get a registered stream resource by its {@code URI}.
     *
     * @param uri
     *            resource URI
     * @return an optional resource, or an empty optional if no resource has
     *         been registered with this URI
     */
    public Optional<StreamResource> getStreamResource(URI uri) {
        assert session.hasLock();
        AbstractStreamResource abstractStreamResource = res.get(uri);
        if (abstractStreamResource instanceof StreamResource)
            return Optional.of((StreamResource) abstractStreamResource);
        return Optional.empty();
    }

    /**
     * Get a registered stream receiver resource by its {@code URI}.
     *
     * @param uri
     *            resource URI
     * @return an optional resource, or an empty optional if no resource has
     *         been registered with this URI
     */
    public Optional<StreamReceiver> getStreamReceiver(URI uri) {
        assert session.hasLock();
        AbstractStreamResource abstractStreamResource = res.get(uri);
        if (abstractStreamResource instanceof StreamReceiver)
            return Optional.of((StreamReceiver) abstractStreamResource);
        return Optional.empty();
    }
}
