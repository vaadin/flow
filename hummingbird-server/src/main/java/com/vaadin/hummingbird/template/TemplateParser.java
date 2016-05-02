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

import java.io.IOException;
import java.io.InputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 * Parser for an Angular 2-like template syntax.
 *
 * @author Vaadin Ltd
 */
public class TemplateParser {

    private static final String ROOT_CLARIFICATION = "If the template contains <html> and <body> tags,"
            + " then only the contents of the <body> tag will be used.";

    private TemplateParser() {
        // Only static methods
    }

    /**
     * Parses the template from the given input stream to a tree of template
     * nodes.
     *
     * @param templateStream
     *            the input stream containing the template to parse, not
     *            <code>null</code>
     * @return the template node at the root of the parsed template tree
     */
    public static TemplateNode parse(InputStream templateStream) {
        assert templateStream != null;
        try {
            Document document = Jsoup.parse(templateStream, null, "");

            return parse(document);
        } catch (IOException e) {
            throw new TemplateParseException("Error reading template data", e);
        }
    }

    /**
     * Parses the given template string to a tree of template nodes.
     *
     * @param templateString
     *            the template string to parse, not <code>null</code>
     * @return the template node at the root of the parsed template tree
     */
    public static TemplateNode parse(String templateString) {
        assert templateString != null;

        Document document = Jsoup.parseBodyFragment(templateString);

        return parse(document);
    }

    private static TemplateNode parse(Document bodyFragment) {
        Elements children = bodyFragment.body().children();

        int childNodeSize = children.size();
        if (childNodeSize != 1) {
            if (childNodeSize == 0) {
                throw new TemplateParseException(
                        "Template must not be empty. " + ROOT_CLARIFICATION);
            } else {
                throw new TemplateParseException(
                        "Template must not have multiple root elements. "
                                + ROOT_CLARIFICATION);
            }
        }

        TemplateNodeBuilder templateBuilder = createBuilder(children.get(0));

        return templateBuilder.build(null);
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

    private static TemplateNodeBuilder createTextBuilder(TextNode node) {
        String text = node.text();
        String trimmedText = text.trim();

        if ("@child@".equals(trimmedText)) {
            return new ChildSlotBuilder();
        } else if (text.startsWith("{{") && text.endsWith("}}")) {
            String key = text.substring(2);
            key = key.substring(0, key.length() - 2);
            return new TextTemplateBuilder(
                    new ModelValueBinding(ModelValueBinding.TEXT, key));
        } else {
            // No special bindings to support for now
            return new TextTemplateBuilder(new StaticBinding(text));
        }
    }

    private static ElementTemplateBuilder createElementBuilder(
            Element element) {
        ElementTemplateBuilder builder = new ElementTemplateBuilder(
                element.tagName());

        element.attributes().forEach(attr -> {
            String name = attr.getKey();

            if (name.startsWith("(") || name.startsWith("[")) {
                throw new TemplateParseException(
                        "Dynamic binding support has not yet been implemented");
            } else {
                /*
                 * Regular attribute names in the template, i.e. name not
                 * starting with [ or (, are used as static attributes on the
                 * target element.
                 */
                builder.setAttribute(name, new StaticBinding(attr.getValue()));
            }
        });

        element.childNodes().stream().map(TemplateParser::createBuilder)
                .forEach(builder::addChild);

        return builder;
    }
}
