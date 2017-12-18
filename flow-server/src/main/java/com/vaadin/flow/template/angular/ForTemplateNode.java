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

import java.util.List;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ModelList;
import com.vaadin.flow.internal.nodefeature.ModelMap;

import elemental.json.JsonObject;

/**
 * A template AST node representing the "*ngFor" looping part of an element.
 * <p>
 * This node always has one element child node, which represents the element
 * containing the "*ngFor" attribute. E.g.
 * <code>&lt;li class="item" *ngFor="let item of list"&gt;</code>
 *
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class ForTemplateNode extends AbstractControlTemplateNode {

    /**
     * Type value for ngFor template nodes in JSON messages.
     */
    public static final String TYPE = "ngFor";

    public static final String LOOP_VARIABLE = "loopVariable";

    public static final String COLLECTION_VARIABLE = "collectionVariable";

    private final AbstractElementTemplateNode childNode;
    private final String loopVariable;
    private final String collectionVariable;

    /**
     * Creates a new for template node.
     *
     * @param parent
     *            the parent node of this node, can not be <code>null</code>
     * @param collectionVariable
     *            the name of the variable to loop through
     * @param loopVariable
     *            the name of the variable used inside the loop
     * @param childBuilder
     *            the template builder for the child node
     */
    public ForTemplateNode(AbstractElementTemplateNode parent,
            String collectionVariable, String loopVariable,
            ElementTemplateBuilder childBuilder) {
        super(parent);
        this.collectionVariable = collectionVariable;
        this.loopVariable = loopVariable;
        List<TemplateNode> nodes = childBuilder.build(this);
        assert nodes.size() == 1;
        TemplateNode node = nodes.get(0);
        assert node instanceof AbstractElementTemplateNode;
        childNode = (AbstractElementTemplateNode) node;
    }

    @Override
    public int getChildCount() {
        return 1; // The element containing *ngFor
    }

    @Override
    public TemplateNode getChild(int index) {
        if (index != 0) {
            throw new IllegalArgumentException(
                    getClass().getName() + " only has 1 child");
        }

        return childNode;
    }

    private ModelList getModelList(StateNode modelNode) {
        StateNode stateNodeWithList = (StateNode) modelNode
                .getFeature(ModelMap.class).getValue(collectionVariable);
        if (stateNodeWithList == null) {
            throw new IllegalArgumentException("No model defined for the key '"
                    + collectionVariable + "'");
        }
        return stateNodeWithList.getFeature(ModelList.class);
    }

    @Override
    public int getGeneratedElementCount(StateNode templateStateNode) {
        return getModelList(templateStateNode).size();
    }

    @Override
    public Element getElement(int index, StateNode templateStateNode) {
        return childNode.getElement(0,
                getModelList(templateStateNode).get(index));
    }

    @Override
    public Element getParentElement(StateNode node) {
        TemplateNode parentTemplateNode = getParent().get();
        StateNode nodeWithList = node.getParent();
        StateNode nodeWithModel = nodeWithList.getParent();

        return parentTemplateNode.getElement(0, nodeWithModel);
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put(TemplateNode.KEY_TYPE, TYPE);
        json.put(LOOP_VARIABLE, getLoopVariable());
        json.put(COLLECTION_VARIABLE, getCollectionVariable());
    }

    /**
     * Gets the loop variable.
     * <p>
     * Only for testing purposes.
     *
     * @return the loop variable
     */
    protected String getLoopVariable() {
        return loopVariable;
    }

    /**
     * Gets the collection variable.
     * <p>
     * Only for testing purposes.
     *
     * @return the collection variable
     */
    protected String getCollectionVariable() {
        return collectionVariable;
    }
}
