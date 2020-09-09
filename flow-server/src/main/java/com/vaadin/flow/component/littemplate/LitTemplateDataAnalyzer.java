/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.component.littemplate;

import java.io.Serializable;
import java.util.Collections;
import java.util.Optional;

import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.littemplate.LitTemplateParser.TemplateData;
import com.vaadin.flow.component.polymertemplate.IdCollector;
import com.vaadin.flow.component.polymertemplate.TemplateDataAnalyzer.ParserData;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.VaadinService;

/**
 * Template data analyzer which produces immutable data required for template
 * initializer using provided template class and a parser.
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
                idExtractor.getTagById(), Collections.emptyMap(),
                Collections.emptySet(), Collections.emptyList());
    }

    private String getTag(Class<? extends LitTemplate> clazz) {
        Optional<String> tagNameAnnotation = AnnotationReader
                .getAnnotationFor(clazz, Tag.class).map(Tag::value);
        assert tagNameAnnotation.isPresent();
        return tagNameAnnotation.get();
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(LitTemplateDataAnalyzer.class.getName());
    }

}
