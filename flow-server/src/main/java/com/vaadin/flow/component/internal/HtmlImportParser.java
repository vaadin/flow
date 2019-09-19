/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for finding the HTML imports of a resource.
 *
 * @author Vaadin Ltd
 * @since 1.2
 */
public class HtmlImportParser {

    private HtmlImportParser() {
        // Static helpers only
    }

    private static String resolveUri(String relative, String base) {
        if (relative.startsWith("/")) {
            return relative;
        }
        try {
            URI uri = new URI(base);
            return relativize(relative, uri);
        } catch (URISyntaxException exception) {
            getLogger().debug(
                    "Couldn't make URI for {}. The path {} will be used as is.",
                    base, relative, exception);
        }
        return relative;
    }

    private static String relativize(String relative, URI base)
            throws URISyntaxException {
        URI newUri;
        if (base.getPath().isEmpty()) {
            String uriString = base.toString();
            int index = uriString.lastIndexOf('/');
            newUri = new URI(uriString.substring(0, index + 1) + relative);
        } else {
            newUri = base.resolve(relative);
        }

        return toNormalizedURI(newUri);
    }

    /**
     * Returns a normalized version of the URI, converted to a string.
     *
     * @param uri
     *            the URI to normalize
     * @return a nonrmalized version of the URI
     */
    // Package private for testing purposes
    static String toNormalizedURI(URI uri) {
        URI normalized = uri.normalize();
        // This is because of https://github.com/vaadin/flow/issues/3892
        if ("frontend".equals(normalized.getScheme())
                || "base".equals(normalized.getScheme())
                || "context".equals(normalized.getScheme())) {
            if (".".equals(normalized.getAuthority())
                    && normalized.getHost() == null) {
                // frontend://./foo.html
                return normalized.toString().replace("//./", "//");
            }
            if (normalized.getPath().startsWith("/../")) {
                return normalized.toString()
                        .replace(normalized.getHost() + "/../", "");
            }
        }
        return normalized.toString();
    }

    private static Stream<String> parseHtmlImports(InputStream content,
            String path) {
        assert content != null;
        try {
            Document parsedDocument = Jsoup.parse(content,
                    StandardCharsets.UTF_8.name(), "");

            return parsedDocument.getElementsByTag("link").stream()
                    .filter(link -> link.hasAttr("rel") && link.hasAttr("href"))
                    .filter(link -> "import".equals(link.attr("rel")))
                    .map(link -> link.attr("href"));
        } catch (IOException exception) {
            getLogger().info(
                    "Can't parse the template declared using '{}' path", path,
                    exception);
        }
        return Stream.empty();
    }

    /**
     * Parses the contents of the given resource and passes any found HTML
     * imports to the given consumer.
     *
     * @param resourcePath
     *            the path of the resource from which to fin
     * @param getResourceAsStream
     *            a callback for opening an input stream with the contents of
     *            the given resource
     * @param resolveResource
     *            a callback that resolves the given resource path
     * @param importHandler
     *            the callback to which found HTML imports should be passed
     */
    public static void parseImports(String resourcePath,
            Function<String, InputStream> getResourceAsStream,
            Function<String, String> resolveResource,
            Consumer<String> importHandler) {
        try (InputStream content = getResourceAsStream.apply(resourcePath)) {
            if (content == null) {
                getLogger().trace(
                        "Can't find resource '{}' to parse for imports via the servlet context",
                        resourcePath);
            } else {
                String resolvedPath = resolveResource.apply(resourcePath);
                parseHtmlImports(content, resolvedPath)
                        .map(uri -> resolveUri(uri, resourcePath))
                        .forEach(importHandler);
            }
        } catch (IOException exception) {
            // ignore exception on close()
            getLogger().debug("Couldn't close template input stream",
                    exception);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(HtmlImportParser.class);
    }
}
