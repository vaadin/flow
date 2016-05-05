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

import com.vaadin.hummingbird.processor.annotations.ServiceProvider;
import com.vaadin.hummingbird.template.ModelValueBindingProvider;
import com.vaadin.hummingbird.template.TemplateNodeBuilder;
import com.vaadin.hummingbird.template.TextTemplateBuilder;

/**
 * @author Vaadin Ltd
 *
 */
@ServiceProvider(TemplateNodeBuilderFactory.class)
public class TextModelBuilderFactory
        extends AbstractTemplateBuilderFactory<TextNode> {

    /**
     * @param nodeType
     */
    public TextModelBuilderFactory() {
        super(TextNode.class);
    }

    @Override
    public TemplateNodeBuilder createBuilder(TextNode node,
            Function<Node, Optional<TemplateNodeBuilder>> builderProducer) {
        String text = node.text();
        String key = text.substring(2);
        key = key.substring(0, key.length() - 2);
        return new TextTemplateBuilder(new ModelValueBindingProvider(key));
    }

    @Override
    protected boolean canHandle(TextNode node) {
        String text = node.text();
        return text.startsWith("{{") && text.endsWith("}}");
    }

}
