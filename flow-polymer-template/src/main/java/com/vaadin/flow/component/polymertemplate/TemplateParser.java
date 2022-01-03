/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.VaadinService;

/**
 * Template content parser.
 * <p>
 * It returns a JSOUP element representing the content of template for the given
 * template class.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see NpmTemplateParser
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @deprecated Use {@code LitTemplateParser} for {@code LitTemplate} components.
 *             Polymer template support is deprecated - we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
@FunctionalInterface
public interface TemplateParser {

    /**
     * Wrapper for the parsing result.
     * <p>
     * The data contains path uri where the template is declared and its content
     * as an {@link Element} instance.
     *
     * @author Vaadin Ltd
     * @since 1.0
     * @deprecated Use {@code LitTemplateParser.TemplateData} instead
     */
    @Deprecated
    class TemplateData {

        private final String modulePath;
        private final Element templateElement;

        public TemplateData(String uri, Element element) {
            modulePath = uri;
            templateElement = element;
        }

        /**
         * Gets the uri where the template is declared.
         *
         * @return template uri
         */
        public String getModulePath() {
            return modulePath;
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
     * Template parser factory.
     * <p>
     * To be able to create a parser which can be provided as SPI use
     * {@link Instantiator} to create the factory and then get a parser from it:
     * 
     * <pre>
     * <code>
     * Instantiator instantiator = ...;
     * TemplateParserFactory factory = instantiator.getOrCreate(TemplateParserFactory.class);
     * TemplateParser parser = factory.createParser();
     * </code>
     * </pre>
     * <p>
     * 
     * @author Vaadin Ltd
     * @since
     *
     */
    class TemplateParserFactory {

        /**
         * Creates a template parser instance.
         * 
         * @return a template parser instance
         */
        public TemplateParser createParser() {
            return NpmTemplateParser.getInstance();
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
