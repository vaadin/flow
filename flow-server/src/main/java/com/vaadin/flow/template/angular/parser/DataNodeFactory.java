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

import com.vaadin.external.jsoup.nodes.DataNode;
import com.vaadin.external.jsoup.nodes.Node;
import com.vaadin.flow.template.angular.StaticBindingValueProvider;
import com.vaadin.flow.template.angular.TemplateNodeBuilder;
import com.vaadin.flow.template.angular.TextTemplateBuilder;

/**
 * Handles data nodes, i.e. the content of inline &lt;script&gt; and
 * &lt;style&gt;.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class DataNodeFactory extends AbstractTemplateBuilderFactory<DataNode> {

    protected DataNodeFactory() {
        super(DataNode.class);
    }

    @Override
    protected boolean canHandle(DataNode node) {
        return true;
    }

    @Override
    public TemplateNodeBuilder createBuilder(DataNode node,
            Function<Node, Optional<TemplateNodeBuilder>> builderProducer) {
        String data = node.getWholeData();

        return new TextTemplateBuilder(new StaticBindingValueProvider(data));
    }

}
