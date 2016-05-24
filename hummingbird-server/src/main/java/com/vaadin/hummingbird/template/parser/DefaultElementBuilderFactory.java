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

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import com.vaadin.hummingbird.template.ElementTemplateBuilder;
import com.vaadin.hummingbird.template.ModelValueBindingProvider;
import com.vaadin.hummingbird.template.StaticBindingValueProvider;
import com.vaadin.hummingbird.template.TemplateNodeBuilder;
import com.vaadin.hummingbird.template.TemplateParseException;

/**
 * The factory that is default for JSOUP {@link Element}s.
 *
 * @author Vaadin Ltd
 *
 */
public class DefaultElementBuilderFactory
        extends AbstractTemplateBuilderFactory<Element> {

    /**
     * Creates a new factory.
     */
    public DefaultElementBuilderFactory() {
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

    private void setBinding(Attribute attribute,
            ElementTemplateBuilder builder) {
        String name = attribute.getKey();

        if (name.startsWith("(")) {
            if (!name.endsWith(")")) {
                StringBuilder msg = new StringBuilder(
                        "Event listener registration should be in the form (click)='...' but template contains '");
                msg.append(attribute.toString()).append("'.");
                throw new TemplateParseException(msg.toString());
            }
            String key = extractKey(name, 1);
            builder.addEventHandler(key, attribute.getValue());
        } else if (name.startsWith("[")) {
            if (!name.endsWith("]")) {
                StringBuilder msg = new StringBuilder(
                        "Property binding should be in the form [property]='value' but template contains '");
                msg.append(attribute.toString()).append("'.");
                throw new TemplateParseException(msg.toString());
            }
            String key = extractKey(name, 1);
            builder.setProperty(key, new ModelValueBindingProvider(
                    stripForLoopVariableIfNeeded(attribute.getValue())));
        } else {
            /*
             * Regular attribute names in the template, i.e. name not starting
             * with [ or (, are used as static attributes on the target element.
             */
            builder.setAttribute(name,
                    new StaticBindingValueProvider(attribute.getValue()));
        }
    }
}
