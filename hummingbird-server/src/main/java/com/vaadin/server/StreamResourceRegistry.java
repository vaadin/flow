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
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
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
=======
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
>>>>>>> 025249e Review based fixes.
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for {@link StreamResource} instances.
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
 * <p>
 * This class is not thread safe. One should care about thread safety in the
 * code which uses this class explicitly.
>>>>>>> 80ab6ba... Stream resource registration on the session level.
=======
>>>>>>> 025249e Review based fixes.
 * 
 * @author Vaadin Ltd
 *
 */
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< HEAD
public class StreamResourceRegistry implements Serializable {

    private final Map<URI, StreamResource> resources = new HashMap<>();

    private final VaadinSession session;

    private int nextResourceId;
=======
class StreamResourceRegistry implements Serializable {
=======
public class StreamResourceRegistry implements Serializable {
>>>>>>> 025249e Review based fixes.

    private static final char PATH_SEPARATOR = '/';
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51

<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
    private final Map<Integer, StreamResource> resources = new HashMap<>();
>>>>>>> 80ab6ba... Stream resource registration on the session level.
=======
    private final Map<String, StreamResource> resources = new HashMap<>();
=======
>>>>>>> 025249e Review based fixes.

    /**
     * Dynamic resource URI prefix.
     */
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
    static final String DYN_RES_PREFIX = "vaadin-dynamic/generated-resources/";
>>>>>>> 542ad4a Review based fixes.
=======
    static final String DYN_RES_PREFIX = "VAADIN/dynamic/generated-resources/";

    private final Map<URI, StreamResource> resources = new HashMap<>();

    private final VaadinSession session;

    private int nextResourceId;
>>>>>>> 025249e Review based fixes.

    private static final class Registration
            implements StreamResourceRegistration {

        private final StreamResourceRegistry registry;

<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< HEAD
        private final URI uri;
=======
        private final int resourceId;

        private final String name;
>>>>>>> 80ab6ba... Stream resource registration on the session level.
=======
        private final String url;
>>>>>>> 542ad4a Review based fixes.
=======
        private final URI uri;
>>>>>>> f4adc2d Corrections.

        private Registration(StreamResourceRegistry registry, int id,
                String fileName) {
            this.registry = registry;
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< HEAD
            try {
                uri = new URI(
                        StreamResourceRequestHandler.generateURI(id, fileName));
=======
            try {
                uri = new URI(generateURI(id, fileName));
>>>>>>> f4adc2d Corrections.
            } catch (URISyntaxException e) {
                // this may not happen if implementation is correct
                throw new RuntimeException(e);
            }
        }

        @Override
        public URI getResourceUri() {
            return uri;
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
=======
            resourceId = id;
=======
>>>>>>> 542ad4a Review based fixes.
            StringBuilder resourceName = new StringBuilder(fileName);
            while (resourceName.length() > 0
                    && resourceName.charAt(0) == PATH_SEPARATOR) {
                resourceName.delete(0, 1);
            }
            String name = resourceName.toString();
            url = generateUrl(id, name);
=======
            url = generateUrl(id, fileName);
>>>>>>> df234a2 Review based fixes.
        }

        @Override
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
        public String getResourceUri() {
            // TODO : prefix should be configurable
            StringBuilder builder = new StringBuilder(
                    VaadinSession.DYN_RES_PREFIX);
            builder.append(resourceId).append(name);
            return builder.toString();
>>>>>>> 80ab6ba... Stream resource registration on the session level.
=======
        public String getResourceUrl() {
            return url;
>>>>>>> 542ad4a Review based fixes.
=======
>>>>>>> f4adc2d Corrections.
        }

        @Override
        public void unregister() {
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< HEAD
            registry.resources.remove(getResourceUri());
=======
            registry.resources.remove(resourceId);
>>>>>>> 80ab6ba... Stream resource registration on the session level.
=======
            registry.resources.remove(getResourceUrl());
>>>>>>> 542ad4a Review based fixes.
=======
            registry.resources.remove(getResourceUri());
>>>>>>> f4adc2d Corrections.
        }

        private String generateURI(int id, String name) {
            StringBuilder builder = new StringBuilder(DYN_RES_PREFIX);
            try {
                builder.append(id).append(PATH_SEPARATOR).append(
                        URLEncoder.encode(name, StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                // UTF8 has to be supported
                throw new RuntimeException(e);
            }
            return builder.toString();
        }
    }

<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
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
=======
    /**
     * Creates stream resource registry for provided {@code session}.
     * 
     * @param session
     *            vaadin session
     */
    public StreamResourceRegistry(VaadinSession session) {
        this.session = session;
    }
>>>>>>> 025249e Review based fixes.

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
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
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
=======
    StreamResource getResource(String url) {
        return resources.get(url);
>>>>>>> 542ad4a Review based fixes.
=======
    public Optional<StreamResource> getResource(String url) {
=======
    public Optional<StreamResource> getResource(URI uri) {
>>>>>>> f4adc2d Corrections.
        assert session.hasLock();
        return Optional.ofNullable(resources.get(uri));
    }

    /**
     * Get registered resource by its {@code prefix} (which has been generated
     * by framework) and file {@code name}.
     * 
     * @return registered resource if any
     */
    public Optional<StreamResource> getResource(String prefix, String name) {
        assert session.hasLock();
        try {
            if (!prefix.isEmpty()
                    && prefix.charAt(prefix.length() - 1) != PATH_SEPARATOR) {
                return Optional.empty();
            }
            URI uri = new URI(prefix
                    + URLEncoder.encode(name, StandardCharsets.UTF_8.name()));
            return Optional.ofNullable(resources.get(uri));
        } catch (UnsupportedEncodingException | URISyntaxException e) {
            // UTF8 has to be supported
            throw new RuntimeException(e);
        }
>>>>>>> 025249e Review based fixes.
    }

}
