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

import com.vaadin.hummingbird.template.TemplateNodeBuilder;

/**
 * Strategy to handle a JSOUP node and produce a {@link TemplateNodeBuilder}
 * instance if applicable.
 * 
 * @param <T>
 *            the node type which factory is able to handle
 * 
 * @author Vaadin Ltd
 *
 */
public interface TemplateNodeBuilderFactory<T extends Node> {

    /**
     * Returns {@code true} if applicable to the {@code node}.
     * <p>
     * Only one factory must be applicable for the {@code node}. The
     * {@link #isDefault(Node)} method is also checked to apply the factory as a
     * default strategy in case there are no applicable factories.
     * 
     * @see #isDefault(Node)
     * @param node
     *            the node to check against of
     * @return {@code true} if the factory is applicable to the node
     */
    boolean isApplicable(Node node);

    /**
     * Returns a template builder for the given {@code node} using
     * {@code builderProducer} as a context to create a builder for other node
     * type.
     * 
     * @param node
     *            the node that the factory is able to handle and produce a
     *            builder for
     * @param builderProducer
     *            builder producer as a context to handle nodes that the factory
     *            is not able to handle
     * @return the template node builder for the {@code node}
     */
    TemplateNodeBuilder createBuilder(T node,
            Function<Node, Optional<TemplateNodeBuilder>> builderProducer);
}
