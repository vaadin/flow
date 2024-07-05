/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import org.jsoup.nodes.Element;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.server.VaadinService;

/**
 * Template content parser.
 * <p>
 * It returns a JSOUP element representing the content of template for the given
 * template class.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see DefaultTemplateParser
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@FunctionalInterface
public interface TemplateParser {

    /**
     * Wrapper for the parsing result.
     * <p>
     * The data contains {@link HtmlImport} uri where the template is declared
     * and its content as an {@link Element} instance.
     *
     * @author Vaadin Ltd
     * @since 1.0
     *
     */
    class TemplateData {

        private final String htmlImportUri;
        private final Element templateElement;

        public TemplateData(String uri, Element element) {
            htmlImportUri = uri;
            templateElement = element;
        }

        /**
         * Gets the {@link HtmlImport} uri where the template is declared.
         *
         * @return template uri
         */
        public String getHtmlImportUri() {
            return htmlImportUri;
        }

        /**
         * Gets the content of the template.
         *
         * @return the content of the template
         */
        public Element getTemplateElement() {
            return templateElement;
        }
    }

    /**
     * Gets the template data which contains a JSOUP {@link Element}
     * representing the template content and the template uri.
     *
     * @param clazz
     *            the template class
     * @param tag
     *            the template tag name
     * @param service
     *            the related Vaadin service
     *
     * @return the template data
     */
    TemplateData getTemplateContent(Class<? extends PolymerTemplate<?>> clazz,
            String tag, VaadinService service);
}
