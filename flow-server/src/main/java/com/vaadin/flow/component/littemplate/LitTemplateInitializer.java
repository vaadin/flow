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

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.vaadin.flow.component.polymertemplate.IdMapper;
import com.vaadin.flow.component.polymertemplate.TemplateDataAnalyzer.ParserData;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.server.VaadinService;

/**
 * Template initialization related logic.
 *
 * @author Vaadin Ltd
 *
 */
public class LitTemplateInitializer {
    private static final ConcurrentHashMap<LitTemplateParser, ReflectionCache<LitTemplate, ParserData>> CACHE = new ConcurrentHashMap<>();

    private final LitTemplate template;

    private final ParserData parserData;

    /**
     * Creates a new initializer instance.
     *
     * @param template
     *            a template to initialize
     * @param service
     *            the related service
     */
    public LitTemplateInitializer(LitTemplate template, VaadinService service) {
        this(template, LitTemplateParserImpl.getInstance(), service);
    }

    /**
     * Creates a new initializer instance.
     *
     * @param template
     *            a template to initialize
     * @param parser
     *            lit template parser
     * @param service
     *            the related service
     */
    LitTemplateInitializer(LitTemplate template, LitTemplateParser parser,
            VaadinService service) {
        this.template = template;

        boolean productionMode = service.getDeploymentConfiguration()
                .isProductionMode();

        Class<? extends LitTemplate> templateClass = template.getClass();

        ParserData data = null;
        if (productionMode) {
            ReflectionCache<LitTemplate, ParserData> cache = CACHE
                    .computeIfAbsent(parser, analyzer -> new ReflectionCache<>(
                            clazz -> new LitTemplateDataAnalyzer(clazz,
                                    analyzer, service).parseTemplate()));
            data = cache.get(templateClass);
        }
        if (data == null) {
            data = new LitTemplateDataAnalyzer(templateClass, parser, service)
                    .parseTemplate();
        }
        parserData = data;
    }

    /**
     * Initializes child elements.
     */
    public void initChildElements() {
        IdMapper idMapper = new IdMapper(template);
        Consumer<Element> noOp = element -> {
            // Nothing to do for elements
        };

        parserData.forEachInjectedField((field, id, tag) -> idMapper
                .mapComponentOrElement(field, id, tag, noOp));
    }

}
