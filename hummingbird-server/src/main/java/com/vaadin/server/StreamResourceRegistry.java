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
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for {@link StreamResource} instances.
 * <p>
 * This class is not thread safe. One should care about thread safety in the
 * code which uses this class explicitly.
 * 
 * @author Vaadin Ltd
 *
 */
class StreamResourceRegistry implements Serializable {

    private static final char PATH_SEPARATOR = '/';

    private final Map<String, StreamResource> resources = new HashMap<>();

    /**
     * Dynamic resource URI prefix.
     */
    static final String DYN_RES_PREFIX = "vaadin-dynamic/generated-resources/";

    private static final class Registration
            implements StreamResourceRegistration {

        private final StreamResourceRegistry registry;

        private final String url;

        private Registration(StreamResourceRegistry registry, int id,
                String fileName) {
            this.registry = registry;
            url = generateUrl(id, fileName);
        }

        @Override
        public String getResourceUrl() {
            return url;
        }

        @Override
        public void unregister() {
            registry.resources.remove(getResourceUrl());
        }

        private String generateUrl(int id, String name) {
            StringBuilder builder = new StringBuilder(DYN_RES_PREFIX);
            builder.append(id).append(PATH_SEPARATOR).append(name);
            return builder.toString();
        }
    }

    private int nextResourceId;

    /**
     * Registers the {@code resource}.
     * 
     * @param resource
     *            resource to register
     * @return registration handler
     */
    StreamResourceRegistration registerResource(StreamResource resource) {
        int id = nextResourceId;
        nextResourceId++;
        Registration registration = new Registration(this, id,
                resource.getFileName());
        resources.put(registration.getResourceUrl(), resource);
        return registration;
    }

    /**
     * Get registered resource by its {@code url}.
     * 
     * @param uri
     *            resource url
     * @return registered resource if any
     */
    StreamResource getResource(String url) {
        return resources.get(url);
    }

}
