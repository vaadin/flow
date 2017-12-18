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
package com.vaadin.flow.template.angular;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.TemplateMap;

import elemental.json.JsonObject;

/**
 * AngularTemplate node corresponding to a child slot in a template. This is
 * represented as <code>@child@</code> in the template file.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class ChildSlotNode extends AbstractControlTemplateNode {
    /**
     * Type value for child slot template nodes in JSON messages.
     */
    public static final String TYPE = "childSlot";

    /**
     * Creates a new child slot node.
     *
     * @param parent
     *            the parent of the new template node, not null
     */
    public ChildSlotNode(AbstractElementTemplateNode parent) {
        super(parent);

        assert parent != null : "Child slot can't be the root of a template";
    }

    @Override
    public int getGeneratedElementCount(StateNode templateStateNode) {
        assert templateStateNode != null;

        Optional<StateNode> childElementNode = templateStateNode
                .getFeature(TemplateMap.class).getChild();

        if (childElementNode.isPresent()) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public Element getElement(int index, StateNode templateStateNode) {
        assert templateStateNode != null;
        assert index == 0;

        assert getGeneratedElementCount(templateStateNode) == 1;

        StateNode childElementNode = templateStateNode
                .getFeature(TemplateMap.class).getChild().get();
        return Element.get(childElementNode);
    }

    @Override
    public int getChildCount() {
        // No child template nodes
        return 0;
    }

    @Override
    public TemplateNode getChild(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put(TemplateNode.KEY_TYPE, TYPE);
    }

    @Override
    public Element getParentElement(StateNode node) {
        TemplateNode parentTemplateNode = getParent().get();
        StateNode templateStateNode = node.getParent();

        return parentTemplateNode.getElement(0, templateStateNode);
    }

    /**
     * Find the first (and assumingly only) child slot node in a template node
     * tree.
     *
     * @param root
     *            the root of the template tree to traverse, not
     *            <code>null</code>
     *
     * @return an optional child slot node, or an empty optional if the template
     *         tree contains no child slot node
     */
    public static Optional<ChildSlotNode> find(TemplateNode root) {
        assert root != null;

        Deque<TemplateNode> stack = new ArrayDeque<>();
        stack.add(root);

        while (!stack.isEmpty()) {
            TemplateNode node = stack.removeLast();
            if (node instanceof ChildSlotNode) {
                return Optional.of((ChildSlotNode) node);
            }

            for (int i = 0; i < node.getChildCount(); i++) {
                stack.addLast(node.getChild(i));
            }
        }

        return Optional.empty();
    }

}
