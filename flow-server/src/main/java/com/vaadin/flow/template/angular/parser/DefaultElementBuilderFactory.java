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
package com.vaadin.flow.template.angular.parser;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.vaadin.external.jsoup.nodes.Attribute;
import com.vaadin.external.jsoup.nodes.Element;
import com.vaadin.external.jsoup.nodes.Node;
import com.vaadin.flow.template.angular.AbstractBindingValueProvider;
import com.vaadin.flow.template.angular.ElementTemplateBuilder;
import com.vaadin.flow.template.angular.StaticBindingValueProvider;
import com.vaadin.flow.template.angular.TemplateNodeBuilder;
import com.vaadin.flow.template.angular.TemplateParseException;

/**
 * The factory that is default for JSOUP {@link Element}s.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
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

        element.attributes()
                .forEach(attr -> setBinding(attr, builder, element));

        element.childNodes().stream().map(builderProducer::apply)
                .filter(Optional::isPresent).map(Optional::get)
                .forEach(builder::addChild);

        return builder;
    }

    @Override
    protected boolean canHandle(Element node) {
        return true;
    }

    private void setBinding(Attribute attribute, ElementTemplateBuilder builder,
            Element element) {
        String name = attribute.getKey();

        if (name.startsWith("(")) {
            if (!name.endsWith(")")) {
                throw new TemplateParseException(
                        "Event listener registration should be in the form (click)='...' but template contains '"
                                + attribute.toString() + "'.");
            }
            String key = extractKey(name, 1);
            builder.addEventHandler(key, attribute.getValue());
        } else if (name.startsWith("[")) {
            if (!name.endsWith("]")) {
                throw new TemplateParseException(
                        "Property binding should be in the form [property]='value' but template contains '"
                                + attribute.toString() + "'.");
            }
            handlePropertyParsing(attribute, builder, element, name);
        } else {
            /*
             * Regular attribute names in the template, i.e. name not starting
             * with [ or (, are used as static attributes on the target element.
             */
            builder.setAttribute(name,
                    new StaticBindingValueProvider(attribute.getValue()));
        }
    }

    private void handlePropertyParsing(Attribute attribute,
            ElementTemplateBuilder builder, Element element, String name) {
        String key = extractKey(name, 1);
        AbstractBindingValueProvider binding = createExpressionBinding(
                stripForLoopVariableIfNeeded(attribute.getValue()));
        if (key.startsWith("class.")) {
            String className = key.substring("class.".length());

            String classAttribute = element.attr("class");
            if (Stream.of(classAttribute.split("\\s+"))
                    .anyMatch(className::equals)) {
                throw new TemplateParseException(String.format(
                        "The class attribute can't contain '%s' "
                                + "when there's also a binding for [class.%s]",
                        className, className));
            }

            builder.setClassName(className, binding);
        } else if (key.startsWith("attr.")) {
            String attributeName = key.substring("attr.".length());

            if (element.hasAttr(attributeName)) {
                throw new TemplateParseException(String.format(
                        "The '%s' attribute can't be present when there "
                                + "is also a binding for [attr.%s]",
                        attributeName, attributeName));
            }

            builder.setAttribute(attributeName, binding);
        } else {
            builder.setProperty(key, binding);
        }
    }
}
