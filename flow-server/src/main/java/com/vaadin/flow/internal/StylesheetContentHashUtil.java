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
        try (InputStream stream = openResource(service, resourceUrl)) {
            if (stream == null) {
                logger.debug(
                        "Could not compute cache-busting hash for '{}': "
                                + "resource not found. Tried via {} "
                                + "(service class: {}). The stylesheet URL "
                                + "will not have a ?v=<hash> parameter.",
                        resourceUrl, describeAttempts(resourceUrl),
                        service.getClass().getName());
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

    private static String describeAttempts(String resourceUrl) {
        if (resourceUrl.startsWith("/") || resourceUrl.contains("://")) {
            return "getResourceAsStream(\"" + resourceUrl + "\")";
        }
        return "getResourceAsStream(\"" + resourceUrl
                + "\") and getResourceAsStream(\"/" + resourceUrl + "\")";
    }

    /**
     * Spring Boot static resource prefixes to search on the classpath when
     * {@code servletContext.getResourceAsStream()} fails. This covers the
     * standard locations where Spring Boot serves static content from.
     */
    private static final String[] CLASSPATH_PREFIXES = { "META-INF/resources/",
            "static/", "public/", "resources/" };

    private static InputStream openResource(VaadinService service,
            String resourceUrl) {
        String resolved = service.resolveResource(resourceUrl);
        InputStream stream = service.getResourceAsStream(resourceUrl);
        if (stream != null) {
            logger.debug("Resolved '{}' -> '{}' (found)", resourceUrl,
                    resolved);
            return stream;
        }
        logger.debug("Resolved '{}' -> '{}' (not found)", resourceUrl,
                resolved);
        // Bare paths (e.g. "styles.css") may not resolve in the servlet
        // context which requires a leading '/'. Try with '/' prefix as
        // fallback.
        if (!resourceUrl.startsWith("/") && !resourceUrl.contains("://")) {
            String withSlash = "/" + resourceUrl;
            stream = service.getResourceAsStream(withSlash);
            if (stream != null) {
                logger.debug("Resolved '{}' (found via '/' prefix fallback)",
                        resourceUrl);
                return stream;
            }
        }
        // servletContext.getResourceAsStream() does not find resources in
        // Spring Boot's static resource locations (static/, public/, etc.)
        // when running from a packaged jar. Fall back to classpath lookup.
        stream = openResourceFromClasspath(resourceUrl);
        return stream;
    }

    private static InputStream openResourceFromClasspath(String resourceUrl) {
        String path = resourceUrl;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.contains("://")) {
            // Strip context:// or similar protocol prefixes
            path = path.substring(path.indexOf("://") + 3);
        }
        ClassLoader classLoader = Thread.currentThread()
                .getContextClassLoader();
        if (classLoader == null) {
            classLoader = StylesheetContentHashUtil.class.getClassLoader();
        }
        for (String prefix : CLASSPATH_PREFIXES) {
            String classpathPath = prefix + path;
            InputStream stream = classLoader.getResourceAsStream(classpathPath);
            if (stream != null) {
                logger.debug("Resolved '{}' via classpath '{}'", resourceUrl,
                        classpathPath);
                return stream;
            }
        }
        logger.debug("'{}' not found on classpath (tried prefixes: {})",
                resourceUrl, String.join(", ", CLASSPATH_PREFIXES));
        return null;
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
