/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinService;

/**
 * Computes and caches content-based hashes for static resources, enabling
 * cache-busting by appending a version query parameter to resource URLs. The
 * hash changes only when file content changes, allowing aggressive browser
 * caching.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
public class ResourceContentHash {

    private static final int HASH_LENGTH = 8;

    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory
            .getLogger(ResourceContentHash.class);

    private ResourceContentHash() {
    }

    /**
     * Computes a truncated SHA-256 content hash for the given resource URL.
     * Returns {@code null} for external URLs (http/https), missing resources,
     * or on any I/O error. Results are cached so that each resource is read at
     * most once.
     *
     * @param service
     *            the Vaadin service used to load the resource
     * @param resourceUrl
     *            the resource path to hash
     * @return an 8-character hex hash string, or {@code null} if the hash
     *         cannot be computed
     */
    public static String getContentHash(VaadinService service,
            String resourceUrl) {
        if (resourceUrl == null || resourceUrl.isBlank()) {
            return null;
        }
        String lower = resourceUrl.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return null;
        }
        return cache.computeIfAbsent(resourceUrl,
                url -> computeHash(service, url));
    }

    private static String computeHash(VaadinService service,
            String resourceUrl) {
        try (InputStream stream = openResource(service, resourceUrl)) {
            if (stream == null) {
                return null;
            }
            byte[] bytes = stream.readAllBytes();
            String fullHash = MessageDigestUtil.sha256Hex(bytes);
            logger.debug(
                    "Computed cache-busting hash for '{}': v={} ({} bytes)",
                    resourceUrl, fullHash.substring(0, HASH_LENGTH),
                    bytes.length);
            return fullHash.substring(0, HASH_LENGTH);
        } catch (IOException e) {
            logger.debug(
                    "Failed to read resource for content hashing: '{}' "
                            + "(service class: {})",
                    resourceUrl, service.getClass().getName(), e);
            return null;
        }
    }

    private static InputStream openResource(VaadinService service,
            String resourceUrl) {
        String resolved = service.resolveResource(resourceUrl);
        if (!resolved.startsWith("/") && !resolved.contains("://")) {
            if (resolved.startsWith("./")) {
                resolved = resolved.substring(1);
            } else {
                resolved = "/" + resolved;
            }
        }
        URL url = service.getStaticResource(resolved);
        if (url == null) {
            logger.debug(
                    "Could not find static resource for '{}' "
                            + "(resolved: '{}', service: {})",
                    resourceUrl, resolved, service.getClass().getName());
            return null;
        }
        try {
            return url.openStream();
        } catch (IOException e) {
            logger.debug("Failed to open stream for '{}'", resourceUrl, e);
            return null;
        }
    }

}
