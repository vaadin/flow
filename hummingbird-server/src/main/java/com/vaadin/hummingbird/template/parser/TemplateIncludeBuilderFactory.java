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

import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.vaadin.hummingbird.template.TemplateIncludeBuilder;
import com.vaadin.hummingbird.template.TemplateNodeBuilder;

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
        Optional<String> path = getIncludePath(node);
        assert path.isPresent();
        return new TemplateIncludeBuilder(path.get(), templateResolver);
    }

    @Override
    protected boolean canHandle(TextNode node) {
        return getIncludePath(node).isPresent();
    }

    /**
     * Gets the include path from a text node with an "@include path/to/file@"
     * directive.
     *
     * @param node
     *            the text node which possibly contains an include directive
     * @return an optional file with path, or an empty optional if the text node
     *         does not contain an include file with path
     */
    static Optional<String> getIncludePath(TextNode node) {
        String nodeText = node.text().trim();
        if (!nodeText.startsWith(PREFIX)) {
            return Optional.empty();
        }
        if (!nodeText.endsWith(SUFFIX)) {
            return Optional.empty();
        }

        String path = nodeText.substring(PREFIX.length(),
                nodeText.length() - "@".length());
        return Optional.of(path.trim());
    }

}
