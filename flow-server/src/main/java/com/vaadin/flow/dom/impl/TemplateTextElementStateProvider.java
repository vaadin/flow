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
package com.vaadin.flow.dom.impl;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.dom.NodeVisitor;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ModelMap;
import com.vaadin.flow.template.angular.TextTemplateNode;

/**
 * Handles storing and retrieval of the state information for a text node
 * defined in a template.
 *
 * @author Vaadin Ltd
 */
public class TemplateTextElementStateProvider
        extends AbstractTextElementStateProvider {

    private final TextTemplateNode templateNode;

    /**
     * Creates a new text element state provider for the given template AST
     * node.
     *
     * @param templateNode
     *            the template AST node
     */
    public TemplateTextElementStateProvider(TextTemplateNode templateNode) {
        assert templateNode != null;
        this.templateNode = templateNode;
    }

    @Override
    public boolean supports(StateNode node) {
        /*
         * ModelMap is the only one that is strictly needed. Other features such
         * as TemplateMap might be present in most cases, but those are not
         * needed e.g. for a text node inside a *ngFor since it's bound to a sub
         * model node.
         */
        return node.hasFeature(ModelMap.class);
    }

    @Override
    public String getTextContent(StateNode node) {
        return templateNode.getTextBinding().getValue(node, "");
    }

    @Override
    public void setTextContent(StateNode node, String textContent) {
        throw new UnsupportedOperationException(
                "Cannot modify text node defined in a template");
    }

    @Override
    public Node getParent(StateNode node) {
        return TemplateElementStateProvider.getParent(node, templateNode);
    }

    @Override
    public void setComponent(StateNode node, Component component) {
        throw new UnsupportedOperationException(
                "Cannot modify text node defined in a template");
    }

    @Override
    public Optional<Component> getComponent(StateNode node) {
        return Optional.empty();
    }

    @Override
    public void visit(StateNode node, NodeVisitor visitor,
            boolean visitDescendants) {
        throw new UnsupportedOperationException();
    }
}
