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
package com.vaadin.hummingbird.template;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 * Parser for an Angular 2-like template syntax.
 *
 * @since
 * @author Vaadin Ltd
 */
public class TemplateParser {
    /**
     * Parses the given template string to a tree of template nodes.
     *
     * @param templateString
     *            the template string to parse, not <code>null</code>
     * @return the template node at the root of the parsed template tree
     */
    public static TemplateNode parse(String templateString) {
        assert templateString != null;

        Document bodyFragment = Jsoup.parseBodyFragment(templateString);
        Elements children = bodyFragment.body().children();

        int childNodeSize = children.size();
        if (childNodeSize != 1) {
            if (childNodeSize == 0) {
                throw new TemplateParseException("Template is empty");
            } else {
                throw new TemplateParseException(
                        "Template has multiple root elements");
            }
        }

        TemplateNodeBuilder templateBuilder = createBuilder(children.get(0));

        TemplateNode templateNode = templateBuilder.build(null);
        return templateNode;
    }

    private static TemplateNodeBuilder createBuilder(Node node) {
        if (node instanceof Element) {
            return createElementBuilder((Element) node);
        } else if (node instanceof TextNode) {
            return createTextBuilder((TextNode) node);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported node type: " + node.getClass().getName());
        }
    }

    private static TextTemplateBuilder createTextBuilder(TextNode node) {
        String text = node.text();

        // No special bindings to support for now
        return new TextTemplateBuilder(new StaticBinding(text));
    }

    private static ElementTemplateBuilder createElementBuilder(
            Element element) {
        ElementTemplateBuilder builder = new ElementTemplateBuilder(
                element.tagName());

        element.attributes().forEach(attr -> {
            // No special bindings to support for now
            builder.setProperty(attr.getKey(),
                    new StaticBinding(attr.getValue()));
        });

        element.childNodes().stream().map(TemplateParser::createBuilder)
                .forEach(builder::addChild);

        return builder;
    }
}
