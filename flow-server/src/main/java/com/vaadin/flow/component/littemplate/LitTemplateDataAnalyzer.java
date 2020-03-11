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

import com.vaadin.flow.component.polymertemplate.IdCollector;
import com.vaadin.flow.component.polymertemplate.TemplateDataAnalyzer.ParserData;

/**
 * Template data analyzer which produces immutable data required for template
 * initializer using provided template class and a parser.
 *
 * @author Vaadin Ltd
 *
 */
class LitTemplateDataAnalyzer implements Serializable {

    private final Class<? extends LitTemplate> templateClass;

    /**
     * Create an instance of the analyzer using the {@code templateClass} and the
     * template {@code parser}.
     *
     * @param templateClass a template type
     * @param parser        a template parser
     * @param service       the related service instance
     */
    LitTemplateDataAnalyzer(Class<? extends LitTemplate> templateClass) {
        this.templateClass = templateClass;
    }

    /**
     * Gets the template data for the template initializer.
     *
     * @return the template data
     */
    ParserData parseTemplate() {
        IdCollector idExtractor = new IdCollector(templateClass, null, null);
        idExtractor.collectInjectedIds(Collections.emptySet());
        return new ParserData(idExtractor.getIdByField(), idExtractor.getTagById(), Collections.emptySet(),
                Collections.emptyList());
    }

}
