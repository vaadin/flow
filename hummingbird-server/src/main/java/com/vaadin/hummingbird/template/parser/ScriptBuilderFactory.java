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

import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import com.vaadin.hummingbird.template.ScriptNodeBuilder;
import com.vaadin.hummingbird.template.TemplateNodeBuilder;

/**
 * Factory that handles &lt;script&gt; tags in templates.
 *
 * @author Vaadin Ltd
 *
 */
public class ScriptBuilderFactory
        extends AbstractTemplateBuilderFactory<Element> {

    /**
     * Creates a new factory.
     */
    public ScriptBuilderFactory() {
        super(Element.class);
    }

    @Override
    public TemplateNodeBuilder createBuilder(Element node,
            Function<Node, Optional<TemplateNodeBuilder>> builderProducer) {
        DataNode dataNode = (DataNode) node.childNode(0);
        String script = dataNode.getWholeData();
        return new ScriptNodeBuilder(script);

    }

    @Override
    protected boolean canHandle(Element node) {
        return "script".equals(node.tagName());
    }

}
