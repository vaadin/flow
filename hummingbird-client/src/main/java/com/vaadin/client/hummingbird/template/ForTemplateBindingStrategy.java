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
package com.vaadin.client.hummingbird.template;

import com.vaadin.client.Command;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.binding.BinderContext;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.dom.DomApi;
import com.vaadin.client.hummingbird.nodefeature.MapProperty;
import com.vaadin.client.hummingbird.nodefeature.NodeMap;
import com.vaadin.client.hummingbird.reactive.Computation;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.Node;

/**
 * *ngFor template binding strategy.
 *
 * @author Vaadin Ltd
 *
 */
public class ForTemplateBindingStrategy extends AbstractTemplateStrategy<Node> {

    /**
     * The command uses {@code anchor} comment node as a placeholder (which has
     * been created by {@link ForTemplateBindingStrategy#create(StateNode)}
     * method to populate generated *ngFor elements after it.
     * <p>
     * Once something happens (which triggers the command : dependencies update)
     * the command removes everything after the {@code anchor} and before the
     * node that was immediate {@code anchor} sibling initially. Then it
     * populates the *ngFor elements using the model {@link StateNode} so that
     * it adds generated elements between the {@code anchor} and its initial
     * sibling {@code beforeNode}.
     *
     */
    private static final class ForTemplateNodeUpdate implements Command {

        private Node anchor;
        private Node beforeNode;
        private final StateNode stateNode;
        private final ForTemplateNode templateNode;
        private final BinderContext context;

        ForTemplateNodeUpdate(BinderContext context, Node anchor,
                StateNode stateNode, ForTemplateNode templateNode) {
            this.anchor = anchor;
            this.stateNode = stateNode;
            this.templateNode = templateNode;
            this.context = context;
        }

        @Override
        public void execute() {
            Element parent = anchor.getParentElement();
            if (beforeNode == null) {
                beforeNode = DomApi.wrap(anchor).getNextSibling();
            }

            JsArray<Double> children = templateNode.getChildrenIds();
            assert children.length() == 1;
            int childId = children.get(0).intValue();

            Node htmlNode = DomApi.wrap(anchor).getNextSibling();
            while (htmlNode != beforeNode) {
                DomApi.wrap(parent).removeChild(htmlNode);
                htmlNode = DomApi.wrap(anchor).getNextSibling();
            }

            NodeMap model = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
            MapProperty property = model
                    .getProperty(templateNode.getCollectionVariable());
            if (property.getValue() != null) {
                StateNode node = (StateNode) property.getValue();
                context.populateChildren(parent, node,
                        NodeFeatures.TEMPLATE_MODELLIST,
                        childNode -> AbstractTemplateStrategy
                                .createAndBind(childNode, childId, context),
                        beforeNode);
            }
        }

    }

    @Override
    protected String getTemplateType() {
        return com.vaadin.hummingbird.template.ForTemplateNode.TYPE;
    }

    @Override
    protected Node create(StateTree tree, int templateId) {
        return Browser.getDocument().createComment(" *ngFor ");
    }

    @Override
    protected void bind(StateNode stateNode, Node anchor, int templateId,
            BinderContext context) {
        ForTemplateNode templateNode = (ForTemplateNode) getTemplateNode(
                stateNode.getTree(), templateId);
        Computation computation = Reactive
                .runWhenDepedenciesChange(new ForTemplateNodeUpdate(context,
                        anchor, stateNode, templateNode));
        stateNode.addUnregisterListener(event -> computation.stop());
    }

}
