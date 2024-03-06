/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.littemplate;

import org.jsoup.nodes.Element;

import com.vaadin.flow.component.littemplate.internal.LitTemplateParserImpl;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.VaadinService;

/**
 * Lit template content parser.
 * <p>
 * It returns a JSOUP element representing the content of template for the given
 * template class.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see LitTemplateParserImpl
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
public interface LitTemplateParser {

    /**
     * Lit template parser factory.
     * <p>
     * To be able to create a parser which can be provided as SPI use
     * {@link Instantiator} to create the factory and then get a parser from it:
     *
     * <pre>
     * <code>
     * Instantiator instantiator = ...;
     * LitTemplateParserFactory factory = instantiator.getOrCreate(LitTemplateParserFactory.class);
     * LitTemplateParser parser = factory.createParser();
     * </code>
     * </pre>
     * <p>
     *
     * @author Vaadin Ltd
     * @see LitTemplateParser
     * @since
     *
     */
    class LitTemplateParserFactory {

        /**
         * Creates a Lit template parser instance.
         *
         * @return a lit template parser instance
         */
        public LitTemplateParser createParser() {
            return LitTemplateParserImpl.getInstance();
        }
    }

    /**
     * Wrapper for the parsing result.
     * <p>
     * The data contains path uri where the template is declared and its content
     * as an {@link Element} instance.
     *
     * @author Vaadin Ltd
     * @since
     *
     */
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
     * @return the template data, may be {@code null}
     */
    TemplateData getTemplateContent(Class<? extends LitTemplate> clazz,
            String tag, VaadinService service);
}
