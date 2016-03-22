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
<<<<<<< HEAD
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.vaadin.server.communication.StreamResourceRequestHandler;

/**
 * Registry for {@link StreamResource} instances.
=======
import java.util.HashMap;
import java.util.Map;

/**
 * This class is not thread safe. One should care about thread safety in the
 * code which uses this class explicitly.
>>>>>>> 80ab6ba... Stream resource registration on the session level.
 * 
 * @author Vaadin Ltd
 *
 */
<<<<<<< HEAD
public class StreamResourceRegistry implements Serializable {

    private final Map<URI, StreamResource> resources = new HashMap<>();

    private final VaadinSession session;

    private int nextResourceId;
=======
class StreamResourceRegistry implements Serializable {

    private static final char PATH_SEPARATOR = '/';

    private final Map<Integer, StreamResource> resources = new HashMap<>();
>>>>>>> 80ab6ba... Stream resource registration on the session level.

    private static final class Registration
            implements StreamResourceRegistration {

        private final StreamResourceRegistry registry;

<<<<<<< HEAD
        private final URI uri;
=======
        private final int resourceId;

        private final String name;
>>>>>>> 80ab6ba... Stream resource registration on the session level.

        private Registration(StreamResourceRegistry registry, int id,
                String fileName) {
            this.registry = registry;
<<<<<<< HEAD
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
=======
            resourceId = id;
            StringBuilder resourceName = new StringBuilder(fileName);
            while (resourceName.length() > 0
                    && resourceName.charAt(0) == PATH_SEPARATOR) {
                resourceName.delete(0, 1);
            }
            resourceName.insert(0, PATH_SEPARATOR);
            name = resourceName.toString();
        }

        @Override
        public String getResourceUri() {
            // TODO : prefix should be configurable
            StringBuilder builder = new StringBuilder(
                    VaadinSession.DYN_RES_PREFIX);
            builder.append(resourceId).append(name);
            return builder.toString();
>>>>>>> 80ab6ba... Stream resource registration on the session level.
        }

        @Override
        public void unregister() {
<<<<<<< HEAD
            registry.resources.remove(getResourceUri());
=======
            registry.resources.remove(resourceId);
>>>>>>> 80ab6ba... Stream resource registration on the session level.
        }

    }

<<<<<<< HEAD
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
     * @return registered resource if any
     */
    public Optional<StreamResource> getResource(URI uri) {
        assert session.hasLock();
        return Optional.ofNullable(resources.get(uri));
=======
    private int nextResourceId;

    /**
     * Register the {@code resource}.
     * 
     * @param resource
     *            resource to register
     * @return registration handler
     */
    StreamResourceRegistration registerResource(StreamResource resource) {
        int id = nextResourceId;
        resources.put(id, resource);
        return new Registration(this, id, resource.getFileName());
    }

    /**
     * Get registered resource by its {@code uri}.
     * 
     * @param uri
     *            resource uri
     * @return registered resource if any
     */
    StreamResource getResource(String uri) {
        if (uri.startsWith(VaadinSession.DYN_RES_PREFIX)) {
            String postfix = uri
                    .substring(VaadinSession.DYN_RES_PREFIX.length());
            int index = postfix.indexOf(PATH_SEPARATOR);
            if (index >= 0) {
                String id = postfix.substring(0, index);
                try {
                    int resId = Integer.parseInt(id);
                    return resources.get(resId);
                } catch (NumberFormatException ignore) {
                    // ignore the exception. URI is not dyn resource URI
                }
            }
        }
        return null;
>>>>>>> 80ab6ba... Stream resource registration on the session level.
    }

}
