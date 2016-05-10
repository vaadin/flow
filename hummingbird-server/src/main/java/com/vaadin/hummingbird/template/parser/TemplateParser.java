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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.vaadin.hummingbird.template.TemplateNode;
import com.vaadin.hummingbird.template.TemplateNodeBuilder;
import com.vaadin.hummingbird.template.TemplateParseException;

/**
 * Parser for an Angular 2-like template syntax.
 *
 * @author Vaadin Ltd
 */
public class TemplateParser {

    private static final String ROOT_CLARIFICATION = "If the template contains <html> and <body> tags,"
            + " then only the contents of the <body> tag will be used.";

    private static final Collection<TemplateNodeBuilderFactory<?>> FACTORIES = loadFactories();
    private static final Collection<TemplateNodeBuilderFactory<?>> DEFAULT_FACTORIES = loadDefaultFactories();

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

    @SuppressWarnings("rawtypes")
    private static Collection<TemplateNodeBuilderFactory<?>> loadFactories() {
        Collection<TemplateNodeBuilderFactory<?>> factories = new ArrayList<>();
        factories.add(new ChildTextNodeBuilderFactory());
        factories.add(new TextModelBuilderFactory());
        factories.add(new ForElementBuilderFactory());
        return factories;
    }

    @SuppressWarnings("rawtypes")
    private static Collection<TemplateNodeBuilderFactory<?>> loadDefaultFactories() {
        Collection<TemplateNodeBuilderFactory<?>> factories = new ArrayList<>();
        factories.add(new DefaultTextModelBuilderFactory());
        factories.add(new DefaultElementBuilderFactory());
        return factories;
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

        Optional<TemplateNodeBuilder> templateBuilder = createBuilder(
                children.get(0));
        assert templateBuilder.isPresent();
        return templateBuilder.get().build(null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Optional<TemplateNodeBuilder> createBuilder(Node node) {
        if (node instanceof Comment) {
            return Optional.empty();
        }
        List<TemplateNodeBuilderFactory<?>> list = filterApplicable(FACTORIES,
                node);
        if (list.isEmpty()) {
            list = filterApplicable(DEFAULT_FACTORIES, node);
            if (list.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unsupported node type: " + node.getClass().getName());
            }
        }
        assert list.size() == 1;
        TemplateNodeBuilderFactory factory = list.get(0);
        Function<Node, Optional<TemplateNodeBuilder>> function = TemplateParser::createBuilder;
        return Optional.of(factory.createBuilder(node, function));
    }

    private static List<TemplateNodeBuilderFactory<?>> filterApplicable(
            Collection<TemplateNodeBuilderFactory<?>> factories, Node node) {
        return factories.stream().filter(factory -> factory.isApplicable(node))
                .collect(Collectors.toList());
    }

}
