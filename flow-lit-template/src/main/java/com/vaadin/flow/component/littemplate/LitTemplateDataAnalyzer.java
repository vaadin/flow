/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.littemplate;

import java.io.Serializable;
import java.util.Collections;
import java.util.Optional;

import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.littemplate.LitTemplateParser.TemplateData;
import com.vaadin.flow.component.template.internal.IdCollector;
import com.vaadin.flow.component.template.internal.ParserData;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.VaadinService;

/**
 * Template data analyzer which produces immutable data required for template
 * initializer using provided template class and a parser.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
class LitTemplateDataAnalyzer implements Serializable {

    private final Class<? extends LitTemplate> templateClass;
    private final LitTemplateParser parser;
    private final VaadinService service;
    private final String tag;

    /**
     * Create an instance of the analyzer using the {@code templateClass} and
     * the template {@code parser}.
     *
     * @param templateClass
     *            a template type
     * @param parser
     *            a template parser
     * @param service
     *            the related service instance
     */
    LitTemplateDataAnalyzer(Class<? extends LitTemplate> templateClass,
            LitTemplateParser parser, VaadinService service) {
        this.templateClass = templateClass;
        this.parser = parser;
        this.service = service;
        tag = getTag(templateClass);
    }

    /**
     * Gets the template data for the template initializer.
     *
     * @return the template data
     */
    ParserData parseTemplate() {
        TemplateData templateData = parser.getTemplateContent(templateClass,
                tag, service);
        if (templateData == null) {
            getLogger().info("Couldn't parse template for {} class. "
                    + "Only specific Lit template format is supported. Please check that your template definition"
                    + " directly contains 'render' method which returns html`_template_content_`.",
                    templateClass);
        }

        Element templateRoot = templateData == null ? null
                : templateData.getTemplateElement();
        String modulePath = templateData == null ? null
                : templateData.getModulePath();
        IdCollector idExtractor = new IdCollector(templateClass, modulePath,
                templateRoot);
        idExtractor.collectInjectedIds(Collections.emptySet());
        return new ParserData(idExtractor.getIdByField(),
                idExtractor.getTagById(), idExtractor.getAttributes());
    }

    private String getTag(Class<? extends LitTemplate> clazz) {
        Optional<String> tagNameAnnotation = AnnotationReader
                .getAnnotationFor(clazz, Tag.class).map(Tag::value);
        assert tagNameAnnotation.isPresent();
        return tagNameAnnotation.get();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(LitTemplateDataAnalyzer.class.getName());
    }

}
