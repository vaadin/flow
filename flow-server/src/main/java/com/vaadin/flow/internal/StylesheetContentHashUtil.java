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
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinService;

/**
 * Computes and caches content-based hashes for stylesheet resources, enabling
 * cache-busting by appending {@code ?v=<hash>} to stylesheet URLs. The hash
 * changes only when file content changes, allowing aggressive browser caching.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
public class StylesheetContentHashUtil {

    private static final int HASH_LENGTH = 8;

    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory
            .getLogger(StylesheetContentHashUtil.class);

    private StylesheetContentHashUtil() {
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
        try (InputStream stream = service.getResourceAsStream(resourceUrl)) {
            if (stream == null) {
                logger.debug("Resource not found for content hashing: {}",
                        resourceUrl);
                return null;
            }
            byte[] bytes = stream.readAllBytes();
            String fullHash = MessageDigestUtil.sha256Hex(bytes);
            return fullHash.substring(0, HASH_LENGTH);
        } catch (IOException e) {
            logger.debug("Failed to read resource for content hashing: {}",
                    resourceUrl, e);
            return null;
        }
    }

    /**
     * Appends a version query parameter to the given URL using the provided
     * hash. Returns the original URL unchanged if the hash is {@code null}.
     *
     * @param url
     *            the original URL
     * @param hash
     *            the content hash, or {@code null}
     * @return the URL with {@code ?v=<hash>} or {@code &v=<hash>} appended, or
     *         the original URL if hash is {@code null}
     */
    public static String appendHashToUrl(String url, String hash) {
        if (hash == null) {
            return url;
        }
        char separator = url.contains("?") ? '&' : '?';
        return url + separator + "v=" + hash;
    }
}
