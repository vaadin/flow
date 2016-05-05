/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template.parser;

import java.util.Optional;
import java.util.function.Function;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import com.vaadin.hummingbird.processor.annotations.ServiceProvider;
import com.vaadin.hummingbird.template.ElementTemplateBuilder;
import com.vaadin.hummingbird.template.TemplateNodeBuilder;

/**
 * @author Vaadin Ltd
 *
 */
@ServiceProvider(TemplateNodeBuilderFactory.class)
public class ElementBuilderFactory
        extends AbstractTemplateBuilderFactory<Element> {

    public ElementBuilderFactory() {
        super(Element.class);
    }

    @Override
    public TemplateNodeBuilder createBuilder(Element element,
            Function<Node, Optional<TemplateNodeBuilder>> builderProducer) {
        ElementTemplateBuilder builder = new ElementTemplateBuilder(
                element.tagName());

        element.attributes().forEach(attr -> setBinding(attr, builder));

        element.childNodes().stream().map(builderProducer::apply)
                .filter(Optional::isPresent).map(Optional::get)
                .forEach(builder::addChild);

        return builder;
    }

    @Override
    protected boolean canHandle(Element node) {
        return true;
    }

}
