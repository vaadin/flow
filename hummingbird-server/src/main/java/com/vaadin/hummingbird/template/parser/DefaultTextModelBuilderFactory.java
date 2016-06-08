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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.vaadin.hummingbird.template.ChildSlotBuilder;
import com.vaadin.hummingbird.template.ModelValueBindingProvider;
import com.vaadin.hummingbird.template.StaticBindingValueProvider;
import com.vaadin.hummingbird.template.TemplateIncludeBuilder;
import com.vaadin.hummingbird.template.TemplateNodeBuilder;
import com.vaadin.hummingbird.template.TextTemplateBuilder;

/**
 * The factory that is default for JSOUP {@link TextNode}s.
 *
 * @author Vaadin Ltd
 *
 */
public class DefaultTextModelBuilderFactory
        extends AbstractTemplateBuilderFactory<TextNode> {

    private static final String CHILD = "@child@";
    private static final String INCLUDE_SUFFIX = "@";
    private static final String INCLUDE_PREFIX = INCLUDE_SUFFIX + "include ";

    /**
     * Creates a new factory.
     */
    public DefaultTextModelBuilderFactory() {
        super(TextNode.class);
    }

    @Override
    public TemplateNodeBuilder createBuilder(TextNode node,
            TemplateResolver templateResolver,
            Function<Node, Optional<TemplateNodeBuilder>> builderProducer) {
        List<TemplateNodeBuilder> builders = new ArrayList<>();
        collectBuilders(node.text(), builders, templateResolver);
        return makeCompound(builders);
    }

    @Override
    protected boolean canHandle(TextNode node) {
        return true;
    }

    private void collectBuilders(String text,
            List<TemplateNodeBuilder> builders,
            TemplateResolver templateResolver) {
        if (text.isEmpty()) {
            return;
        }

        int bindingIndex = text.indexOf("{{");
        int childIndex = text.indexOf(CHILD);
        int includeIndex = text.indexOf(INCLUDE_PREFIX);

        if (bindingIndex < 0 && childIndex < 0 && includeIndex < 0) {
            builders.add(new TextTemplateBuilder(
                    new StaticBindingValueProvider(text)));
        } else {
            Map<Integer, Runnable> handlers = new HashMap<>();
            handlers.put(bindingIndex, createBindingHandler(text, builders,
                    templateResolver, bindingIndex));
            handlers.put(childIndex, createChildHandler(text, builders,
                    templateResolver, childIndex));
            handlers.put(includeIndex, getIncludeHandler(text, builders,
                    templateResolver, includeIndex));
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
            List<TemplateNodeBuilder> builders,
            TemplateResolver templateResolver, int childIndex) {
        return () -> handleChild(text, builders, templateResolver, childIndex);
    }

    private void handleChild(String text, List<TemplateNodeBuilder> builders,
            TemplateResolver templateResolver, int childIndex) {
        handleStaticPrefix(text, builders, childIndex);
        builders.add(new ChildSlotBuilder());

        collectBuilders(text.substring(childIndex + CHILD.length()), builders,
                templateResolver);
    }

    private Runnable getIncludeHandler(String text,
            List<TemplateNodeBuilder> builders,
            TemplateResolver templateResolver, int includeIndex) {
        int index = text.indexOf(INCLUDE_SUFFIX,
                INCLUDE_PREFIX.length() + includeIndex);
        if (index >= 0) {
            return () -> handleInclude(text, builders, templateResolver,
                    includeIndex, index);
        }
        return null;
    }

    private void handleInclude(String text, List<TemplateNodeBuilder> builders,
            TemplateResolver templateResolver, int includeIndex,
            int suffixIndex) {
        handleStaticPrefix(text, builders, includeIndex);
        builders.add(new TemplateIncludeBuilder(templateResolver, text
                .substring(INCLUDE_PREFIX.length() + includeIndex, suffixIndex)
                .trim()));

        collectBuilders(text.substring(suffixIndex + 1), builders,
                templateResolver);
    }

    private Runnable createBindingHandler(String text,
            List<TemplateNodeBuilder> builders,
            TemplateResolver templateResolver, int bindingIndex) {
        int index = text.indexOf("}}", bindingIndex);
        if (index >= 0) {
            return () -> handleBinding(text, builders, templateResolver,
                    bindingIndex, index);
        }
        return null;
    }

    private void handleBinding(String text, List<TemplateNodeBuilder> builders,
            TemplateResolver templateResolver, int bindingIndex,
            int suffixIndex) {
        handleStaticPrefix(text, builders, bindingIndex);
        builders.add(new TextTemplateBuilder(
                new ModelValueBindingProvider(stripForLoopVariableIfNeeded(
                        text.substring(bindingIndex + 2, suffixIndex)))));

        collectBuilders(text.substring(suffixIndex + 2), builders,
                templateResolver);
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
