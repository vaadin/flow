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
package com.vaadin.client.flow.template;

import com.vaadin.client.Command;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.binding.BinderContext;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.reactive.Computation;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.TemplateMap;
import com.vaadin.flow.template.angular.ChildSlotNode;

import elemental.client.Browser;
import elemental.dom.Node;

/**
 * Child slot binding strategy. Handles "@child@" node.
 *
 * @author Vaadin Ltd
 *
 */
public class ChildSlotBindingStrategy extends AbstractTemplateStrategy<Node> {

    private static final class ChildSlotBinder implements Command {
        private final Node anchor;
        private final StateNode stateNode;
        private final BinderContext context;

        private StateNode childNode;

        public ChildSlotBinder(BinderContext context, Node anchor,
                StateNode stateNode) {
            this.anchor = anchor;
            this.stateNode = stateNode;
            this.context = context;
        }

        @Override
        public void execute() {
            final StateNode oldChildNode = childNode;
            final StateNode newChildNode = (StateNode) stateNode
                    .getMap(NodeFeatures.TEMPLATE)
                    .getProperty(TemplateMap.CHILD_SLOT_CONTENT).getValue();
            childNode = newChildNode;

            // Do the actual work separately so that this
            // computation doesn't depend on the values used for
            // binding
            Reactive.addFlushListener(
                    () -> updateChildSlot(oldChildNode, newChildNode));
        }

        private void updateChildSlot(StateNode oldChildNode,
                StateNode newChildNode) {
            Node parent = DomApi.wrap(anchor).getParentNode();

            if (oldChildNode != null) {
                Node oldChild = oldChildNode.getDomNode();
                assert DomApi.wrap(oldChild).getParentNode() == parent;

                DomApi.wrap(parent).removeChild(oldChild);
            }

            if (newChildNode != null) {
                Node newChild = context.createAndBind(childNode);

                DomApi.wrap(parent).insertBefore(newChild,
                        DomApi.wrap(anchor).getNextSibling());
            }
        }
    }

    @Override
    protected String getTemplateType() {
        return ChildSlotNode.TYPE;
    }

    @Override
    protected Node create(StateTree tree, int templateId) {
        return Browser.getDocument().createComment(" @child@ ");
    }

    @Override
    protected void bind(StateNode modelNode, Node anchor, int templateId,
            TemplateBinderContext context) {
        Computation computation = Reactive.runWhenDependenciesChange(
                new ChildSlotBinder(context, anchor, modelNode));
        modelNode.addUnregisterListener(e -> computation.stop());
    }

}
