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
package com.vaadin.flow.component.polymertemplate;

import org.jsoup.nodes.Element;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.server.VaadinService;

/**
 * Template content parser.
 * <p>
 * It returns a JSOUP element representing the content of template for the given
 * template class.
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
