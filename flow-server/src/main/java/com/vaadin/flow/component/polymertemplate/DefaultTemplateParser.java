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
package com.vaadin.flow.component.polymertemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinUriResolverFactory;
import com.vaadin.flow.server.WrappedHttpSession;
import com.vaadin.flow.server.DependencyFilter.FilterContext;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.Dependency.Type;
import com.vaadin.flow.util.AnnotationReader;
import com.vaadin.flow.util.ReflectionCache;

/**
 * Default template parser implementation.
 * <p>
 * The implementation scans all HTML imports annotations for the given template
 * class and tries to find the one that contains template definition using the
 * tag name.
 *
 * @see TemplateParser
 *
 * @author Vaadin Ltd
 *
 */
public class DefaultTemplateParser implements TemplateParser {
    private static final ReflectionCache<PolymerTemplate<?>, AtomicBoolean> LOG_CACHE = new ReflectionCache<>(
            clazz -> new AtomicBoolean());

    @Override
    public Element getTemplateContent(Class<? extends PolymerTemplate<?>> clazz,
            String tag) {
        VaadinRequest request = VaadinService.getCurrentRequest();
        WrappedHttpSession session = (WrappedHttpSession) request
                .getWrappedSession();
        assert session != null;

        ServletContext context = session.getHttpSession().getServletContext();

        boolean logEnabled = LOG_CACHE.get(clazz).compareAndSet(false, true);

        List<Dependency> dependencies = AnnotationReader
                .getAnnotationsFor(clazz, HtmlImport.class).stream()
                .map(htmlImport -> new Dependency(Type.HTML_IMPORT,
                        htmlImport.value(), htmlImport.loadMode()))
                .collect(Collectors.toList());

        FilterContext filterContext = new FilterContext(
                VaadinSession.getCurrent());
        for (DependencyFilter filter : VaadinService.getCurrent()
                .getDependencyFilters()) {
            dependencies = filter.filter(new ArrayList<>(dependencies),
                    filterContext);
        }

        for (Dependency dependency : dependencies) {
            if (dependency.getType() != Type.HTML_IMPORT) {
                continue;
            }

            String url = dependency.getUrl();
            String path = resolvePath(request, url);

            if (logEnabled) {
                getLogger().info("Html import path '{}' is resolved to '{}'", url, path);
            }
            try (InputStream content = context.getResourceAsStream(path)) {
                if (content == null) {
                    throw new IllegalStateException(
                            String.format("Can't find resource '%s' "
                                    + "via the servlet context", url));
                }
                Element templateElement = parseHtmlImport(content, url, tag);
                if (logEnabled && templateElement != null) {
                    getLogger().info("Found a template file containing template "
                        + "definition for the tag '{}' by the path '{}'", tag, url);
                }

                if (templateElement != null) {
                    return templateElement;

                }
            } catch (IOException exception) {
                // ignore exception on close()
                if (logEnabled) {
                    getLogger().warn("Couldn't close template input stream", exception);
                }
            }
        }
        throw new IllegalStateException(String.format("Couldn't find the "
                + "definition of the element with tag '%s' "
                + "in any template file declared using @'%s' annotations. "
                + "Check the availability of the template files in your WAR "
                + "file or provide alternative implementation of the "
                + "method getTemplateContent() which should return an element "
                + "representing the content of the template file", tag,
                HtmlImport.class.getSimpleName()));
    }

    private static String resolvePath(VaadinRequest request, String path) {
        VaadinUriResolverFactory uriResolverFactory = VaadinSession.getCurrent()
                .getAttribute(VaadinUriResolverFactory.class);
        assert uriResolverFactory != null;

        return uriResolverFactory.toServletContextPath(request, path);
    }

    private static Element parseHtmlImport(InputStream content, String path,
            String tag) {
        assert content != null;
        try {
            Document parsedDocument = Jsoup.parse(content,
                    StandardCharsets.UTF_8.name(), "");
            Optional<Element> optionalDomModule = getDomModule(parsedDocument,
                    tag);
            if (!optionalDomModule.isPresent()) {
                return null;
            }
            Element domModule = optionalDomModule.get();
            removeCommentsRecursively(domModule);
            return domModule;
        } catch (IOException exception) {
            throw new RuntimeException(String.format(
                    "Can't parse the template declared using '%s' path", path),
                    exception);
        }
    }

    private static Optional<Element> getDomModule(Element parent, String id) {
        return parent.getElementsByTag("dom-module").stream()
                .filter(element -> id.equals(element.id())).findFirst();
    }

    private static void removeCommentsRecursively(Node node) {
        int i = 0;
        while (i < node.childNodes().size()) {
            Node child = node.childNode(i);
            if (child instanceof Comment) {
                child.remove();
            } else {
                removeCommentsRecursively(child);
                i++;
            }
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(DefaultTemplateParser.class.getName());
    }

}
