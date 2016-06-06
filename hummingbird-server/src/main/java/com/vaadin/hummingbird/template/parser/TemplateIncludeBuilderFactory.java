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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.vaadin.hummingbird.template.TemplateIncludeBuilder;
import com.vaadin.hummingbird.template.TemplateNodeBuilder;
import com.vaadin.hummingbird.template.TemplateParseException;

/**
 * The factory that handles "@include relative/path/to/filename.html@" template.
 *
 * @author Vaadin Ltd
 *
 */
public class TemplateIncludeBuilderFactory
        extends AbstractTemplateBuilderFactory<TextNode> {

    private static final String PREFIX = "@include ";
    private static final String SUFFIX = "@";

    /**
     * Creates a new factory.
     */
    public TemplateIncludeBuilderFactory() {
        super(TextNode.class);
    }

    @Override
    public TemplateNodeBuilder createBuilder(TextNode node,
            TemplateResolver templateResolver,
            Function<Node, Optional<TemplateNodeBuilder>> builderProducer) {
        Collection<String> paths = getIncludePaths(node);
        assert !paths.isEmpty();
        return new TemplateIncludeBuilder(templateResolver,
                paths.toArray(new String[paths.size()]));
    }

    @Override
    protected boolean canHandle(TextNode node) {
        return !getIncludePaths(node).isEmpty();
    }

    /**
     * Gets the include paths from a text node with an "@include path/to/file@"
     * directives.
     *
     * @param node
     *            the text node which possibly contains include directives
     * @return a collection of file paths, if the text node does not contain an
     *         include file with path then the collection is empty
     */
    static Collection<String> getIncludePaths(TextNode node) {
        String nodeText = node.text().trim();
        if (!nodeText.startsWith(PREFIX)) {
            return Collections.emptyList();
        }
        if (!nodeText.endsWith(SUFFIX)) {
            return Collections.emptyList();
        }

        List<String> paths = new ArrayList<>();
        int endIndex = nodeText.indexOf(SUFFIX, PREFIX.length());
        while (endIndex != -1) {
            if (!nodeText.startsWith(PREFIX)) {
                reportBadDirective(node, nodeText);
            }
            paths.add(nodeText.substring(PREFIX.length(), endIndex).trim());
            nodeText = nodeText.substring(endIndex + 1).trim();
            endIndex = nodeText.indexOf(SUFFIX, PREFIX.length());
        }
        if (!nodeText.isEmpty()) {
            reportBadDirective(node, nodeText);
        }
        return paths;
    }

    private static void reportBadDirective(TextNode node, String nodeText) {
        throw new TemplateParseException(String.format(
                "Unexpected include directive subcontent '%s' in text node %s",
                nodeText, node.text().trim()));
    }

}
