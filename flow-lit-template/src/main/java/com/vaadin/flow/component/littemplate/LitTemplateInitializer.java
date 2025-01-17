/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.vaadin.flow.component.littemplate.LitTemplateParser.LitTemplateParserFactory;
import com.vaadin.flow.component.template.internal.IdMapper;
import com.vaadin.flow.component.template.internal.ParserData;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.server.VaadinService;

/**
 * Template initialization related logic.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public class LitTemplateInitializer {
    private static final ConcurrentHashMap<LitTemplateParser, ReflectionCache<LitTemplate, ParserData>> CACHE = new ConcurrentHashMap<>();

    private final LitTemplate template;

    private final ParserData parserData;

    private final Class<? extends LitTemplate> templateClass;

    /**
     * Creates a new initializer instance.
     * <p>
     * The call is delegated to the
     * {@link #LitTemplateInitializer(LitTemplate, LitTemplateParser, VaadinService)}
     * with parser created via {@link LitTemplateParserFactory} retrieved from
     * {@link Instantiator}.
     *
     * @param template
     *            a template to initialize
     * @param service
     *            the related service
     *
     * @see VaadinService
     * @see LitTemplateParserFactory
     * @see Instantiator
     * @see Instantiator#getOrCreate(Class)
     */
    public LitTemplateInitializer(LitTemplate template, VaadinService service) {
        this(template, LitTemplate.getParser(service), service);
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

        templateClass = template.getClass();

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

        parserData.forEachInjectedField(
                (field, id, tag) -> idMapper.mapComponentOrElement(field, id,
                        tag,
                        element -> new InjectableLitElementInitializer(element,
                                templateClass)
                                .accept(parserData.getAttributes(id))));
    }

}
