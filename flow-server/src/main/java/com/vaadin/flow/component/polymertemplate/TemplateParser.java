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
package com.vaadin.flow.component.polymertemplate;

import org.jsoup.nodes.Element;

/**
 * Template content parser.
 * <p>
 * It returns a JSOUP element representing the content of template for the given
 * template class.
 *
 * @see DefaultTemplateParser
 *
 * @author Vaadin Ltd
 *
 */
@FunctionalInterface
public interface TemplateParser {

    /**
     * Gets a JSOUP {@link Element} representing the template content.
     *
     * @param clazz
     *            the template class
     * @param tag
     *            the template tag name
     *
     * @return template content as an Element
     */
    Element getTemplateContent(Class<? extends PolymerTemplate<?>> clazz,
                               String tag);
}
