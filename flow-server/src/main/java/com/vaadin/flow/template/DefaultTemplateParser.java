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
package com.vaadin.flow.template;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.vaadin.annotations.AnnotationReader;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.external.jsoup.Jsoup;
import com.vaadin.external.jsoup.nodes.Element;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.VaadinUriResolverFactory;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.shared.VaadinUriResolver;

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

    @SuppressWarnings("rawtypes")
    @Override
    public Element getTemplateContent(Class<? extends PolymerTemplate> clazz,
            String tag) {
        VaadinRequest request = VaadinService.getCurrentRequest();
        WrappedHttpSession session = (WrappedHttpSession) request
                .getWrappedSession();
        assert session != null;

        ServletContext context = session.getHttpSession().getServletContext();
        for (HtmlImport htmlImport : AnnotationReader.getAnnotationsFor(clazz,
                HtmlImport.class)) {
            String path = resolvePath(request, htmlImport.value());
            getLogger().info(
                    String.format("Html import path '%s' is resolved to '%s'",
                            htmlImport.value(), path));
            try (InputStream content = context.getResourceAsStream(path)) {
                if (content == null) {
                    throw new IllegalStateException(String.format(
                            "Can't find resource '%s' "
                                    + "via the servlet context",
                            htmlImport.value()));
                }
                Element templateElement = parseHtmlImport(content,
                        htmlImport.value());
                if (isTemplateImport(templateElement, tag)) {
                    getLogger().info(String.format(
                            "Found a template file containing template "
                                    + "definition for the tag '%s' by the path '%s'",
                            tag, htmlImport.value()));
                    return templateElement;
                }
            } catch (IOException exception) {
                // ignore exception on close()
                getLogger().log(Level.WARNING,
                        "Couldn't close template input stream", exception);
            }
        }
        throw new IllegalStateException(String.format(
                "Couldn't find the "
                        + "definition of the element with tag '%s' "
                        + "in any template file declared using @'%s' annotations. "
                        + "Check the availability of the template files in your WAR "
                        + "file or provide alternative implementation of the "
                        + "method getTemplateContent() which should return an element "
                        + "representing the content of the template file",
                tag, HtmlImport.class.getSimpleName()));
    }

    private String resolvePath(VaadinRequest request, String path) {
        VaadinUriResolverFactory uriResolverFactory = VaadinSession.getCurrent()
                .getAttribute(VaadinUriResolverFactory.class);
        assert uriResolverFactory != null;

        VaadinUriResolver uriResolver = uriResolverFactory
                .getUriResolver(request);
        assert uriResolver != null;

        String pathInfo = request.getPathInfo();
        String uri = uriResolver.resolveVaadinUri(path);
        if (pathInfo == null || uri.startsWith("/")) {
            return uri;
        }
        if (pathInfo.endsWith("/")) {
            return pathInfo + uri;
        } else {
            return pathInfo + "/" + uri;
        }
    }

    private boolean isTemplateImport(Element contentElement, String tag) {
        if (contentElement == null) {
            return false;
        }
        return contentElement.getElementsByTag("dom-module").stream()
                .anyMatch(element -> tag.equals(element.attr("id")));
    }

    private Element parseHtmlImport(InputStream content, String path) {
        assert content != null;
        try {
            return Jsoup.parse(content, StandardCharsets.UTF_8.name(), "");
        } catch (IOException exception) {
            throw new RuntimeException(String.format(
                    "Can't parse the template declared using '%s' path", path),
                    exception);
        }
    }

    private Logger getLogger() {
        return Logger.getLogger(DefaultTemplateParser.class.getName());
    }

}
