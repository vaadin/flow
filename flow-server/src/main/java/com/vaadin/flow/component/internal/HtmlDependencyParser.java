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
package com.vaadin.flow.component.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.util.SharedUtil;

/**
 * Html import dependencies parser.
 * <p>
 * It takes the an HTML import url as a root and parse the content recursively
 * collecting html import dependencies.
 *
 * @author Vaadin Ltd
 *
 */
public class HtmlDependencyParser {

    static class HtmlDependenciesCache implements Serializable {
        private final Set<String> dependencies = new HashSet<>();

        void addDependency(String url) {
            dependencies.add(url);
        }

        boolean hasDependency(String url) {
            return dependencies.contains(url);
        }
    }

    private final String root;

    /**
     * Creates a new instance using the given {@code uri} as a root.
     *
     * @param uri
     *            HTML import uri
     */
    public HtmlDependencyParser(String uri) {
        root = uri;
    }

    Collection<String> parseDependencies() {
        Set<String> dependencies = new HashSet<>();
        String rooUri = SharedUtil.prefixIfRelative(root,
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX);
        parseDependencies(rooUri, dependencies);

        return dependencies;
    }

    private void parseDependencies(String path, Set<String> dependencies) {
        if (dependencies.contains(path)) {
            return;
        }
        dependencies.add(path);

        VaadinSession session = VaadinSession.getCurrent();
        VaadinServlet servlet = VaadinServlet.getCurrent();
        if (servlet == null || session == null) {
            /*
             * Cannot happen in runtime.
             *
             * But not all unit tests set it. Let's just don't proceed further.
             */
            return;
        }

        session.checkHasLock();
        HtmlDependenciesCache cache = session
                .getAttribute(HtmlDependenciesCache.class);
        if (cache == null) {
            cache = new HtmlDependenciesCache();
            session.setAttribute(HtmlDependenciesCache.class, cache);
        }

        String resolvedPath = servlet.resolveResource(path);

        if (cache.hasDependency(resolvedPath)) {
            return;
        }
        cache.addDependency(resolvedPath);

        try (InputStream content = servlet.getResourceAsStream(resolvedPath)) {
            if (content == null) {
                getLogger().trace(
                        "Can't find resource '{}' to parse for imports via the servlet context",
                        path);
            } else {
                parseHtmlImports(content, resolvedPath)
                        .map(uri -> resolveUri(uri, path))
                        .forEach(uri -> parseDependencies(uri, dependencies));
            }
        } catch (IOException exception) {
            // ignore exception on close()
            getLogger().debug("Couldn't close template input stream",
                    exception);
        }
    }

    private String resolveUri(String relative, String base) {
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

    private String relativize(String relative, URI base) {
        if (base.getPath().isEmpty()) {
            String uriString = base.toString();
            int index = uriString.lastIndexOf('/');
            return uriString.substring(0, index + 1) + relative;
        }
        return base.resolve(relative).toString();
    }

    private Stream<String> parseHtmlImports(InputStream content, String path) {
        assert content != null;
        try {
            Document parsedDocument = Jsoup.parse(content,
                    StandardCharsets.UTF_8.name(), "");

            return parsedDocument.getElementsByTag("link").stream()
                    .filter(link -> link.hasAttr("rel") && link.hasAttr("href"))
                    .filter(link -> link.attr("rel").equals("import"))
                    .map(link -> link.attr("href"));
        } catch (IOException exception) {
            getLogger().info(
                    "Can't parse the template declared using '{}' path", path,
                    exception);
        }
        return Stream.empty();
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(HtmlDependencyParser.class);
    }
}
