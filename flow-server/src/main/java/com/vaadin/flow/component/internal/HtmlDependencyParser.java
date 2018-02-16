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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.servlet.ServletContext;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinUriResolverFactory;
import com.vaadin.flow.server.WrappedHttpSession;

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

    private final String root;

    /**
     * Creates a new instance using the given {@code uri} as a root.
     *
     * @param uri
     *            HTML import uri
     */
    public HtmlDependencyParser(String uri) {
        this.root = uri;
    }

    Collection<String> parseDependencies() {
        Set<String> dependencies = new HashSet<>();

        parseDependencies(root, dependencies);

        return dependencies;
    }

    private void parseDependencies(String path, Set<String> dependencies) {
        if (dependencies.contains(path)) {
            return;
        }
        dependencies.add(path);

        VaadinRequest request = VaadinService.getCurrentRequest();
        VaadinSession session = VaadinSession.getCurrent();
        if (request == null || session == null
                || request.getWrappedSession() == null) {
            /*
             * Cannot happen in runtime.
             *
             * But not all unit tests set it. Let's just don't proceed further.
             */
            return;
        }
        VaadinUriResolverFactory factory = session
                .getAttribute(VaadinUriResolverFactory.class);
        assert factory != null;
        ServletContext context = ((WrappedHttpSession) request
                .getWrappedSession()).getHttpSession().getServletContext();

        String resolvedUri = factory.toServletContextPath(request, path);

        try (InputStream content = context.getResourceAsStream(resolvedUri)) {
            if (content == null) {
                getLogger().info(
                        "Can't find resource '%s' via the servlet context",
                        path);
            } else {
                parseHtmlImports(content, path)
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
            return uri.resolve(relative).toString();
        } catch (URISyntaxException exception) {
            getLogger().debug(
                    "Couldn't make URI for {}. The path {} will be used as is.",
                    base, relative);
        }
        return relative;
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
                    "Can't parse the template declared using '%s' path", path,
                    exception);
        }
        return Stream.empty();
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(HtmlDependencyParser.class);
    }
}
