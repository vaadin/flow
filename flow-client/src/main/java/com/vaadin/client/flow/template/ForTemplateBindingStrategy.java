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
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.dom.DomElement;
import com.vaadin.client.flow.dom.DomNode;
import com.vaadin.client.flow.nodefeature.ListSpliceEvent;
import com.vaadin.client.flow.nodefeature.ListSpliceListener;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Computation;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

import elemental.client.Browser;
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
    private static final class ForTemplateNodeUpdate
            implements Command, ListSpliceListener {

        private final Node anchor;
        private final TemplateBinderContext context;

        private final int childId;
        private final MapProperty collectionProperty;

        // The number of children to remove if the binding is cleared
        private int childCount = 0;

        ForTemplateNodeUpdate(TemplateBinderContext context, Node anchor,
                StateNode stateNode, ForTemplateNode templateNode) {
            this.anchor = anchor;
            this.context = context;

            JsArray<Double> children = templateNode.getChildrenIds();
            assert children.length() == 1;
            childId = children.get(0).intValue();

            String collectionVariable = templateNode.getCollectionVariable();
            NodeMap model = stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP);
            collectionProperty = model.getProperty(collectionVariable);
        }

        @Override
        public void execute() {
            DomElement wrappedAnchor = DomApi.wrap(anchor);
            DomElement wrappedParent = DomApi
                    .wrap(wrappedAnchor.getParentNode());

            // This is run when the list property value changes, i.e. when we
            // should bind to a new list

            // Remove all children from the previous binding
            for (int i = 0; i < childCount; i++) {
                Node nextSibling = wrappedAnchor.getNextSibling();
                wrappedParent.removeChild(nextSibling);
            }
            childCount = 0;

            // Bind to a new list if there is one
            StateNode node = (StateNode) collectionProperty.getValue();
            if (node != null) {
                NodeList childList = node
                        .getList(NodeFeatures.TEMPLATE_MODELLIST);

                // Listener added inside a computation -> will be removed when
                // this computation is invalidated
                childList.addSpliceListener(e -> {
                    /*
                     * Handle lazily so we can create the children we need to
                     * insert. The change that gives a child node an element tag
                     * name might not yet have been applied at this point.
                     */
                    Reactive.addFlushListener(() -> onSplice(e));
                });

                // Init DOM contents based on current list values without making
                // the current computation depend on the current list contents
                Reactive.runWithComputation(null, () -> {
                    JsArray<Object> currentChildren = JsCollections.array();
                    for (int i = 0; i < childList.length(); i++) {
                        currentChildren.push(childList.get(i));
                    }

                    onSplice(new ListSpliceEvent(childList, 0,
                            JsCollections.array(), currentChildren, false));
                });
            }
        }

        @Override
        public void onSplice(ListSpliceEvent event) {
            DomNode wrappedParent = DomApi
                    .wrap(DomApi.wrap(anchor).getParentNode());

            // Find the DOM node before the splice target. Must find location
            // relative to the anchor since we don't know exactly what index the
            // anchor has.
            Node beforeNode = anchor;
            int startIndex = event.getIndex();
            for (int i = 0; i < startIndex; i++) {
                beforeNode = DomApi.wrap(beforeNode).getNextSibling();
            }

            // Remove a number of nodes following the splice target
            int removeCount = event.getRemove().length();
            for (int i = 0; i < removeCount; i++) {
                Node nextSibling = DomApi.wrap(beforeNode).getNextSibling();
                wrappedParent.removeChild(nextSibling);
            }

            // Create, bind and insert new nodes
            JsArray<?> add = event.getAdd();
            int addCount = add.length();
            for (int i = 0; i < addCount; i++) {
                StateNode childNode = (StateNode) add.get(i);

                Node childDomNode = AbstractTemplateStrategy
                        .createAndBind(childNode, childId, context);

                Node nextSibling = DomApi.wrap(beforeNode).getNextSibling();

                wrappedParent.insertBefore(childDomNode, nextSibling);
                beforeNode = childDomNode;
            }

            // Update child count so that the right number of nodes can be
            // removed when when the binding is reset
            childCount = childCount - removeCount + addCount;
        }
    }

    @Override
    protected String getTemplateType() {
        return com.vaadin.flow.template.angular.ForTemplateNode.TYPE;
    }

    @Override
    protected Node create(StateTree tree, int templateId) {
        return Browser.getDocument().createComment(" *ngFor ");
    }

    @Override
    protected void bind(StateNode modelNode, Node anchor, int templateId,
            TemplateBinderContext context) {
        ForTemplateNode templateNode = (ForTemplateNode) getTemplateNode(
                modelNode.getTree(), templateId);
        Computation computation = Reactive
                .runWhenDependenciesChange(new ForTemplateNodeUpdate(context,
                        anchor, modelNode, templateNode));
        modelNode.addUnregisterListener(event -> computation.stop());
    }

}
