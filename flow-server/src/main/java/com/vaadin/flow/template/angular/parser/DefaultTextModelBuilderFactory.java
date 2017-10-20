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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.external.jsoup.nodes.Node;
import com.vaadin.external.jsoup.nodes.TextNode;
import com.vaadin.flow.template.angular.ChildSlotBuilder;
import com.vaadin.flow.template.angular.StaticBindingValueProvider;
import com.vaadin.flow.template.angular.TemplateNodeBuilder;
import com.vaadin.flow.template.angular.TextTemplateBuilder;

/**
 * The factory that is default for JSOUP {@link TextNode}s.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class DefaultTextModelBuilderFactory
        extends AbstractTemplateBuilderFactory<TextNode> {

    private static final String TEXT_BINDING_SUFFIX = "}}";
    private static final String TEXT_BINDING_PREFIX = "{{";
    private static final String AT_DELIMITER = "@";
    private static final String CHILD = AT_DELIMITER + "child" + AT_DELIMITER;

    /**
     * Creates a new factory.
     */
    public DefaultTextModelBuilderFactory() {
        super(TextNode.class);
    }

    @Override
    public TemplateNodeBuilder createBuilder(TextNode node,
            Function<Node, Optional<TemplateNodeBuilder>> builderProducer) {
        List<TemplateNodeBuilder> builders = new ArrayList<>();
        collectBuilders(node.getWholeText(), builders);
        return makeCompound(builders);
    }

    @Override
    protected boolean canHandle(TextNode node) {
        return true;
    }

    private void collectBuilders(String text,
            List<TemplateNodeBuilder> builders) {
        if (text.isEmpty()) {
            return;
        }

        int bindingIndex = text.indexOf(TEXT_BINDING_PREFIX);
        int childIndex = text.indexOf(CHILD);

        if (bindingIndex < 0 && childIndex < 0) {
            builders.add(new TextTemplateBuilder(
                    new StaticBindingValueProvider(text)));
        } else {
            Map<Integer, Runnable> handlers = new HashMap<>();
            handlers.put(bindingIndex,
                    createBindingHandler(text, builders, bindingIndex));
            handlers.put(childIndex,
                    createChildHandler(text, builders, childIndex));
            Optional<Runnable> handler = handlers.keySet().stream()
                    .filter(index -> index.compareTo(0) >= 0).sorted()
                    .map(handlers::get).filter(Objects::nonNull).findFirst();
            if (handler.isPresent()) {
                handler.get().run();
            } else {
                builders.add(new TextTemplateBuilder(
                        new StaticBindingValueProvider(text)));
            }
        }
    }

    private Runnable createChildHandler(String text,
            List<TemplateNodeBuilder> builders, int childIndex) {
        return () -> handleChild(text, builders, childIndex);
    }

    private void handleChild(String text, List<TemplateNodeBuilder> builders,
            int childIndex) {
        handleStaticPrefix(text, builders, childIndex);
        builders.add(new ChildSlotBuilder());

        collectBuilders(text.substring(childIndex + CHILD.length()), builders);
    }

    private Runnable createBindingHandler(String text,
            List<TemplateNodeBuilder> builders, int bindingIndex) {
        int index = text.indexOf(TEXT_BINDING_SUFFIX, bindingIndex);
        if (index >= 0) {
            return () -> handleBinding(text, builders, bindingIndex, index);
        }
        return null;
    }

    private void handleBinding(String text, List<TemplateNodeBuilder> builders,
            int bindingIndex, int suffixIndex) {
        handleStaticPrefix(text, builders, bindingIndex);
        builders.add(new TextTemplateBuilder(
                createExpressionBinding(stripForLoopVariableIfNeeded(text
                        .substring(bindingIndex + TEXT_BINDING_PREFIX.length(),
                                suffixIndex)))));

        collectBuilders(
                text.substring(suffixIndex + TEXT_BINDING_SUFFIX.length()),
                builders);
    }

    private void handleStaticPrefix(String text,
            List<TemplateNodeBuilder> builders, int index) {
        if (index > 0) {
            builders.add(new TextTemplateBuilder(
                    new StaticBindingValueProvider(text.substring(0, index))));
        }
    }

    private TemplateNodeBuilder makeCompound(
            Collection<TemplateNodeBuilder> builders) {
        return parent -> builders.stream()
                .flatMap(builder -> builder.build(parent).stream())
                .collect(Collectors.toList());
    }
}
