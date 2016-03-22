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
 * This class is not thread safe. One should care about thread safety in the
 * code which uses this class explicitly.
 * 
 * @author Vaadin Ltd
 *
 */
class StreamResourceRegistry implements Serializable {

    private static final char PATH_SEPARATOR = '/';

    private final Map<Integer, StreamResource> resources = new HashMap<>();

    private static final class Registration
            implements StreamResourceRegistration {

        private final StreamResourceRegistry registry;

        private final int resourceId;

        private final String name;

        private Registration(StreamResourceRegistry registry, int id,
                String fileName) {
            this.registry = registry;
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
        }

        @Override
        public void unregister() {
            registry.resources.remove(resourceId);
        }

    }

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
    }

}
